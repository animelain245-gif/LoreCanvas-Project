package com.lorecanvas.storage

import com.lorecanvas.common.LcResult
import com.lorecanvas.common.Logger
import com.lorecanvas.common.createLogger
import com.lorecanvas.domain.Node
import com.lorecanvas.storage.serialization.NodeSerializer
import java.io.File

/**
 * NodeFileStorage — the real implementation of [NodeStorage].
 *
 * Layout, inside a Project directory already created by
 * [ProjectFileStorage] (which pre-creates an empty `nodes/` directory):
 *
 * ```
 * <ProjectDirectory>/nodes/<node-uuid>.json
 * ```
 */
class NodeFileStorage(
    private val fileManager: FileManager = RealFileManager(),
    private val logger: Logger = createLogger("NodeFileStorage")
) : NodeStorage {

    companion object {
        const val NODES_SUBDIRECTORY = "nodes"
    }

    private fun nodesDirectory(projectDirectory: File): File = File(projectDirectory, NODES_SUBDIRECTORY)
    private fun nodeFile(projectDirectory: File, nodeId: String): File =
        File(nodesDirectory(projectDirectory), "$nodeId.json")

    override fun createNode(projectDirectory: File, node: Node): LcResult<Unit, StorageError> {
        if (nodeExists(projectDirectory, node.id)) {
            return LcResult.fail(
                StorageError(StorageErrorType.ALREADY_EXISTS, "A node with id ${node.id} already exists.")
            )
        }
        return writeNode(projectDirectory, node)
    }

    override fun saveNode(projectDirectory: File, node: Node): LcResult<Unit, StorageError> {
        if (!nodeExists(projectDirectory, node.id)) {
            return LcResult.fail(
                StorageError(StorageErrorType.NOT_FOUND, "No node with id ${node.id} exists in ${projectDirectory.name}.")
            )
        }
        return writeNode(projectDirectory, node)
    }

    private fun writeNode(projectDirectory: File, node: Node): LcResult<Unit, StorageError> {
        return try {
            fileManager.createDirectories(nodesDirectory(projectDirectory))
            fileManager.writeTextAtomic(nodeFile(projectDirectory, node.id), NodeSerializer.toJson(node))
            logger.info("Node written", node.id)
            LcResult.ok(Unit)
        } catch (e: Exception) {
            logger.error("Failed to write node", e.message)
            LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to write node."))
        }
    }

    override fun loadNode(projectDirectory: File, nodeId: String): LcResult<Node, StorageError> {
        val file = nodeFile(projectDirectory, nodeId)
        if (!fileManager.exists(file)) {
            return LcResult.fail(StorageError(StorageErrorType.NOT_FOUND, "No node with id $nodeId."))
        }

        val raw = try {
            fileManager.readText(file)
        } catch (e: Exception) {
            return LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to read node $nodeId"))
        }

        return when (val result = NodeSerializer.fromJson(raw)) {
            is NodeSerializer.DeserializeResult.Success -> LcResult.ok(result.node)
            is NodeSerializer.DeserializeResult.Malformed -> {
                logger.error("Corrupt node file", "$nodeId: ${result.reason}")
                LcResult.fail(StorageError(StorageErrorType.CORRUPT_DATA, result.reason))
            }
        }
    }

    override fun deleteNode(projectDirectory: File, nodeId: String): LcResult<Unit, StorageError> {
        val file = nodeFile(projectDirectory, nodeId)
        if (!fileManager.exists(file)) {
            return LcResult.fail(StorageError(StorageErrorType.NOT_FOUND, "No node with id $nodeId."))
        }
        return try {
            fileManager.deleteRecursively(file)
            logger.info("Node deleted", nodeId)
            LcResult.ok(Unit)
        } catch (e: Exception) {
            logger.error("Failed to delete node", e.message)
            LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to delete node."))
        }
    }

    override fun nodeExists(projectDirectory: File, nodeId: String): Boolean =
        fileManager.exists(nodeFile(projectDirectory, nodeId))

    override fun listNodes(projectDirectory: File): List<Node> {
        val dir = nodesDirectory(projectDirectory)
        if (!fileManager.exists(dir)) return emptyList()

        return fileManager.listFiles(dir, ".json")
            .mapNotNull { file ->
                val raw = try {
                    fileManager.readText(file)
                } catch (e: Exception) {
                    logger.warn("Skipping unreadable node file", file.name)
                    return@mapNotNull null
                }
                when (val result = NodeSerializer.fromJson(raw)) {
                    is NodeSerializer.DeserializeResult.Success -> result.node
                    is NodeSerializer.DeserializeResult.Malformed -> {
                        logger.warn("Skipping corrupt node file during listing", "${file.name}: ${result.reason}")
                        null
                    }
                }
            }
            .sortedBy { it.name.lowercase() }
    }
}
