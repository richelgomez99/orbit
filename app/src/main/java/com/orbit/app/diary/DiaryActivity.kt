package com.orbit.app.diary

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.app.ui.primitives.MonoLabel
import com.orbit.app.ui.tokens.OrbitType
import androidx.lifecycle.lifecycleScope
import com.orbit.app.ai.LlmProviderRouter
import com.orbit.app.audit.DebugCounters
import com.orbit.app.diary.ui.OrbitCleanupScreen
import com.orbit.app.diary.ui.DiaryScreen
import com.orbit.app.diary.ui.ManualComposeDialog
import com.orbit.app.library.BinderLibraryRepository
import com.orbit.app.library.LibraryViewModel
import com.orbit.app.library.ui.LibraryScreen
import com.orbit.app.onboarding.OnboardingActivity
import com.orbit.app.onboarding.OnboardingPreferences
import com.orbit.app.onboarding.ReducedModeActivity
import com.orbit.app.orbit.AskOrbitViewModel
import com.orbit.app.orbit.BinderAskOrbitRepository
import com.orbit.app.settings.SettingsActivity
import com.orbit.app.ui.MainActivity
import com.orbit.app.ui.theme.OrbitTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * T052 + T053 — launcher entry point for User Story 2.
 *
 * Owns the [BinderDiaryRepository] binding lifecycle (onStart/onStop) and
 * hosts [DiaryScreen] with a [DiaryViewModel] wired to today's ISO date.
 *
 * T055 — the `:ml` bind is **pre-warmed** in `onCreate` via an async
 * `lifecycleScope.launch`, so by the time the user's eye reaches the
 * screen the service connection is ready and the first day-page emission
 * arrives inside the P50 ≤ 1 s target.
 */
class DiaryActivity : ComponentActivity() {

    private lateinit var repository: BinderDiaryRepository
    private lateinit var libraryRepository: BinderLibraryRepository
    private lateinit var askOrbitRepository: BinderAskOrbitRepository
    private lateinit var viewModel: DiaryViewModel
    private lateinit var libraryViewModel: LibraryViewModel
    private lateinit var askOrbitViewModel: AskOrbitViewModel
    private lateinit var pagingSource: DiaryPagingSource

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // T101 — first-run routing. If onboarding hasn't been completed,
        // hand off to OnboardingActivity; if the user elected reduced mode,
        // hand off to ReducedModeActivity. Both cases finish() this
        // activity so the back stack stays clean.
        val onboardingPrefs = OnboardingPreferences(this)
        if (!onboardingPrefs.completed) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        if (onboardingPrefs.reducedMode) {
            startActivity(Intent(this, ReducedModeActivity::class.java))
            finish()
            return
        }
        // T105 — dev-only diary-open counter (no-op in release).
        DebugCounters.incDiaryOpen(this)

        repository = BinderDiaryRepository(applicationContext)
        libraryRepository = BinderLibraryRepository(applicationContext)
        askOrbitRepository = BinderAskOrbitRepository(applicationContext)
        viewModel = DiaryViewModel(
            repository = repository,
            threadGrouper = ThreadGrouper(),
            dayHeaderGenerator = DayHeaderGenerator(LlmProviderRouter.createPreferLocal(this))
        )
        libraryViewModel = LibraryViewModel(repository = libraryRepository)
        askOrbitViewModel = AskOrbitViewModel(repository = askOrbitRepository)
        pagingSource = DiaryPagingSource(repository)

        // T055 — pre-warm the :ml binding off the main thread so first
        // render isn't blocked on bindService round-trip.
        // T054 — also pre-warm the :capture IActionExecutor binding so
        // the first action confirm tap doesn't pay the bindService
        // round-trip cost on the main thread.
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { repository.connect() }
            runCatching { repository.connectExecutor() }
            // T056 — fetch the first batch of older non-empty days in the
            // background so the pager has something to swipe into by the
            // time the user starts backscrolling.
            runCatching { pagingSource.loadMore() }
        }

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModel.observe(today)

        setContent {
            OrbitTheme {
                var tabName by rememberSaveable { mutableStateOf(OrbitHomeTab.DIARY.name) }
                var manualComposeDay by rememberSaveable { mutableStateOf<String?>(null) }
                val selectedTab = remember(tabName) {
                    runCatching { OrbitHomeTab.valueOf(tabName) }.getOrDefault(OrbitHomeTab.DIARY)
                }
                val bottomBar = @androidx.compose.runtime.Composable {
                    OrbitBottomBar(
                        selected = selectedTab,
                        onSelected = {
                            if (selectedTab == OrbitHomeTab.ORBIT && it != OrbitHomeTab.ORBIT) {
                                askOrbitViewModel.reset()
                            }
                            if (selectedTab == OrbitHomeTab.LIBRARY && it != OrbitHomeTab.LIBRARY) {
                                libraryViewModel.reset()
                            }
                            tabName = it.name
                        },
                    )
                }
                Surface(color = MaterialTheme.colorScheme.background) {
                    when (selectedTab) {
                        OrbitHomeTab.DIARY -> DiaryScreen(
                            viewModel = viewModel,
                            onOpenSetup = { openSetup() },
                            onOpenSettings = { openSettings() },
                            onOpenManualCompose = { dayLocal -> manualComposeDay = dayLocal },
                            pagingSource = pagingSource,
                            bottomBar = bottomBar,
                        )
                        OrbitHomeTab.LIBRARY -> Scaffold(
                            topBar = { QuietTabHeader(kicker = "// SAVED MEMORY", title = "Library") },
                            bottomBar = bottomBar,
                            containerColor = OrbitHomeColors.BgDeep,
                        ) { padding ->
                            LibraryScreen(
                                viewModel = libraryViewModel,
                                onOpenCapture = { envelopeId ->
                                    startActivity(
                                        EnvelopeDetailActivity.newIntent(
                                            this,
                                            envelopeId,
                                            dayLocal = null,
                                        )
                                    )
                                },
                                modifier = Modifier.padding(padding),
                            )
                        }
                        OrbitHomeTab.ORBIT -> Scaffold(
                            topBar = { QuietTabHeader(kicker = "// AGENT WORKSPACE", title = "Orbit") },
                            bottomBar = bottomBar,
                            containerColor = OrbitHomeColors.BgDeep,
                        ) { padding ->
                            OrbitCleanupScreen(
                                viewModel = viewModel,
                                askOrbitViewModel = askOrbitViewModel,
                                onOpenCapture = { envelopeId ->
                                    startActivity(
                                        EnvelopeDetailActivity.newIntent(
                                            this,
                                            envelopeId,
                                            dayLocal = null,
                                        )
                                    )
                                },
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }
                    manualComposeDay?.let { dayLocal ->
                        val manualComposeViewModel = remember(dayLocal) {
                            ManualComposeViewModel(
                                repository = repository,
                                dayLocal = dayLocal,
                            )
                        }
                        ManualComposeDialog(
                            viewModel = manualComposeViewModel,
                            onDismiss = { manualComposeDay = null },
                            onOpenCapture = { envelopeId ->
                                startActivity(
                                    EnvelopeDetailActivity.newIntent(
                                        this,
                                        envelopeId,
                                        dayLocal = null,
                                    )
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    private fun openSetup() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onDestroy() {
        // Guard: when first-run routing finish()es in onCreate before the
        // repository is constructed, onDestroy still runs. Accessing the
        // lateinit property in that path threw UninitializedPropertyAccessException
        // and crashed the app on launch.
        if (::repository.isInitialized) {
            repository.disconnect()
        }
        if (::libraryRepository.isInitialized) {
            libraryRepository.disconnect()
        }
        if (::askOrbitRepository.isInitialized) {
            askOrbitRepository.disconnect()
        }
        super.onDestroy()
    }
}

internal enum class OrbitHomeTab(
    val label: String,
) {
    DIARY("Diary"),
    LIBRARY("Library"),
    ORBIT("Orbit")
}

internal object OrbitHomeTestTags {
    const val BOTTOM_BAR = "orbit-bottom-bar"
    const val DIARY_TAB = "orbit-tab-diary"
    const val LIBRARY_TAB = "orbit-tab-library"
    const val ORBIT_TAB = "orbit-tab-orbit"
}

/**
 * Quiet Almanac palette for the home chrome (nav bar + tab headers). Mirrors
 * the diary/settings surfaces; kept local because those objects are private to
 * their files. Zero Material icons — editorial typography only (design bible).
 */
private object OrbitHomeColors {
    val BgDeep = Color(0xFF080B14)
    val Cream = Color(0xFFF3EAD8)
    val CreamDim = Color(0x8CF3EAD8)
    val Rule = Color(0x1AF3EAD8)
    val Accent = Color(0xFFE8B06A)
}

/**
 * Quiet Almanac header for the Library / Orbit tabs — a mono kicker + serif
 * title + hairline rule, matching the Diary's wordmark treatment instead of a
 * bare Material [androidx.compose.material3.TopAppBar].
 */
@androidx.compose.runtime.Composable
internal fun QuietTabHeader(kicker: String, title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitHomeColors.BgDeep)
            .statusBarsPadding()
            .padding(start = 24.dp, end = 18.dp, top = 20.dp),
    ) {
        MonoLabel(
            text = kicker,
            color = OrbitHomeColors.CreamDim,
            size = 9.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Text(
            text = title,
            color = OrbitHomeColors.Cream,
            style = TextStyle(
                fontFamily = OrbitType.QuietAlmanac.displaySerif,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.padding(bottom = 14.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OrbitHomeColors.Rule),
        )
    }
}

@androidx.compose.runtime.Composable
internal fun OrbitBottomBar(
    selected: OrbitHomeTab,
    onSelected: (OrbitHomeTab) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitHomeColors.BgDeep)
            .testTag(OrbitHomeTestTags.BOTTOM_BAR),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OrbitHomeColors.Rule),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            QuietTab(OrbitHomeTab.DIARY.label, selected == OrbitHomeTab.DIARY, OrbitHomeTestTags.DIARY_TAB) {
                onSelected(OrbitHomeTab.DIARY)
            }
            QuietTab(OrbitHomeTab.LIBRARY.label, selected == OrbitHomeTab.LIBRARY, OrbitHomeTestTags.LIBRARY_TAB) {
                onSelected(OrbitHomeTab.LIBRARY)
            }
            QuietTab(OrbitHomeTab.ORBIT.label, selected == OrbitHomeTab.ORBIT, OrbitHomeTestTags.ORBIT_TAB) {
                onSelected(OrbitHomeTab.ORBIT)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun QuietTab(
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        // Selected marker — a small wax-seal dot, not a Material icon.
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (isSelected) OrbitHomeColors.Accent else Color.Transparent),
        )
        Text(
            text = label,
            color = if (isSelected) OrbitHomeColors.Cream else OrbitHomeColors.CreamDim,
            style = TextStyle(
                fontFamily = OrbitType.QuietAlmanac.displaySerif,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontStyle = if (isSelected) FontStyle.Italic else FontStyle.Normal,
            ),
        )
    }
}
