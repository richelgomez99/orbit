package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.orbit.app.graph.GraphStatus

@Entity(
    tableName = "graph_relationship",
    foreignKeys = [
        ForeignKey(
            entity = GraphEntityEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromEntityId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GraphEntityEntity::class,
            parentColumns = ["id"],
            childColumns = ["toEntityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["status"]),
        Index(value = ["fromEntityId"]),
        Index(value = ["toEntityId"]),
        Index(value = ["relationshipType"]),
        Index(value = ["invalidatedAt"])
    ]
)
data class GraphRelationshipEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val fromEntityId: String,
    val toEntityId: String,
    val relationshipType: String,
    val confidence: Float,
    val status: GraphStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val invalidatedAt: Long?,
    val invalidatedReason: String?
)
