package com.orbit.app.library

import com.orbit.app.memory.MemorySearchResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySemanticResultsTest {
    @Test
    fun localBackedResults_filtersDeadRemoteEnvelopesAndPreservesCloudOrder() = runTest {
        val lookup = FakeLocalLookup(
            existingIds = setOf("qr", "dentist"),
            fallback = emptyList()
        )
        val results = LibrarySemanticResults.localBackedResults(
            remoteResults = listOf(
                result("dead", rank = 1, title = "Cloud-only deleted capture"),
                result("qr", rank = 2, title = "QR code for customers"),
                result("dentist", rank = 3, title = "Dentist rescheduling")
            ),
            localEnvelopeLookup = lookup,
            query = "qr code",
            limit = 10
        )

        assertEquals(listOf("qr", "dentist"), results.map { it.envelopeId })
        assertEquals(listOf("dead", "qr", "dentist"), lookup.existsCalls)
    }

    @Test
    fun localBackedResults_appendsLocalFallbackAfterCloudAndDedupesEnvelopeIds() = runTest {
        val lookup = FakeLocalLookup(
            existingIds = setOf("dentist"),
            fallback = listOf(
                result("dentist", rank = 1, title = "Dentist reschedule local duplicate"),
                result("reschedule-note", rank = 2, title = "Call to reschedule haircut")
            )
        )
        val results = LibrarySemanticResults.localBackedResults(
            remoteResults = listOf(result("dentist", rank = 1, title = "Dentist appointment moved")),
            localEnvelopeLookup = lookup,
            query = "reschedule",
            limit = 10
        )

        assertEquals(listOf("dentist", "reschedule-note"), results.map { it.envelopeId })
    }

    @Test
    fun localBackedResults_dedupesSameDisplayRowsAcrossDifferentEnvelopeIds() = runTest {
        val lookup = FakeLocalLookup(
            existingIds = setOf("a", "b"),
            fallback = emptyList()
        )
        val results = LibrarySemanticResults.localBackedResults(
            remoteResults = listOf(
                result("a", rank = 1, title = "Dentist appointment", summary = "Rescheduled for Monday"),
                result("b", rank = 2, title = "Dentist appointment", summary = "Rescheduled for Monday")
            ),
            localEnvelopeLookup = lookup,
            query = "reschedule",
            limit = 10
        )

        assertEquals(listOf("a"), results.map { it.envelopeId })
    }

    @Test
    fun localBackedResults_appliesLimitAfterFallbackAndDedupe() = runTest {
        val lookup = FakeLocalLookup(
            existingIds = setOf("a", "b", "c"),
            fallback = listOf(result("fallback", rank = 1, title = "Fallback"))
        )
        val results = LibrarySemanticResults.localBackedResults(
            remoteResults = listOf(
                result("a", rank = 1, title = "A"),
                result("b", rank = 2, title = "B"),
                result("c", rank = 3, title = "C")
            ),
            localEnvelopeLookup = lookup,
            query = "anything",
            limit = 2
        )

        assertEquals(listOf("a", "b"), results.map { it.envelopeId })
    }

    private class FakeLocalLookup(
        private val existingIds: Set<String>,
        private val fallback: List<MemorySearchResult>
    ) : LocalEnvelopeLookup {
        val existsCalls = mutableListOf<String>()

        override suspend fun exists(envelopeId: String): Boolean {
            existsCalls += envelopeId
            return envelopeId in existingIds
        }

        override suspend fun search(query: String, limit: Int): List<MemorySearchResult> =
            fallback.take(limit)
    }

    private fun result(
        envelopeId: String,
        rank: Int,
        title: String,
        summary: String? = "Summary",
    ): MemorySearchResult = MemorySearchResult(
        envelopeId = envelopeId,
        rank = rank,
        score = 1f / rank,
        title = title,
        summary = summary,
        dayLocal = "2026-06-03",
        createdAtMillis = 0L,
        intent = "REFERENCE"
    )
}
