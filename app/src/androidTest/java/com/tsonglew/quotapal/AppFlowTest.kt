package com.tsonglew.quotapal

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
        compose.onNodeWithTag("add-slim-widget-button").assertExists()
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
    @Test fun backgroundGuideCanSwitchBrandsAndCollapse() {
        compose.onNodeWithTag("tab-settings").performClick()
        compose.onNodeWithTag("background-guide-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("background-brand-picker").performScrollTo().performClick()
        compose.onNodeWithText("小米 / Redmi / POCO").performClick()
        compose.onNodeWithText("1. 允许自启动：", substring = true).performScrollTo().assertIsDisplayed()
        screenshot("background-guide-xiaomi")
        compose.onNodeWithTag("background-brand-picker").performScrollTo().performClick()
        compose.onNodeWithText("Samsung").performClick()
        compose.onNodeWithText("1. 排除休眠：", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("background-app-settings").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("background-guide-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("background-brand-picker").assertDoesNotExist()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        saveDeviceScreenshot(compose.activity, name, compose.onRoot().captureToImage().asAndroidBitmap())
    }
}
