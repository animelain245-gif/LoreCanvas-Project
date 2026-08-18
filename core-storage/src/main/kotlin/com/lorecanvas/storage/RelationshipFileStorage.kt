package com.lorecanvas.storage

import com.lorecanvas.common.LcResult
import com.lorecanvas.common.Logger
import com.lorecanvas.common.createLogger
import com.lorecanvas.domain.Relationship
import com.lorecanvas.storage.serialization.RelationshipSerializer
import java.io.File

class RelationshipFileStorage(
    private val fileManager: FileManager = RealFileManager(),
    private val logger: Logger = createLogger("RelationshipFileStorage")
) : RelationshipStorage {

    companion object {
        const val RELATIONSHIPS_SUBDIRECTORY = "relationships"
    }

    private fun dir(projectDirectory: File) = File(projectDirectory, RELATIONSHIPS_SUBDIRECTORY)
    private fun file(projectDirectory: File, id: String) = File(dir(projectDirectory), "$id.json")

    override fun createRelationship(projectDirectory: File, relationship: Relationship): LcResult<Unit, StorageError> {
        if (relationshipExists(projectDirectory, relationship.id)) {
            return LcResult.fail(StorageError(StorageErrorType.ALREADY_EXISTS, "A relationship with id ${relationship.id} already exists."))
        }
        return write(projectDirectory, relationship)
    }

    override fun saveRelationship(projectDirectory: File, relationship: Relationship): LcResult<Unit, StorageError> {
        if (!relationshipExists(projectDirectory, relationship.id)) {
            return LcResult.fail(StorageError(StorageErrorType.NOT_FOUND, "No relationship with id ${relationship.id}."))
        }
        return write(projectDirectory, relationship)
    }

    private fun write(projectDirectory: File, relationship: Relationship): LcResult<Unit, StorageError> = try {
        fileManager.createDirectories(dir(projectDirectory))
        fileManager.writeTextAtomic(file(projectDirectory, relationship.id), RelationshipSerializer.toJson(relationship))
        logger.info("Relationship written", relationship.id)
        LcResult.ok(Unit)
    } catch (e: Exception) {
        LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to write relationship."))
    }

    override fun loadRelationship(projectDirectory: File, relationshipId: String): LcResult<Relationship, StorageError> {
        val f = file(projectDirectory, relationshipId)
        if (!fileManager.exists(f)) {
            return LcResult.fail(StorageError(StorageErrorType.NOT_FOUND, "No relationship with id $relationshipId."))
        }
        val raw = try {
            fileManager.readText(f)
        } catch (e: Exception) {
            return LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to read relationship."))
        }
        return when (val result = RelationshipSerializer.fromJson(raw)) {
            is RelationshipSerializer.DeserializeResult.Success -> LcResult.ok(result.relationship)
            is RelationshipSerializer.DeserializeResult.Malformed ->
                LcResult.fail(StorageError(StorageErrorType.CORRUPT_DATA, result.reason))
        }
    }

    override fun deleteRelationship(projectDirectory: File, relationshipId: String): LcResult<Unit, StorageError> {
        val f = file(projectDirectory, relationshipId)
        if (!fileManager.exists(f)) {
            return LcResult.fail(StorageError(StorageErrorType.NOT_FOUND, "No relationship with id $relationshipId."))
        }
        return try {
            fileManager.deleteRecursively(f)
            LcResult.ok(Unit)
        } catch (e: Exception) {
            LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to delete relationship."))
        }
    }

    override fun relationshipExists(projectDirectory: File, relationshipId: String): Boolean =
        fileManager.exists(file(projectDirectory, relationshipId))

    override fun listRelationships(projectDirectory: File): List<Relationship> {
        val directory = dir(projectDirectory)
        if (!fileManager.exists(directory)) return emptyList()
        return fileManager.listFiles(directory, ".json").mapNotNull { f ->
            val raw = try {
                fileManager.readText(f)
            } catch (e: Exception) {
                logger.warn("Skipping unreadable relationship file", f.name)
                return@mapNotNull null
            }
            when (val result = RelationshipSerializer.fromJson(raw)) {
                is RelationshipSerializer.DeserializeResult.Success -> result.relationship
                is RelationshipSerializer.DeserializeResult.Malformed -> {
                    logger.warn("Skipping corrupt relationship file", "${f.name}: ${result.reason}")
                    null
                }
            }
        }
    }
}
