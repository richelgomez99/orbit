package com.orbit.app.debug

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.orbit.app.data.ipc.EnvelopeRepositoryService
import com.orbit.app.data.ipc.IEnvelopeRepository
import com.orbit.app.data.ipc.IntentEnvelopeDraftParcel
import com.orbit.app.data.ipc.StateSnapshotParcel
import com.orbit.app.settings.PrivacyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.LocalDateTime
import java.time.ZoneId

class DebugDemoSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SEED_DEMO) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                PrivacyPreferences(context.applicationContext).memoryIndexingEnabled = true
                val seeded = withRepository(context.applicationContext) { repo ->
                    DEMO_ITEMS.map { item ->
                        repo.sealWithResult(item.toDraft(), item.toState()).envelopeId
                    }
                }
                val actionSeed = withRepository(context.applicationContext) { repo ->
                    repo.debugSeedDemoActionProposals()
                }
                val memorySeed = withRepository(context.applicationContext) { repo ->
                    repo.debugSeedDemoMemoryCandidates()
                }
                Log.i(TAG, "seeded demo envelopes count=${seeded.distinct().size}")
                Log.i(TAG, "seeded demo action proposals result=$actionSeed")
                Log.i(TAG, "seeded demo memory candidates result=$memorySeed")
            } catch (t: Throwable) {
                Log.w(TAG, "demo seed failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun <T> withRepository(context: Context, block: suspend (IEnvelopeRepository) -> T): T {
        val bound = bindRepository(context)
        return try {
            block(bound.repository)
        } finally {
            withContext(Dispatchers.Main) {
                runCatching { context.unbindService(bound.connection) }
            }
        }
    }

    private suspend fun bindRepository(context: Context): BoundRepository = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<IEnvelopeRepository>()
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                deferred.complete(IEnvelopeRepository.Stub.asInterface(service))
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                if (!deferred.isCompleted) deferred.completeExceptionally(IllegalStateException("repository disconnected"))
            }
        }
        val bound = context.bindService(
            Intent(context, EnvelopeRepositoryService::class.java),
            conn,
            Context.BIND_AUTO_CREATE,
        )
        if (!bound) error("bindService(EnvelopeRepositoryService) failed")
        BoundRepository(repository = withTimeout(BIND_TIMEOUT_MS) { deferred.await() }, connection = conn)
    }

    private data class BoundRepository(
        val repository: IEnvelopeRepository,
        val connection: ServiceConnection,
    )

    private data class DemoItem(
        val text: String,
        val intent: String,
        val appCategory: String,
        val sourceAppLabel: String,
    ) {
        fun toDraft() = IntentEnvelopeDraftParcel(
            contentType = "TEXT",
            textContent = text,
            imageUri = null,
            intent = intent,
            intentConfidence = 0.96f,
            intentSource = "AUTO_AMBIGUOUS",
        )

        fun toState(): StateSnapshotParcel {
            val now = LocalDateTime.now(ZoneId.systemDefault())
            return StateSnapshotParcel(
                appCategory = appCategory,
                activityState = "STILL",
                tzId = ZoneId.systemDefault().id,
                hourLocal = now.hour,
                dayOfWeekLocal = now.dayOfWeek.value,
                sourceAppLabel = sourceAppLabel,
            )
        }
    }

    private companion object {
        const val ACTION_SEED_DEMO = "com.orbit.app.debug.SEED_DEMO_MEMORY"
        const val TAG = "DebugDemoSeedReceiver"
        const val BIND_TIMEOUT_MS = 5_000L

        val DEMO_ITEMS = listOf(
            DemoItem(
                "Startup event ticket for Monday founder office hours. Venue doors open at 6pm with a panel about MVP scope and traction.",
                "REFERENCE",
                "WORK_EMAIL",
                "Gmail",
            ),
            DemoItem(
                "Flight receipt: NYC to San Francisco, confirmation ORB123, departs Monday morning. Total paid 248 dollars.",
                "REFERENCE",
                "WORK_EMAIL",
                "Gmail",
            ),
            DemoItem(
                "Recipe to try: miso salmon with ginger rice. Marinade uses miso, mirin, soy sauce, and honey.",
                "REFERENCE",
                "BROWSER",
                "Chrome",
            ),
            DemoItem("Order confirmation for hand espresso grinder. Estimated delivery Tuesday.", "REFERENCE", "WORK_EMAIL", "Gmail"),
            DemoItem("Saved hotel address near Moscone Center for the startup event trip. Check-in starts at 3pm.", "REFERENCE", "BROWSER", "Maps"),
            DemoItem("Raizy asked if Monday afternoon works after the startup event. Need to reply with availability.", "FOR_SOMEONE", "MESSAGING", "Messages"),
            DemoItem("Article saved for later: mobile attention memory systems and quiet daybook design patterns.", "READ_LATER", "BROWSER", "Chrome"),
            DemoItem("Dentist appointment reminder conflicts with Monday travel. Need to reschedule before flight.", "FOR_SOMEONE", "OTHER", "Calendar"),
            DemoItem("Receipt for product strategy books, including a founder manual and user research guide.", "REFERENCE", "WORK_EMAIL", "Gmail"),
            DemoItem("Monday MVP checklist: Diary works, Library search opens captures, cloud index stays compact.", "REFERENCE", "OTHER", "Keep"),
            DemoItem("Concert ticket saved for next Friday. Doors at 8pm. Add to shared calendar.", "REFERENCE", "WORK_EMAIL", "Gmail"),
            DemoItem("Recipe clip: lemon pasta with parmesan, black pepper, and a little pasta water.", "REFERENCE", "SOCIAL", "Instagram"),
            DemoItem("Return label for headphones expires Monday. Print label or drop off before 5pm.", "FOR_SOMEONE", "WORK_EMAIL", "Gmail"),
            DemoItem("Ramen restaurant near the hotel, open late after startup event. Try spicy miso bowl.", "REFERENCE", "BROWSER", "Maps"),
            DemoItem("Watch later: on-device AI routing, local model manager, and cloud fallback design.", "READ_LATER", "VIDEO", "YouTube"),
            DemoItem("Developer tools invoice for May. Includes hosting, API credits, and database usage.", "REFERENCE", "WORK_EMAIL", "Gmail"),
            DemoItem("Mom asked for flight arrival time and hotel address for San Francisco travel.", "FOR_SOMEONE", "MESSAGING", "WhatsApp"),
            DemoItem("Knowledge graph design note: ground agent actions in captured evidence and cite source envelopes.", "READ_LATER", "BROWSER", "Chrome"),
            DemoItem("Whiteboard capture summary: MVP means Library search with cited local capture links before Monday.", "REFERENCE", "OTHER", "Photos"),
            DemoItem("Shopping list for recipe night: salmon, miso, ginger, rice, lemons, parmesan, pasta.", "FOR_SOMEONE", "OTHER", "Notes"),
        )
    }
}
