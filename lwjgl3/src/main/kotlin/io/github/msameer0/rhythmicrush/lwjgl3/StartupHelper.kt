package io.github.msameer0.rhythmicrush.lwjgl3

import com.badlogic.gdx.Version
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3NativesLoader
import org.lwjgl.system.JNI
import org.lwjgl.system.linux.UNISTD
import org.lwjgl.system.macosx.LibC
import org.lwjgl.system.macosx.ObjCRuntime
import java.io.File
import java.lang.management.ManagementFactory

class StartupHelper {
    companion object {
        private const val JVM_RESTARTED_ARG = "jvmIsRestarted"
        private const val MAC_JRE_ERR_MSG = "A Java installation could not be found. If you are distributing this app with a bundled JRE, be sure to set the '-XstartOnFirstThread' argument manually!"
        private const val LINUX_JRE_ERR_MSG = "A Java installation could not be found. If you are distributing this app with a bundled JRE, be sure to set the environment variable '__GL_THREADED_OPTIMIZATIONS' to '0'!"
        private const val CHILD_LOOP_ERR_MSG = "The current JVM process is a spawned child JVM process, but StartupHelper has attempted to spawn another child JVM process! This is a broken state, and should not normally happen! Your game may crash or not function properly!"

        fun isLinuxNvidia() : Boolean {
            val drivers = File("/proc/driver").list {
                    _, path -> path.uppercase().contains("NVIDIA")
            }

            if (drivers == null) {
                return false
            }

            return drivers.size > 0
        }

        fun startNewJvmIfRequired() : Boolean {
            return startNewJvmIfRequired(true)
        }

        fun startNewJvmIfRequired(inheritIO : Boolean) : Boolean {
            val osName = System.getProperty("os.name").lowercase()

            if (osName.contains("mac")) return StartupHelper.startNewJvm0( /*isMac =*/true,
                inheritIO
            )

            if (osName.contains("windows")) {
                // Here, we are trying to work around an issue with how LWJGL3 loads its extracted .dll files.
                // By default, LWJGL3 extracts to the directory specified by "java.io.tmpdir": usually, the user's home.
                // If the user's name has non-ASCII (or some non-alphanumeric) characters in it, that would fail.
                // By extracting to the relevant "ProgramData" folder, which is usually "C:\ProgramData", we avoid this.
                // We also temporarily change the "user.name" property to one without any chars that would be invalid.
                // We revert our changes immediately after loading LWJGL3 natives.
                var programData = System.getenv("ProgramData")
                if (programData == null) programData =
                    "C:\\Temp" // if ProgramData isn't set, try some fallback.

                val prevTmpDir = System.getProperty("java.io.tmpdir", programData)
                val prevUser = System.getProperty("user.name", "libGDX_User")
                System.setProperty("java.io.tmpdir", "$programData\\libGDX-temp")
                System.setProperty(
                    "user.name",
                    ("User_" + prevUser.hashCode() + "_GDX" + Version.VERSION).replace('.', '_')
                )
                Lwjgl3NativesLoader.load()
                System.setProperty("java.io.tmpdir", prevTmpDir)
                System.setProperty("user.name", prevUser)
                return false
            }
            return startNewJvm0( /*isMac =*/false, inheritIO)
        }

        fun startNewJvm0(isMac: Boolean, inheritIO: Boolean): Boolean {
            val processID = getProcessID(isMac)

            if (!isMac) {
                // No need to restart non-NVIDIA Linux
                if (!isLinuxNvidia()) return false
                // check whether __GL_THREADED_OPTIMIZATIONS is already disabled
                if ("0" == System.getenv("__GL_THREADED_OPTIMIZATIONS")) return false
            } else {
                // There is no need for -XstartOnFirstThread on Graal native image
                if (!System.getProperty("org.graalvm.nativeimage.imagecode", "")
                        .isEmpty()
                ) return false

                // Checks if we are already on the main thread, such as from running via Construo.
                val objcMsgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend")
                val nsThread = ObjCRuntime.objc_getClass("NSThread")
                val currentThread =
                    JNI.invokePPP(nsThread, ObjCRuntime.sel_getUid("currentThread"), objcMsgSend)
                val isMainThread = JNI.invokePPZ(
                    currentThread,
                    ObjCRuntime.sel_getUid("isMainThread"),
                    objcMsgSend
                )
                if (isMainThread) return false

                if ("1" == System.getenv("JAVA_STARTED_ON_FIRST_THREAD_$processID")) return false
            }


            // Check whether this JVM process is a child JVM process already.
            // This state shouldn't usually be reachable, but this stops us from endlessly spawning new child JVM processes.
            if ("true" == System.getProperty(JVM_RESTARTED_ARG)) {
                System.err.println(CHILD_LOOP_ERR_MSG)
                return false
            }


            // Spawn the child JVM process with updated environment variables or JVM args
            val jvmArgs: MutableList<String?> = ArrayList<String?>()

            // The following line is used assuming you target Java 8, the minimum for LWJGL3.
            val javaExecPath = System.getProperty("java.home") + "/bin/java"


            // If targeting Java 9 or higher, you could use the following instead of the above line:
            //String javaExecPath = ProcessHandle.current().info().command().orElseThrow()
            if (!(File(javaExecPath).exists())) {
                System.err.println(getJreErrMsg(isMac))
                return false
            }

            jvmArgs.add(javaExecPath)
            if (isMac) jvmArgs.add("-XstartOnFirstThread")
            jvmArgs.add("-D$JVM_RESTARTED_ARG=true")
            jvmArgs.addAll(ManagementFactory.getRuntimeMXBean().inputArguments)
            jvmArgs.add("-cp")
            jvmArgs.add(System.getProperty("java.class.path"))
            var mainClass = System.getenv("JAVA_MAIN_CLASS_$processID")
            if (mainClass == null) {
                val trace = Thread.currentThread().stackTrace
                if (trace.size > 0) mainClass = trace[trace.size - 1].className
                else {
                    System.err.println("The main class could not be determined.")
                    return false
                }
            }
            jvmArgs.add(mainClass)

            try {
                val processBuilder = ProcessBuilder(jvmArgs)
                if (!isMac) processBuilder.environment()["__GL_THREADED_OPTIMIZATIONS"] = "0"

                if (!inheritIO) processBuilder.start()
                else processBuilder.inheritIO().start().waitFor()
            } catch (e: Exception) {
                System.err.println("There was a problem restarting the JVM.")
                // noinspection CallToPrintStackTrace
                e.printStackTrace()
            }

            return true
        }

        private fun getJreErrMsg(isMac: Boolean): String {
            return if (isMac) MAC_JRE_ERR_MSG
            else LINUX_JRE_ERR_MSG
        }

        private fun getProcessID(isMac: Boolean): Long {
            return if (isMac) LibC.getpid()
            else UNISTD.getpid().toLong()
        }
    }
}
