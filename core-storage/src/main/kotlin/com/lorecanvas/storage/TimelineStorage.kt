package com.lorecanvas.storage

import com.lorecanvas.common.LcResult
import com.lorecanvas.domain.Timeline
import java.io.File

interface TimelineStorage {
    fun createTimeline(projectDirectory: File, timeline: Timeline): LcResult<Unit, StorageError>
    fun saveTimeline(projectDirectory: File, timeline: Timeline): LcResult<Unit, StorageError>
    fun loadTimeline(projectDirectory: File, timelineId: String): LcResult<Timeline, StorageError>
    fun deleteTimeline(projectDirectory: File, timelineId: String): LcResult<Unit, StorageError>
    fun timelineExists(projectDirectory: File, timelineId: String): Boolean
    fun listTimelines(projectDirectory: File): List<Timeline>
}
