package com.lorecanvas.storage

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * File Manager (LCD-007, Chapter 7): "The File Manager handles all
 * interaction with the operating system... No other package should access
 * project files directly."
 *
 * [ProjectFileStorage] is the only class that talks to [FileManager] — the
 * Repository layer never sees a raw [File].
 */
interface FileManager {
    fun createDirectories(path: File)
    fun exists(path: File): Boolean
    fun readText(path: File): String
    /** Atomic per LCD-007, Chapter 9: readers never observe a partial write. */
    fun writeTextAtomic(path: File, content: String)
    fun deleteRecursively(path: File): Boolean
    fun listDirectories(path: File): List<File>
    fun listFiles(path: File, extension: String): List<File>
    fun copyDirectory(source: File, destination: File)
}

class RealFileManager : FileManager {

    override fun createDirectories(path: File) {
        if (!path.exists() && !path.mkdirs()) {
            throw java.io.IOException("Failed to create directory: ${path.absolutePath}")
        }
    }

    override fun exists(path: File): Boolean = path.exists()

    override fun readText(path: File): String = path.readText(StandardCharsets.UTF_8)

    override fun writeTextAtomic(path: File, content: String) {
        // LCD-007, Chapter 8 (Save Strategy): serialize -> write temp file ->
        // verify -> replace existing file -> confirm. Writing to a sibling
        // temp file and atomically moving it over the destination means a
        // crash mid-write leaves either the old file or the new file
        // intact — never a half-written one.
        path.parentFile?.let { createDirectories(it) }
        val tempFile = File(path.parentFile, "${path.name}.tmp")
        tempFile.writeText(content, StandardCharsets.UTF_8)

        // Verify: re-read what was written before it's allowed to replace
        // the previous good copy (LCD-007, Chapter 15 — basic corruption
        // check at the point of writing, not just at load time).
        val verify = tempFile.readText(StandardCharsets.UTF_8)
        if (verify != content) {
            tempFile.delete()
            throw java.io.IOException("Write verification failed for ${path.absolutePath}")
        }

        try {
            Files.move(
                tempFile.toPath(),
                path.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: java.nio.file.AtomicMoveNotSupportedException) {
            // Some filesystems (notably certain FAT-formatted removable
            // storage) don't support atomic moves. Falling back to a plain
            // replace is strictly weaker, but still only ever reachable
            // after the temp file was fully written and verified above.
            Files.move(tempFile.toPath(), path.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    override fun deleteRecursively(path: File): Boolean = path.deleteRecursively()

    override fun listDirectories(path: File): List<File> =
        path.listFiles { file -> file.isDirectory }?.toList() ?: emptyList()

    override fun listFiles(path: File, extension: String): List<File> =
        path.listFiles { file -> file.isFile && file.name.endsWith(extension) }?.toList() ?: emptyList()

    /** Recursive copy, used to snapshot a project into backups/ before an import applies (LCD-009 Ch.15). */
    override fun copyDirectory(source: File, destination: File) {
        if (!source.exists()) return
        createDirectories(destination)
        source.listFiles()?.forEach { child ->
            val target = File(destination, child.name)
            if (child.isDirectory) {
                copyDirectory(child, target)
            } else {
                child.copyTo(target, overwrite = true)
            }
        }
    }
}
