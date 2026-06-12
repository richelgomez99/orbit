package com.orbit.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.orbit.app.graph.GraphFeedbackType
import com.orbit.app.graph.GraphSourceType
import com.orbit.app.graph.GraphTargetType

@Entity(
    tableName = "graph_feedback",
    foreignKeys = [
        ForeignKey(
            entity = GraphEntityEntity::class,
            parentColumns = ["id"],
            childColumns = ["replacementEntityId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["targetType", "targetId"]),
        Index(value = ["feedbackType"]),
        Index(value = ["replacementEntityId"]),
        Index(value = ["sourceType", "sourceId"])
    ]
)
data class GraphFeedbackEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val targetType: GraphTargetType,
    val targetId: String,
    val feedbackType: GraphFeedbackType,
    val replacementText: String?,
    val replacementEntityId: String?,
    val sourceType: GraphSourceType,
    val sourceId: String,
    val createdAt: Long
)
