package com.orbit.app.data

import android.content.Context
import android.os.Debug
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orbit.app.data.security.KeystoreKeyProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Gap-4 regression tripwire — see
 * `docs/ml-process-memory-baseline-2026-07-06.md`.
 *
 * Seeds a 1,000-envelope corpus through the real encrypted (SQLCipher)
 * Room stack and exercises the hot `:ml` read paths (day page, tokenized
 * evidence search, cluster candidates). Asserts the Java + native heap
 * delta stays under a deliberately generous ceiling: the point is to
 * catch order-of-magnitude regressions (a cache pinning the corpus in
 * memory, a leak in a query path) before they meet a ~1 GB model in the
 * same process — not to police kilobytes.
 *
 * The 128 MB ceiling is ~10x the measured cost of this workload at the
 * time of writing; tighten it only with fresh measurements in hand.
 */
@RunWith(AndroidJUnit4::class)
class MlMemoryFootprintTest {

    private lateinit var db: OrbitDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        System.loadLibrary("sqlcipher")
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
        val passphrase = KeystoreKeyProvider.getOrCreatePassphrase(context)
        db = Room.databaseBuilder(context, OrbitDatabase::class.java, DB_NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun thousandEnvelopeCorpus_staysUnderHeapCeiling() = runBlocking {
        val before = usedHeapBytes()

        // Seed 1k envelopes + a hydration result each via raw SQL (fast,
        // and identical to what the DAOs read back).
        val sql = db.openHelper.writableDatabase
        val now = 1_700_000_000_000L
        sql.beginTransaction()
        try {
            repeat(CORPUS_SIZE) { i ->
                sql.execSQL(
                    """
                    INSERT INTO intent_envelope(
                        id, contentType, textContent, imageUri, textContentSha256,
                        intent, intentConfidence, intentSource, intentHistoryJson,
                        createdAt, day_local, isArchived, isDeleted, deletedAt,
                        sharedContinuationResultId, appCategory, activityState,
                        tzId, hourLocal, dayOfWeekLocal,
                        kind, derivedFromEnvelopeIdsJson, todoMetaJson
                    ) VALUES('mem-env-$i', 'TEXT',
                        'memory fixture $i dentist reschedule flight recipe article https://example.com/$i',
                        NULL, NULL, 'REFERENCE', NULL, 'USER_CHIP', '[]',
                        ${now + i}, '2026-07-0${(i % 6) + 1}', 0, 0, NULL, NULL,
                        'OTHER', 'UNKNOWN', 'UTC', 12, 5,
                        'REGULAR', NULL, NULL)
                    """.trimIndent()
                )
                sql.execSQL(
                    """
                    INSERT INTO continuation(
                        id, envelopeId, type, status, inputUrl,
                        scheduledAt, startedAt, completedAt, attemptCount, failureReason
                    ) VALUES('mem-cont-$i', 'mem-env-$i', 'URL_HYDRATE', 'SUCCEEDED',
                        'https://example.com/$i', ${now + i}, ${now + i}, ${now + i}, 1, NULL)
                    """.trimIndent()
                )
                sql.execSQL(
                    """
                    INSERT INTO continuation_result(
                        id, continuationId, envelopeId, producedAt, title, domain,
                        canonicalUrl, canonicalUrlHash, excerpt, summary, summaryModel
                    ) VALUES('mem-res-$i', 'mem-cont-$i', 'mem-env-$i', ${now + i},
                        'Fixture $i', 'example.com', 'https://example.com/$i',
                        'hash-$i', 'excerpt $i',
                        'summary body for fixture $i with enough words to be realistic about typical hydrated page summaries stored per capture',
                        'test')
                    """.trimIndent()
                )
            }
            sql.setTransactionSuccessful()
        } finally {
            sql.endTransaction()
        }

        // Exercise the hot :ml read paths several times over.
        repeat(5) { pass ->
            db.intentEnvelopeDao().searchActive("dentist", 50)
            db.intentEnvelopeDao().searchActive("recipe", 50)
            db.intentEnvelopeDao().searchActive("flight", 50)
            db.clusterDao().findClusterCandidates(now - 1L, 10)
            db.intentEnvelopeDao()
                .observeDayWithResults("2026-07-0${(pass % 6) + 1}")
                .first()
        }

        // Force a GC pass so transient allocation churn doesn't count
        // against the ceiling — we care about retained growth.
        Runtime.getRuntime().gc()
        Thread.sleep(200)
        val after = usedHeapBytes()
        val deltaMb = (after - before).coerceAtLeast(0) / (1024.0 * 1024.0)

        android.util.Log.i(
            "MlMemoryFootprint",
            "corpus=$CORPUS_SIZE retainedHeapDeltaMb=%.1f".format(deltaMb)
        )
        assertTrue(
            "Retained Java+native heap grew %.1f MB after a $CORPUS_SIZE-envelope workload — ceiling is $CEILING_MB MB. Investigate caches/leaks before any model shares this process.".format(deltaMb),
            deltaMb < CEILING_MB
        )
    }

    private fun usedHeapBytes(): Long {
        val rt = Runtime.getRuntime()
        return (rt.totalMemory() - rt.freeMemory()) + Debug.getNativeHeapAllocatedSize()
    }

    private companion object {
        const val DB_NAME = "orbit-ml-memory-footprint-test.db"
        const val CORPUS_SIZE = 1_000
        const val CEILING_MB = 128
    }
}
