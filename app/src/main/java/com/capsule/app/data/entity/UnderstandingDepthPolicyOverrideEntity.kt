package com.capsule.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.capsule.app.understanding.UnderstandingDepth
import com.capsule.app.understanding.UnderstandingOverrideKind
import com.capsule.app.understanding.UnderstandingPolicyScope

@Entity(
    tableName = "understanding_depth_policy_override",
    foreignKeys = [
        ForeignKey(
            entity = IntentEnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("captureId"),
        Index("domainSuppressionKey"),
        Index(value = ["scope", "domainSuppressionKey", "updatedAt"]),
        Index(value = ["captureId", "updatedAt"])
    ]
)
data class UnderstandingDepthPolicyOverrideEntity(
    @PrimaryKey val id: String,
    val scope: UnderstandingPolicyScope,
    val captureId: String?,
    val depth: UnderstandingDepth,
    val overrideKind: UnderstandingOverrideKind?,
    val domainSuppressionKey: String?,
    val cloudAllowed: Boolean,
    val reason: String?,
    val createdAt: Long,
    val updatedAt: Long
)