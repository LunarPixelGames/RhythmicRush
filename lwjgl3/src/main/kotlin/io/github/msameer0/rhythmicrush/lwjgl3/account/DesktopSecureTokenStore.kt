package io.github.msameer0.rhythmicrush.lwjgl3.account

import com.sun.jna.platform.win32.Crypt32Util
import io.github.msameer0.rhythmicrush.account.InMemorySecureTokenStore
import io.github.msameer0.rhythmicrush.account.SecureTokenStore
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

class DesktopSecureTokenStore private constructor(
    private val delegate: SecureTokenStore,
    val persistent: Boolean
) : SecureTokenStore by delegate {
    companion object {
        fun create(): DesktopSecureTokenStore {
            val os = System.getProperty("os.name", "").lowercase()
            val store = runCatching {
                when {
                    os.contains("win") -> WindowsDpapiTokenStore()
                    os.contains("mac") -> MacKeychainTokenStore()
                    os.contains("linux") -> LinuxSecretServiceTokenStore()
                    else -> null
                }
            }.getOrNull()

            return if (store != null && store.available()) {
                DesktopSecureTokenStore(store, true)
            } else {
                DesktopSecureTokenStore(InMemorySecureTokenStore(), false)
            }
        }
    }
}

private interface PlatformTokenStore : SecureTokenStore {
    fun available(): Boolean
}

private class WindowsDpapiTokenStore : PlatformTokenStore {
    private val directory = File(
        System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"),
        "RhythmicRush/auth"
    )

    override fun available(): Boolean = runCatching {
        directory.mkdirs()
        directory.isDirectory
    }.getOrDefault(false)

    override fun load(key: String): String? = runCatching {
        val file = tokenFile(key)
        if (!file.isFile) return null
        val encrypted = Base64.getDecoder().decode(file.readText(StandardCharsets.US_ASCII))
        val decrypted = Crypt32Util.cryptUnprotectData(encrypted)
        String(decrypted, StandardCharsets.UTF_8)
    }.getOrNull()

    override fun save(key: String, value: String): Boolean = runCatching {
        directory.mkdirs()
        val encrypted = Crypt32Util.cryptProtectData(value.toByteArray(StandardCharsets.UTF_8))
        val temporary = File(directory, "${tokenFile(key).name}.tmp")
        temporary.writeText(Base64.getEncoder().encodeToString(encrypted), StandardCharsets.US_ASCII)
        val destination = tokenFile(key)
        if (destination.exists() && !destination.delete()) return false
        temporary.renameTo(destination)
    }.getOrDefault(false)

    override fun delete(key: String) {
        runCatching { tokenFile(key).delete() }
    }

    override fun clear() {
        runCatching { directory.listFiles()?.forEach { it.delete() } }
    }

    private fun tokenFile(key: String) = File(directory, "${safeKey(key)}.token")
}

private class MacKeychainTokenStore : PlatformTokenStore {
    override fun available(): Boolean = runCommand("/usr/bin/security", "-h").started

    override fun load(key: String): String? {
        val result = runCommand(
            "/usr/bin/security",
            "find-generic-password",
            "-s", SERVICE,
            "-a", key,
            "-w"
        )
        return if (result.exitCode == 0) result.output.trim().takeIf { it.isNotEmpty() } else null
    }

    override fun save(key: String, value: String): Boolean =
        runCommand(
            "/usr/bin/security",
            "add-generic-password",
            "-U",
            "-s", SERVICE,
            "-a", key,
            "-w", value
        ).exitCode == 0

    override fun delete(key: String) {
        runCommand("/usr/bin/security", "delete-generic-password", "-s", SERVICE, "-a", key)
    }

    override fun clear() {}

    companion object {
        private const val SERVICE = "RhythmicRush"
    }
}

private class LinuxSecretServiceTokenStore : PlatformTokenStore {
    override fun available(): Boolean = runCommand("secret-tool", "--version").started

    override fun load(key: String): String? {
        val result = runCommand("secret-tool", "lookup", "service", SERVICE, "account", key)
        return if (result.exitCode == 0) result.output.trim().takeIf { it.isNotEmpty() } else null
    }

    override fun save(key: String, value: String): Boolean =
        runCommand(
            "secret-tool", "store",
            "--label=Rhythmic Rush",
            "service", SERVICE,
            "account", key,
            stdin = value
        ).exitCode == 0

    override fun delete(key: String) {
        runCommand("secret-tool", "clear", "service", SERVICE, "account", key)
    }

    override fun clear() {}

    companion object {
        private const val SERVICE = "rhythmic-rush"
    }
}

private data class CommandResult(
    val started: Boolean,
    val exitCode: Int,
    val output: String
)

private fun runCommand(vararg command: String, stdin: String? = null): CommandResult {
    return try {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()
        if (stdin != null) {
            process.outputStream.bufferedWriter(StandardCharsets.UTF_8).use { it.write(stdin) }
        } else {
            process.outputStream.close()
        }
        val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        CommandResult(true, process.waitFor(), output)
    } catch (_: Exception) {
        CommandResult(false, -1, "")
    }
}

private fun safeKey(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}
