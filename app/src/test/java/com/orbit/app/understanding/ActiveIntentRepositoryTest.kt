package com.orbit.app.understanding

import com.orbit.app.data.dao.ActiveIntentDao
import com.orbit.app.data.entity.ActiveIntentEntity
import com.orbit.app.understanding.domain.ActiveIntentResolutionReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveIntentRepositoryTest {
    @Test
    fun transitions_hideResolvedButKeepByCaptureSearchable() = runTest {
        val dao = FakeActiveIntentDao()
        val repository = RoomActiveIntentRepository(dao)
        repository.upsert(entity(status = "ACTIVE"))

        assertEquals(1, repository.observeActiveGroups().first().single().items.size)

        repository.resolve("intent-1", ActiveIntentResolutionReason.REPLIED_OR_DONE, atMillis = 200L)

        assertEquals(emptyList<ActiveIntentGroup>(), repository.observeActiveGroups().first())
        val searchable = repository.getByCapture("capture-1").single()
        assertEquals("RESOLVED", searchable.status)
    }

    @Test
    fun invalidateCapture_marksActiveRowsInvalidated() = runTest {
        val dao = FakeActiveIntentDao()
        val repository = RoomActiveIntentRepository(dao)
        repository.upsert(entity(status = "ACTIVE"))

        repository.invalidateCapture("capture-1", atMillis = 300L)

        val row = repository.getByCapture("capture-1").single()
        assertEquals("INVALIDATED", row.status)
    }

    @Test
    fun archiveAndExpire_removeRowsFromActiveList() = runTest {
        val dao = FakeActiveIntentDao()
        val repository = RoomActiveIntentRepository(dao)
        repository.upsert(entity(status = "ACTIVE"))

        repository.archive("intent-1", atMillis = 400L)

        assertEquals(emptyList<ActiveIntentGroup>(), repository.observeActiveGroups().first())
        assertEquals("ARCHIVED", repository.getByCapture("capture-1").single().status)

        repository.upsert(entity(status = "ACTIVE").copy(intentId = "intent-2"))
        repository.expire("intent-2", atMillis = 500L)

        val expired = repository.getByCapture("capture-1").first { it.intentId == "intent-2" }
        assertEquals("EXPIRED", expired.status)
    }

    private fun entity(status: String): ActiveIntentEntity = ActiveIntentEntity(
        intentId = "intent-1",
        captureId = "capture-1",
        intentType = "CHAT_ACTION",
        status = status,
        primaryEvidenceJson = "{\"completionKey\":{\"status\":\"FOUND\"}}",
        primaryAction = "Reply or act",
        dueAt = null,
        expiresAt = null,
        resolutionReason = null,
        resolvedAt = null,
        userConfirmed = false,
        createdAt = 100L,
        updatedAt = 100L
    )
}

private class FakeActiveIntentDao : ActiveIntentDao {
    private val rows = linkedMapOf<String, ActiveIntentEntity>()
    private val activeRows = MutableStateFlow<List<ActiveIntentEntity>>(emptyList())

    override suspend fun upsert(entity: ActiveIntentEntity) {
        rows[entity.intentId] = entity
        publish()
    }

    override fun observeActive(): Flow<List<ActiveIntentEntity>> = activeRows

    override suspend fun getByCapture(captureId: String): List<ActiveIntentEntity> =
        rows.values.filter { it.captureId == captureId }.sortedByDescending { it.updatedAt }

    override suspend fun getById(intentId: String): ActiveIntentEntity? = rows[intentId]

    override suspend fun transition(
        intentId: String,
        status: String,
        resolutionReason: String?,
        resolvedAt: Long?,
        userConfirmed: Boolean,
        updatedAt: Long
    ) {
        rows[intentId]?.let { row ->
            rows[intentId] = row.copy(
                status = status,
                resolutionReason = resolutionReason,
                resolvedAt = resolvedAt,
                userConfirmed = userConfirmed,
                updatedAt = updatedAt
            )
        }
        publish()
    }

    override suspend fun invalidateByCapture(captureId: String, reason: String, invalidatedAt: Long) {
        rows.replaceAll { _, row ->
            if (row.captureId == captureId && row.status != "INVALIDATED") {
                row.copy(
                    status = "INVALIDATED",
                    resolutionReason = reason,
                    resolvedAt = invalidatedAt,
                    userConfirmed = false,
                    updatedAt = invalidatedAt
                )
            } else {
                row
            }
        }
        publish()
    }

    override suspend fun deleteByCapture(captureId: String) {
        rows.entries.removeIf { it.value.captureId == captureId }
        publish()
    }

    private fun publish() {
        activeRows.value = rows.values.filter { it.status == "ACTIVE" }.sortedByDescending { it.updatedAt }
    }
}
