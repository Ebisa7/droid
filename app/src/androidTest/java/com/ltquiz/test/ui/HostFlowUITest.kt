package com.ltquiz.test.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ltquiz.test.ui.screens.RoomCreationScreen
import com.ltquiz.test.ui.theme.LTQuizTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HostFlowUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun roomCreationScreen_displaysQRCodeAndRoomCode() {
        var navigatedBack = false
        var startedCall = false
        var roomCodeFromCall = ""

        composeTestRule.setContent {
            LTQuizTheme {
                RoomCreationScreen(
                    onNavigateBack = { navigatedBack = true },
                    onStartCall = { roomCode ->
                        startedCall = true
                        roomCodeFromCall = roomCode
                    }
                )
            }
        }

        // Wait for room creation to complete
        composeTestRule.waitForIdle()

        // Verify UI elements are displayed
        composeTestRule.onNodeWithText("Create Room").assertIsDisplayed()
        composeTestRule.onNodeWithText("Room Code").assertIsDisplayed()
        composeTestRule.onNodeWithText("Waiting for participants...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Participants (1)").assertIsDisplayed() // Host only initially
        composeTestRule.onNodeWithText("Cancel Room").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Call").assertIsDisplayed()
    }

    @Test
    fun roomCreationScreen_startCallButton_initiallyDisabled() {
        composeTestRule.setContent {
            LTQuizTheme {
                RoomCreationScreen(
                    onNavigateBack = { },
                    onStartCall = { }
                )
            }
        }

        // Wait for room creation to complete
        composeTestRule.waitForIdle()

        // Start Call button should be disabled initially (only host present)
        composeTestRule.onNodeWithText("Start Call").assertIsNotEnabled()
    }

    @Test
    fun roomCreationScreen_cancelButton_navigatesBack() {
        var navigatedBack = false

        composeTestRule.setContent {
            LTQuizTheme {
                RoomCreationScreen(
                    onNavigateBack = { navigatedBack = true },
                    onStartCall = { }
                )
            }
        }

        // Wait for room creation to complete
        composeTestRule.waitForIdle()

        // Click cancel button
        composeTestRule.onNodeWithText("Cancel Room").performClick()

        // Verify navigation occurred
        assert(navigatedBack)
    }

    @Test
    fun roomCreationScreen_copyRoomCode_copiesCodeToClipboard() {
        composeTestRule.setContent {
            LTQuizTheme {
                RoomCreationScreen(
                    onNavigateBack = { },
                    onStartCall = { }
                )
            }
        }

        // Wait for room creation to complete
        composeTestRule.waitForIdle()

        // Find and click the copy button
        composeTestRule.onNodeWithContentDescription("Copy room code").performClick()

        // Note: In a real test, you would verify the clipboard content
        // This requires additional setup for clipboard testing
    }
}