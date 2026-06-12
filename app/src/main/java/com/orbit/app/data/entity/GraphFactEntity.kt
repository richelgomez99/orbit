package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.orbit.app.graph.GraphStatus

@Entity(
    tableName = "graph_fact",
    foreignKeys = [
        ForeignKey(
            entity = GraphEntityEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectEntityId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GraphEntityEntity::class,
            parentColumns = ["id"],
            childColumns = ["objectEntityId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["status"]),
        Index(value = ["subjectEntityId"]),
        Index(value = ["objectEntityId"]),
        Index(value = ["predicate"]),
        Index(value = ["invalidatedAt"])
    ]
)
data class GraphFactEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val subjectEntityId: String,
    val predicate: String,
    val objectText: String?,
    val objectEntityId: String?,
    val confidence: Float,
    val status: GraphStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val invalidatedAt: Long?,
    val invalidatedReason: String?
)
