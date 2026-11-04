package com.example.myalarmapp.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionOnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun permissionScreen_displaysElementsAndCallbacks() {
        var finished = false

        composeTestRule.setContent {
            PermissionOnboardingScreen(
                onFinishOnboarding = { finished = true }
            )
        }

        // Verify title or texts are displayed
        composeTestRule.onNodeWithText("🛡️ Security & Permissions Setup").assertExists()
        composeTestRule.onNodeWithText("Notifications").assertExists()
        composeTestRule.onNodeWithText("Camera (Secret Capture)").assertExists()

        // Click continue button
        composeTestRule.onNodeWithText("Continue to Dashboard 🚀").performClick()

        assertTrue(finished)
    }
}
