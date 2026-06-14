package com.orbit.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orbit.app.data.entity.ResolutionReceiptEntity
import com.orbit.app.resolution.ResolutionKind
import com.orbit.app.resolution.ResolutionTargetType

@Dao
interface ResolutionReceiptDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ResolutionReceiptEntity): Long

    @Query(
        """
        SELECT * FROM resolution_receipt
        WHERE targetType = :targetType AND targetId = :targetId
        ORDER BY occurredAtMillis ASC, id ASC
        """
    )
    suspend fun getForTarget(
        targetType: ResolutionTargetType,
        targetId: String,
    ): List<ResolutionReceiptEntity>

    @Query(
        """
        SELECT * FROM resolution_receipt
        WHERE envelopeId = :envelopeId
        ORDER BY occurredAtMillis DESC, id DESC
        LIMIT :limit
        """
    )
    suspend fun getForEnvelope(
        envelopeId: String,
        limit: Int,
    ): List<ResolutionReceiptEntity>

    @Query(
        """
        SELECT * FROM resolution_receipt
        WHERE kind = :kind
        ORDER BY occurredAtMillis DESC, id DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentByKind(
        kind: ResolutionKind,
        limit: Int,
    ): List<ResolutionReceiptEntity>
}
