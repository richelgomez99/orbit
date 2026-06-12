package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.orbit.app.graph.GraphEntityType
import com.orbit.app.graph.GraphStatus

@Entity(
    tableName = "graph_entity",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["type"]),
        Index(value = ["normalizedName"]),
        Index(value = ["status"]),
        Index(value = ["invalidatedAt"])
    ]
)
data class GraphEntityEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: GraphEntityType,
    val canonicalName: String,
    val normalizedName: String,
    val description: String?,
    val status: GraphStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val invalidatedAt: Long?,
    val invalidatedReason: String?
)
