package com.lorecanvas.storage

import com.lorecanvas.common.LcResult
import com.lorecanvas.common.Logger
import com.lorecanvas.common.createLogger
import com.lorecanvas.domain.Timeline
import com.lorecanvas.storage.serialization.TimelineSerializer
import java.io.File

class TimelineFileStorage(
    private val fileManager: FileManager = RealFileManager(),
    private val logger: Logger = createLogger("TimelineFileStorage")
) : TimelineStorage {

    companion object {
        const val TIMELINES_SUBDIRECTORY = "timelines"
    }

    private fun dir(projectDirectory: File) = File(projectDirectory, TIMELINES_SUBDIRECTORY)
    private fun file(projectDirectory: File, id: String) = File(dir(projectDirectory), "$id.json")

    override fun createTimeline(projectDirectory: File, timeline: Timeline): LcResult<Unit, StorageError> {
        if (timelineExists(projectDirectory, timeline.id)) {
            return LcResult.fail(StorageError(StorageErrorType.ALREADY_EXISTS, "A timeline with id ${timeline.id} already exists."))
        }
        return write(projectDirectory, timeline)
    }

    override fun saveTimeline(projectDirectory: File, timeline: Timeline): LcResult<Unit, StorageError> {
        if (!timelineExists(projectDirectory, timeline.id)) {
            return LcResult.fail(StorageError(StorageErrorType.NOT_FOUND, "No timeline with id ${timeline.id}."))
        }
        return write(projectDirectory, timeline)
    }

    private fun write(projectDirectory: File, timeline: Timeline): LcResult<Unit, StorageError> = try {
        fileManager.createDirectories(dir(projectDirectory))
        fileManager.writeTextAtomic(file(projectDirectory, timeline.id), TimelineSerializer.toJson(timeline))
        logger.info("Timeline written", timeline.id)
        LcResult.ok(Unit)
    } catch (e: Exception) {
        LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to write timeline."))
    }

    override fun loadTimeline(projectDirectory: File, timelineId: String): LcResult<Timeline, StorageError> {
        val f = file(projectDirectory, timelineId)
        if (!fileManager.exists(f)) {
            return LcResult.fail(StorageError(StorageErrorType.NOT_FOUND, "No timeline with id $timelineId."))
        }
        val raw = try {
            fileManager.readText(f)
        } catch (e: Exception) {
            return LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to read timeline."))
        }
        return when (val result = TimelineSerializer.fromJson(raw)) {
            is TimelineSerializer.DeserializeResult.Success -> LcResult.ok(result.timeline)
            is TimelineSerializer.DeserializeResult.Malformed ->
                LcResult.fail(StorageError(StorageErrorType.CORRUPT_DATA, result.reason))
        }
    }

    override fun deleteTimeline(projectDirectory: File, timelineId: String): LcResult<Unit, StorageError> {
        val f = file(projectDirectory, timelineId)
        if (!fileManager.exists(f)) {
            return LcResult.fail(StorageError(StorageErrorType.NOT_FOUND, "No timeline with id $timelineId."))
        }
        return try {
            fileManager.deleteRecursively(f)
            LcResult.ok(Unit)
        } catch (e: Exception) {
            LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to delete timeline."))
        }
    }

    override fun timelineExists(projectDirectory: File, timelineId: String): Boolean =
        fileManager.exists(file(projectDirectory, timelineId))

    override fun listTimelines(projectDirectory: File): List<Timeline> {
        val directory = dir(projectDirectory)
        if (!fileManager.exists(directory)) return emptyList()
        return fileManager.listFiles(directory, ".json").mapNotNull { f ->
            val raw = try {
                fileManager.readText(f)
            } catch (e: Exception) {
                logger.warn("Skipping unreadable timeline file", f.name)
                return@mapNotNull null
            }
            when (val result = TimelineSerializer.fromJson(raw)) {
                is TimelineSerializer.DeserializeResult.Success -> result.timeline
                is TimelineSerializer.DeserializeResult.Malformed -> {
                    logger.warn("Skipping corrupt timeline file", "${f.name}: ${result.reason}")
                    null
                }
            }
        }
    }
}
