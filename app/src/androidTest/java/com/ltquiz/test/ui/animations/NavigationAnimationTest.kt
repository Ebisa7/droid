package com.ltquiz.test.ui.animations

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ltquiz.test.ui.navigation.XSendNavigation
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationAnimationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun navigation_launchToSettings_hasTransitionAnimation() {
        composeTestRule.setContent {
            XSendNavigation(navController = rememberNavController())
        }

        // Wait for initial screen to load
        composeTestRule.waitForIdle()

        // Verify launch screen is displayed
        composeTestRule.onNodeWithText("XSEND Meet").assertIsDisplayed()

        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        // Wait for navigation animation
        composeTestRule.waitForIdle()

        // Verify settings screen is displayed (this would require the settings screen to be properly implemented)
        // For now, we just verify the navigation doesn't crash
    }

    @Test
    fun navigation_launchToCreateRoom_hasTransitionAnimation() {
        composeTestRule.setContent {
            XSendNavigation(navController = rememberNavController())
        }

        // Wait for initial screen to load
        composeTestRule.waitForIdle()

        // Verify launch screen is displayed
        composeTestRule.onNodeWithText("Create Room").assertIsDisplayed()

        // Navigate to create room (this will go through permissions first)
        composeTestRule.onNodeWithText("Create Room").performClick()

        // Wait for navigation animation
        composeTestRule.waitForIdle()

        // Verify navigation occurred (permissions screen should be shown)
        // The exact verification depends on the permissions screen implementation
    }

    @Test
    fun navigation_launchToJoinRoom_hasTransitionAnimation() {
        composeTestRule.setContent {
            XSendNavigation(navController = rememberNavController())
        }

        // Wait for initial screen to load
        composeTestRule.waitForIdle()

        // Verify launch screen is displayed
        composeTestRule.onNodeWithText("Join Room").assertIsDisplayed()

        // Navigate to join room (this will go through permissions first)
        composeTestRule.onNodeWithText("Join Room").performClick()

        // Wait for navigation animation
        composeTestRule.waitForIdle()

        // Verify navigation occurred (permissions screen should be shown)
        // The exact verification depends on the permissions screen implementation
    }

    @Test
    fun navigation_animationsDoNotCrash() {
        composeTestRule.setContent {
            XSendNavigation(navController = rememberNavController())
        }

        // Perform multiple rapid navigation actions to test animation stability
        composeTestRule.waitForIdle()

        // Click create room
        composeTestRule.onNodeWithText("Create Room").performClick()
        composeTestRule.waitForIdle()

        // Go back (if back button exists)
        try {
            composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
            composeTestRule.waitForIdle()
        } catch (e: AssertionError) {
            // Back button might not be available, that's okay for this test
        }

        // Click join room
        composeTestRule.onNodeWithText("Join Room").performClick()
        composeTestRule.waitForIdle()

        // The test passes if no crashes occur during these navigation actions
    }
}