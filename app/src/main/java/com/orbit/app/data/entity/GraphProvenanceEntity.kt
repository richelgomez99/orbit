package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.orbit.app.graph.GraphSourceType
import com.orbit.app.graph.GraphSupportKind
import com.orbit.app.graph.GraphTargetType

@Entity(
    tableName = "graph_provenance",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["targetType", "targetId"]),
        Index(value = ["sourceType", "sourceId"]),
        Index(value = ["supportKind"]),
        Index(value = ["invalidatedAt"])
    ]
)
data class GraphProvenanceEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val targetType: GraphTargetType,
    val targetId: String,
    val sourceType: GraphSourceType,
    val sourceId: String,
    val supportKind: GraphSupportKind,
    val createdAt: Long,
    val invalidatedAt: Long?,
    val invalidatedReason: String?
)
