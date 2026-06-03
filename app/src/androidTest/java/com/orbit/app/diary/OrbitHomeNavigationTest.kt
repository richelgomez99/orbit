package com.orbit.app.diary

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OrbitHomeNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun bottomBarSelectsDiaryLibraryAndOrbitTabs() {
        val selections = mutableListOf<OrbitHomeTab>()
        composeRule.setContent {
            MaterialTheme {
                var selected by remember { mutableStateOf(OrbitHomeTab.DIARY) }
                OrbitBottomBar(
                    selected = selected,
                    onSelected = {
                        selected = it
                        selections += it
                    },
                )
            }
        }

        composeRule.onNodeWithTag(OrbitHomeTestTags.BOTTOM_BAR).assertIsDisplayed()

        composeRule.onNodeWithTag(OrbitHomeTestTags.LIBRARY_TAB).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(OrbitHomeTestTags.ORBIT_TAB).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(OrbitHomeTestTags.DIARY_TAB).performClick()
        composeRule.waitForIdle()

        assertEquals(
            listOf(OrbitHomeTab.LIBRARY, OrbitHomeTab.ORBIT, OrbitHomeTab.DIARY),
            selections,
        )
    }
}
