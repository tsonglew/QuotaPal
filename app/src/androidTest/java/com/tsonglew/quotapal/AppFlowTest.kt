package com.tsonglew.quotapal

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun demoThemeWidgetAndLogoutFlow() {
        compose.onNodeWithTag("demo-button").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("示例数据", substring = false).assertExists()
        screenshot("quota-light")
        compose.onNodeWithTag("tab-widgets").performClick()
        compose.onNodeWithTag("add-widget-button").assertExists()
        screenshot("widgets-light")
        compose.onNodeWithTag("tab-settings").performClick()
        compose.onNodeWithTag("theme-dark").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("tab-quota").performClick()
        compose.waitForIdle()
        screenshot("quota-dark")
        compose.onNodeWithTag("tab-settings").performClick()
        compose.onNodeWithTag("logout-button").performScrollTo().performClick()
        compose.onNodeWithTag("confirm-logout").performClick()
        compose.onNodeWithTag("tab-quota").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("connect-button").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("connect-button").assertExists()
        screenshot("welcome-dark")
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val folder = File(compose.activity.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
