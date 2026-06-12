package com.orbit.app.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.orbit.app.ui.theme.LocalRuntimeFlags
import com.orbit.app.ui.theme.RuntimeFlagValues
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun flagOffPauseToggle_preservesCallback() {
        var lastPauseValue: Boolean? = null

        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalRuntimeFlags provides RuntimeFlagValues(useNewVisualLanguage = false),
                ) {
                    SettingsScreen(
                        paused = false,
                        onPauseChange = { lastPauseValue = it },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsScreenTestTags.PAUSE_TOGGLE).performClick()
        composeRule.waitForIdle()

        assert(lastPauseValue == true) { "Flag-off pause toggle must call onPauseChange(true)" }
    }

    @Test
    fun quietFlagPauseToggle_preservesCallbackAndCopyContract() {
        var lastPauseValue: Boolean? = null
        var lastMemoryIndexValue: Boolean? = null
        var lastCloudAskValue: Boolean? = null
        var lastCloudAiValue: Boolean? = null
        var openedCaptureSetup = false

        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalRuntimeFlags provides RuntimeFlagValues(useNewVisualLanguage = true),
                ) {
                    SettingsScreen(
                        paused = false,
                        onPauseChange = { lastPauseValue = it },
                        memoryIndexingEnabled = false,
                        onMemoryIndexingChange = { lastMemoryIndexValue = it },
                        cloudAskSynthesisEnabled = true,
                        onCloudAskSynthesisChange = { lastCloudAskValue = it },
                        cloudAiRoutingEnabled = true,
                        onCloudAiRoutingChange = { lastCloudAiValue = it },
                        onOpenCaptureSetup = { openedCaptureSetup = true },
                        trashCount = 2,
                        onOpenTrash = {},
                        onOpenAuditLog = {},
                        onExportData = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("// PRINCIPLE I · DEFAULT PRIVACY").assertIsDisplayed()
        composeRule.onNodeWithText("WHERE YOUR CAPTURES THINK").assertIsDisplayed()
        composeRule.onNodeWithText("Compact memory index").assertIsDisplayed()
        composeRule.onNodeWithText("Cloud Ask synthesis").assertIsDisplayed()
        composeRule.onNodeWithText("Cloud AI routing").assertIsDisplayed()
        composeRule.onNodeWithText("Cloud activity").assertIsDisplayed()
        composeRule.onNodeWithText("Floating bubble").assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsScreenTestTags.CAPTURE_SETUP_ROW).performClick()
        composeRule.waitForIdle()
        assert(openedCaptureSetup) { "Quiet nested capture setup row must call onOpenCaptureSetup" }
        composeRule.onNodeWithText("Forget everything from before").assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsScreenTestTags.PAUSE_TOGGLE).performClick()
        composeRule.waitForIdle()

        assert(lastPauseValue == true) { "Quiet pause toggle must call onPauseChange(true)" }
        composeRule.onNodeWithTag(SettingsScreenTestTags.MEMORY_INDEX_TOGGLE).performClick()
        composeRule.onNodeWithTag(SettingsScreenTestTags.CLOUD_ASK_TOGGLE).performClick()
        composeRule.onNodeWithTag(SettingsScreenTestTags.CLOUD_AI_TOGGLE).performClick()
        composeRule.waitForIdle()

        assert(lastMemoryIndexValue == true) { "Memory index toggle must call onMemoryIndexingChange(true)" }
        assert(lastCloudAskValue == false) { "Cloud Ask toggle must call onCloudAskSynthesisChange(false)" }
        assert(lastCloudAiValue == false) { "Cloud AI toggle must call onCloudAiRoutingChange(false)" }
    }

    @Test
    fun quietActionsSettings_preservesSkillToggleCallback() {
        var toggleCall: Pair<String, Boolean>? = null

        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalRuntimeFlags provides RuntimeFlagValues(useNewVisualLanguage = true),
                ) {
                    ActionsSettingsUI(
                        rows = listOf(
                            SkillSettingsRow(
                                skillId = "todo",
                                displayName = "To-do",
                                enabled = false,
                                invocationCount = 0,
                                successRate = null,
                                cancelRate = null,
                                avgLatencyMs = null,
                            ),
                        ),
                        rememberedTodoPackage = "com.example.todo",
                        onToggleSkill = { skillId, enabled -> toggleCall = skillId to enabled },
                        onClearRememberedTodoTarget = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("// Orbit actions").assertIsDisplayed()
        composeRule.onNodeWithTag(REMEMBERED_TARGET_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(skillToggleTestTag("todo")).performClick()
        composeRule.waitForIdle()

        assert(toggleCall == "todo" to true) { "Quiet actions toggle must call onToggleSkill(todo, true)" }
    }
}
