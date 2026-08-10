package com.capsule.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.capsule.app.understanding.UnderstandingSourceGlyphKind

@Entity(
    tableName = "source_identity",
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
        Index(value = ["captureId", "version"], unique = true),
        Index(value = ["captureId", "invalidatedAt", "supersededAt"]),
        Index("providerKey")
    ]
)
data class SourceIdentityEntity(
    @PrimaryKey val id: String,
    val captureId: String,
    val version: Int,
    val providerKey: String?,
    val providerLabel: String?,
    val originAppLabel: String?,
    val genericCategory: String?,
    val displayLabel: String,
    val secondaryLabel: String?,
    val glyphKind: UnderstandingSourceGlyphKind,
    val confidence: Float?,
    val evidenceIdsJson: String,
    val limitationsJson: String,
    val resolverVersion: Int,
    val createdAt: Long,
    val supersededAt: Long? = null,
    val invalidatedAt: Long? = null
)