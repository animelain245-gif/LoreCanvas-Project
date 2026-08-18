package com.lorecanvas.storage

import com.lorecanvas.common.LcResult
import com.lorecanvas.domain.Node
import java.io.File

/**
 * NodeStorage (LCD-007): persists Nodes into a Project's `nodes/`
 * subdirectory, one JSON file per Node (`nodes/<uuid>.json`) — following
 * the same directory-based, human-inspectable format as
 * [ProjectStorage], just one level down.
 */
interface NodeStorage {
    fun createNode(projectDirectory: File, node: Node): LcResult<Unit, StorageError>
    fun saveNode(projectDirectory: File, node: Node): LcResult<Unit, StorageError>
    fun loadNode(projectDirectory: File, nodeId: String): LcResult<Node, StorageError>
    fun deleteNode(projectDirectory: File, nodeId: String): LcResult<Unit, StorageError>
    fun nodeExists(projectDirectory: File, nodeId: String): Boolean

    /**
     * Loads every Node in the project. Simple and sufficient at Version 1
     * scale (LCD-007, Chapter 17 flags performance as something to revisit
     * for large projects, not something Phase 3 needs to solve up front).
     */
    fun listNodes(projectDirectory: File): List<Node>
}
