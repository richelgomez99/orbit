package com.orbit.app.ui.primitives

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Polish audit Batch A — the Quiet back affordance must be reachable and
 * actionable by content description (TalkBack), not just visually. This
 * is the regression guard for the "bare Text('‹') announces nothing" bug.
 */
class QuietBackButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun backButton_hasSpokenLabel_andInvokesOnClick() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                QuietBackButton(onClick = { clicked = true }, color = Color.White)
            }
        }

        composeRule.onNodeWithContentDescription("Back")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertTrue("back button click must invoke onClick", clicked)
    }

    @Test
    fun backButton_customLabel_isSpoken() {
        composeRule.setContent {
            MaterialTheme {
                QuietBackButton(onClick = {}, color = Color.White, label = "Back to Diary")
            }
        }
        composeRule.onNodeWithContentDescription("Back to Diary").assertIsDisplayed()
    }
}
