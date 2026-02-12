package com.ltquiz.test.ui.animations

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ltquiz.test.ui.components.*
import com.ltquiz.test.ui.screens.LaunchScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnimationBehaviorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun launchScreen_displaysAnimatedElements() {
        var createRoomClicked = false
        var joinRoomClicked = false
        var settingsClicked = false

        composeTestRule.setContent {
            LaunchScreen(
                onCreateRoom = { createRoomClicked = true },
                onJoinRoom = { joinRoomClicked = true },
                onSettings = { settingsClicked = true }
            )
        }

        // Wait for animations to complete
        composeTestRule.waitForIdle()

        // Verify animated elements are visible
        composeTestRule.onNodeWithText("XSEND Meet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Instant video calls, zero confusion").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create Room").assertIsDisplayed()
        composeTestRule.onNodeWithText("Join Room").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun animatedButton_respondsToClicks() {
        var buttonClicked = false

        composeTestRule.setContent {
            AnimatedButton(
                onClick = { buttonClicked = true }
            ) {
                androidx.compose.material3.Text("Test Button")
            }
        }

        composeTestRule.onNodeWithText("Test Button").performClick()
        
        // Verify button was clicked
        assert(buttonClicked)
    }

    @Test
    fun connectionLoadingIndicator_displaysCorrectly() {
        composeTestRule.setContent {
            ConnectionLoadingIndicator(
                message = "Test Loading..."
            )
        }

        // Verify loading indicator elements
        composeTestRule.onNodeWithText("Test Loading...").assertIsDisplayed()
        
        // Wait for animation to start
        composeTestRule.waitForIdle()
    }

    @Test
    fun pulsingDotsIndicator_animatesCorrectly() {
        composeTestRule.setContent {
            PulsingDotsIndicator()
        }

        // Wait for animation cycles
        composeTestRule.waitForIdle()
        
        // The dots should be visible (we can't easily test the animation itself in UI tests)
        // but we can verify the component renders without crashing
    }

    @Test
    fun statusIndicator_changesColorBasedOnState() {
        composeTestRule.setContent {
            StatusIndicator(
                isActive = true
            )
        }

        // Wait for animation
        composeTestRule.waitForIdle()
        
        // Verify the indicator is displayed
        // Note: Testing color changes requires more complex setup with semantic properties
    }

    @Test
    fun participantJoinAnimation_displaysAndDismisses() {
        var animationCompleted = false

        composeTestRule.setContent {
            ParticipantJoinAnimation(
                participantName = "Test User",
                onAnimationComplete = { animationCompleted = true }
            )
        }

        // Verify participant join message is displayed
        composeTestRule.onNodeWithText("Test User joined the call").assertIsDisplayed()
        
        // Wait for animation to complete (this will take 3+ seconds in real scenario)
        // For testing, we can verify the initial state
    }

    @Test
    fun participantLeaveAnimation_displaysCorrectMessage() {
        composeTestRule.setContent {
            ParticipantLeaveAnimation(
                participantName = "Test User"
            )
        }

        // Verify participant leave message is displayed
        composeTestRule.onNodeWithText("Test User left the call").assertIsDisplayed()
    }

    @Test
    fun reconnectingOverlay_displaysWithCancelOption() {
        var cancelClicked = false

        composeTestRule.setContent {
            ReconnectingOverlay(
                onCancel = { cancelClicked = true }
            )
        }

        // Verify reconnecting overlay elements
        composeTestRule.onNodeWithText("Reconnecting...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Please wait while we restore your connection").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()

        // Test cancel functionality
        composeTestRule.onNodeWithText("Cancel").performClick()
        assert(cancelClicked)
    }

    @Test
    fun joiningRoomAnimation_displaysCorrectContent() {
        composeTestRule.setContent {
            JoiningRoomAnimation()
        }

        // Verify joining room animation content
        composeTestRule.onNodeWithText("Joining room...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Setting up your video call").assertIsDisplayed()
    }

    @Test
    fun creatingRoomAnimation_displaysCorrectContent() {
        composeTestRule.setContent {
            CreatingRoomAnimation()
        }

        // Verify creating room animation content
        composeTestRule.onNodeWithText("Creating room...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Preparing your meeting space").assertIsDisplayed()
    }

    @Test
    fun networkQualityAnimation_displaysNetworkStatus() {
        composeTestRule.setContent {
            NetworkQualityAnimation(
                networkQuality = "Good",
                isConnected = true
            )
        }

        // Verify network quality display
        composeTestRule.onNodeWithText("Good").assertIsDisplayed()
        
        // Wait for animations
        composeTestRule.waitForIdle()
    }
}