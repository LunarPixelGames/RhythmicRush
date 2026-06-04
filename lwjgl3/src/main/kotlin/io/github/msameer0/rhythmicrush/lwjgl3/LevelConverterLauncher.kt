package io.github.msameer0.rhythmicrush.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3FileHandle
import io.github.msameer0.rhythmicrush.game.level.LevelSerializer
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.io.File

/**
 * Standalone utility to batch convert JSON levels to the optimized UBJSON binary format.
 */
object LevelConverterLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        println("--- Rhythmic Rush Level Converter ---")

        val folder: String? = TinyFileDialogs.tinyfd_selectFolderDialog("Select Levels Directory", "assets/levels")
        if (folder == null) {
            println("No folder selected. Exiting.")
            return
        }

        val dir = File(folder)
        if (!dir.exists() || !dir.isDirectory) {
            println("Invalid directory.")
            return
        }

        val backupDir = File(dir, "backups_json")
        if (!backupDir.exists()) backupDir.mkdirs()

        val files = dir.listFiles { f -> f.name.endsWith(".json") }
        if (files == null || files.isEmpty()) {
            println("No .json levels found in $folder")
            return
        }

        println("Found ${files.size} levels to convert.")

        var successCount = 0
        var failCount = 0

        for (file in files) {
            try {
                val handle = Lwjgl3FileHandle(file, com.badlogic.gdx.Files.FileType.Absolute)
                val level = LevelSerializer.load(handle)

                if (level != null) {
                    val binaryName = file.nameWithoutExtension + ".ubj"
                    val binaryFile = File(dir, binaryName)
                    val binaryHandle = Lwjgl3FileHandle(binaryFile, com.badlogic.gdx.Files.FileType.Absolute)

                    // Save as binary
                    LevelSerializer.saveBinary(level, binaryHandle)

                    // Backup original json (Copy then delete to be safer across file systems)
                    val backupFile = File(backupDir, file.name)
                    file.copyTo(backupFile, overwrite = true)
                    file.delete()

                    println("Converted: ${file.name} -> $binaryName (Original moved to backups_json)")
                    successCount++
                } else {
                    println("Failed to load: ${file.name}")
                    failCount++
                }
            } catch (e: Exception) {
                println("Error converting ${file.name}: ${e.message}")
                failCount++
            }
        }

        println("\n--- Done! ---")
        println("Successfully converted: $successCount")
        println("Failed: $failCount")
        if (successCount > 0) {
            println("Original JSON files are in: ${backupDir.absolutePath}")
        }
    }
}
