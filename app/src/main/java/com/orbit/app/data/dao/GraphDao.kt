package com.orbit.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orbit.app.data.entity.GraphEntityEntity
import com.orbit.app.data.entity.GraphFactEntity
import com.orbit.app.data.entity.GraphFeedbackEntity
import com.orbit.app.data.entity.GraphMentionEntity
import com.orbit.app.data.entity.GraphProvenanceEntity
import com.orbit.app.data.entity.GraphRelationshipEntity
import com.orbit.app.graph.GraphSourceType
import com.orbit.app.graph.GraphTargetType

@Dao
interface GraphDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntity(entity: GraphEntityEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMention(mention: GraphMentionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFact(fact: GraphFactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: GraphRelationshipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvenance(provenance: GraphProvenanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: GraphFeedbackEntity): Long

    @Query("SELECT * FROM graph_entity WHERE id = :id AND userId = :userId")
    suspend fun getEntity(userId: String, id: String): GraphEntityEntity?

    @Query("SELECT * FROM graph_fact WHERE id = :id AND userId = :userId")
    suspend fun getFact(userId: String, id: String): GraphFactEntity?

    @Query("SELECT * FROM graph_fact WHERE id = :id")
    suspend fun getFactById(id: String): GraphFactEntity?

    @Query("SELECT * FROM graph_relationship WHERE id = :id AND userId = :userId")
    suspend fun getRelationship(userId: String, id: String): GraphRelationshipEntity?

    @Query("SELECT * FROM graph_relationship WHERE id = :id")
    suspend fun getRelationshipById(id: String): GraphRelationshipEntity?

    @Query(
        """
        SELECT * FROM graph_provenance
        WHERE userId = :userId
            AND targetType = :targetType
            AND targetId = :targetId
            AND invalidatedAt IS NULL
        ORDER BY createdAt ASC
        """
    )
    suspend fun activeProvenanceForTarget(
        userId: String,
        targetType: GraphTargetType,
        targetId: String
    ): List<GraphProvenanceEntity>

    @Query(
        """
        SELECT COUNT(*) FROM graph_provenance
        WHERE userId = :userId
            AND targetType = :targetType
            AND targetId = :targetId
            AND invalidatedAt IS NULL
        """
    )
    suspend fun activeProvenanceCount(
        userId: String,
        targetType: GraphTargetType,
        targetId: String
    ): Int

    @Query(
        """
        UPDATE graph_provenance
        SET invalidatedAt = :at,
            invalidatedReason = :reason
        WHERE sourceType = :sourceType
            AND sourceId = :sourceId
            AND invalidatedAt IS NULL
        """
    )
    suspend fun invalidateProvenanceBySource(
        sourceType: GraphSourceType,
        sourceId: String,
        reason: String,
        at: Long
    ): Int

    @Query(
        """
        SELECT DISTINCT targetId FROM graph_provenance
        WHERE targetType = :targetType
            AND sourceType = :sourceType
            AND sourceId = :sourceId
        """
    )
    suspend fun targetIdsForSource(
        targetType: GraphTargetType,
        sourceType: GraphSourceType,
        sourceId: String
    ): List<String>

    @Query(
        """
        UPDATE graph_fact
        SET status = 'INVALIDATED',
            updatedAt = :at,
            invalidatedAt = :at,
            invalidatedReason = :reason
        WHERE userId = :userId
            AND id = :id
            AND status = 'ACTIVE'
        """
    )
    suspend fun invalidateFact(userId: String, id: String, reason: String, at: Long): Int

    @Query(
        """
        UPDATE graph_relationship
        SET status = 'INVALIDATED',
            updatedAt = :at,
            invalidatedAt = :at,
            invalidatedReason = :reason
        WHERE userId = :userId
            AND id = :id
            AND status = 'ACTIVE'
        """
    )
    suspend fun invalidateRelationship(userId: String, id: String, reason: String, at: Long): Int

    @Query("SELECT * FROM graph_entity WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun exportEntities(userId: String): List<GraphEntityEntity>

    @Query("SELECT * FROM graph_fact WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun exportFacts(userId: String): List<GraphFactEntity>

    @Query("SELECT * FROM graph_relationship WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun exportRelationships(userId: String): List<GraphRelationshipEntity>

    @Query("SELECT * FROM graph_provenance WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun exportProvenance(userId: String): List<GraphProvenanceEntity>

    @Query("SELECT * FROM graph_feedback WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun exportFeedback(userId: String): List<GraphFeedbackEntity>
}
