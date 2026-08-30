package com.lorecanvas.repository

import com.lorecanvas.storage.serialization.NodeSerializer
import com.lorecanvas.domain.NodeStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NodeSerializerTest {

    @Test
    fun testMigration10To11() {
        val json10 = """
            {
              "id": "node-1",
              "name": "Old Node",
              "type": "Character",
              "summary": "Summary",
              "status": "ACTIVE",
              "tags": [],
              "createdAt": "2024-01-01T00:00:00Z",
              "modifiedAt": "2024-01-01T00:00:00Z"
            }
        """.trimIndent()
        
        val result = NodeSerializer.fromJson(json10)
        assertTrue(result is NodeSerializer.DeserializeResult.Success)
        val node = (result as NodeSerializer.DeserializeResult.Success).node
        
        assertEquals("node-1", node.id)
        assertNull(node.parentNodeId)
        assertEquals(0, node.displayOrder)
    }

    private fun assertTrue(condition: Boolean) {
        if (!condition) throw AssertionError("Expected true")
    }
}
