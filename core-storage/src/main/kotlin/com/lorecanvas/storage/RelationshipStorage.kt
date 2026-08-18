package com.lorecanvas.storage

import com.lorecanvas.common.LcResult
import com.lorecanvas.domain.Relationship
import java.io.File

interface RelationshipStorage {
    fun createRelationship(projectDirectory: File, relationship: Relationship): LcResult<Unit, StorageError>
    fun saveRelationship(projectDirectory: File, relationship: Relationship): LcResult<Unit, StorageError>
    fun loadRelationship(projectDirectory: File, relationshipId: String): LcResult<Relationship, StorageError>
    fun deleteRelationship(projectDirectory: File, relationshipId: String): LcResult<Unit, StorageError>
    fun relationshipExists(projectDirectory: File, relationshipId: String): Boolean
    fun listRelationships(projectDirectory: File): List<Relationship>
}
