package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.orbit.app.graph.GraphSourceType

@Entity(
    tableName = "graph_mention",
    foreignKeys = [
        ForeignKey(
            entity = GraphEntityEntity::class,
            parentColumns = ["id"],
            childColumns = ["entityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["entityId"]),
        Index(value = ["sourceType", "sourceId"])
    ]
)
data class GraphMentionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val entityId: String,
    val sourceType: GraphSourceType,
    val sourceId: String,
    val label: String?,
    val excerptDigest: String?,
    val createdAt: Long
)
