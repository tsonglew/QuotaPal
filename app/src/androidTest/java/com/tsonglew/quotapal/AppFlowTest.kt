package com.tsonglew.quotapal

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun startDisconnected() {
        runBlocking { compose.activity.quotaApp.repository.logout() }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("connect-button").fetchSemanticsNodes().isNotEmpty() }
    }
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

    @Test fun privacyNoticeIsAvailableOfflineInSettingsAndBeforeLogin() {
        compose.onNodeWithTag("tab-settings").performClick()
        compose.onNodeWithTag("privacy-notice-button").performScrollTo().performClick()
        compose.onNodeWithTag("privacy-notice-text").assertTextContains("额度缓存本身不是加密数据库", substring = true)
        saveDeviceScreenshot(compose.activity, "privacy-notice", compose.onNode(isDialog()).captureToImage().asAndroidBitmap())
        compose.onNodeWithTag("privacy-notice-close").performClick()
        compose.onNodeWithTag("privacy-notice-text").assertDoesNotExist()
        compose.onNodeWithTag("tab-quota").performClick()
        compose.onNodeWithTag("connect-button").performClick()
        compose.onNodeWithTag("privacy-notice-button").performScrollTo().performClick()
        compose.onNodeWithTag("privacy-notice-text").assertTextContains("此操作不撤销远程授权", substring = true)
        compose.onNodeWithTag("privacy-notice-close").performClick()
        compose.onNodeWithText("取消", substring = false).performClick()
    }

    @Test fun diagnosticsCanPreviewAndClearWithoutUploading() {
        val diagnostics = compose.activity.quotaApp.diagnostics
        diagnostics.clear()
        diagnostics.record(com.tsonglew.quotapal.diagnostics.DiagnosticEvent.HTTP_START, 0)
        compose.onNodeWithTag("tab-settings").performClick()
        compose.onNodeWithTag("diagnostics-preview").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("diagnostics-report").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("diagnostics-report").assertTextContains("Hourly observations schema=1", substring = true)
        compose.onNodeWithTag("diagnostics-report").assertTextContains("HTTP_START 0", substring = true)
        compose.onNodeWithTag("diagnostics-close").performClick()
        compose.onNodeWithTag("diagnostics-clear").performScrollTo().performClick()
        compose.waitUntil(10_000) { !diagnostics.report().contains("HTTP_START 0") }
        compose.onNodeWithTag("diagnostics-preview").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("diagnostics-report").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("diagnostics-report").assertTextContains("Recent events", substring = true)
        compose.onNodeWithTag("diagnostics-close").performClick()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        saveDeviceScreenshot(compose.activity, name, compose.onRoot().captureToImage().asAndroidBitmap())
    }
}
