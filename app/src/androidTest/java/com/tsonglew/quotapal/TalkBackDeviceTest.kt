package com.tsonglew.quotapal

import android.Manifest
import android.app.UiAutomation
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.tsonglew.quotapal.data.Session
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit emulator probe: requires installed TalkBack and temporarily enables it. */
class TalkBackDeviceTest {
    @Test fun talkBackFocusAndDoubleTapRefreshBothWidgetProviders() {
        assumeTrue("Run explicitly with -e talkbackProbe true",
            InstrumentationRegistry.getArguments().getString("talkbackProbe") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val context = instrumentation.targetContext
        val app = context.quotaApp as TestQuotaApplication
        fun shell(command: String): String = automation.executeShellCommand(command).use {
            java.io.FileInputStream(it.fileDescriptor).bufferedReader().use { reader -> reader.readText().trim() }
        }
        val previousServices = shell("settings get secure enabled_accessibility_services")
        val previousEnabled = shell("settings get secure accessibility_enabled")
        val previousScale = shell("settings get system font_scale")
        val accessibility = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        fun await(message: String, condition: () -> Boolean) {
            val deadline = SystemClock.uptimeMillis() + 20_000
            while (SystemClock.uptimeMillis() < deadline) {
                if (condition()) return
                Thread.sleep(100)
            }
            fail(message)
        }
        fun descendants(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> =
            listOf(node) + (0 until node.childCount).flatMap { node.getChild(it)?.let(::descendants).orEmpty() }
        fun doubleTap(bounds: Rect) {
            repeat(2) {
                val start = SystemClock.uptimeMillis()
                for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                    val event = MotionEvent.obtain(start, SystemClock.uptimeMillis(), action,
                        bounds.exactCenterX(), bounds.exactCenterY(), 0).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
                    try { assertTrue(automation.injectInputEvent(event, true)) } finally { event.recycle() }
                    Thread.sleep(50)
                }
            }
        }
        automation.adoptShellPermissionIdentity(Manifest.permission.BIND_APPWIDGET)
        try {
            shell("settings put system font_scale 2.0")
            shell("settings put secure enabled_accessibility_services com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService")
            shell("settings put secure accessibility_enabled 1")
            await("TalkBack must actually enable touch exploration") { accessibility.isTouchExplorationEnabled }
            assertTrue("The actual TalkBack service must be bound", accessibility.getEnabledAccessibilityServiceList(
                android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_SPOKEN).any {
                it.resolveInfo.serviceInfo.packageName == "com.google.android.marvin.talkback"
            })
            await("The platform must apply large fonts") { context.resources.configuration.fontScale >= 1.9f }
            for (slim in listOf(false, true)) {
                runBlocking {
                    app.repository.logout()
                    app.usedPercent = 38
                    app.repository.connect(Session("test-access", "test-refresh", "test-account", Long.MAX_VALUE))
                    app.settings.syncResult(null, lastAttemptAt = 0)
                }
                ActivityScenario.launch<WidgetTestHostActivity>(Intent(context, WidgetTestHostActivity::class.java)
                    .putExtra("slim", slim).putExtra("width", 280).putExtra("height", if (slim) 70 else 150)).use {
                    runBlocking { app.updateWidgets() }
                    var refresh: AccessibilityNodeInfo? = null
                    await("TalkBack tree must expose quota, success time and refresh") {
                        val root = automation.rootInActiveWindow ?: return@await false
                        val nodes = descendants(root)
                        refresh = nodes.firstOrNull { node -> node.contentDescription?.toString() == "刷新额度" }
                        nodes.any { node -> node.contentDescription?.toString()?.let { description ->
                            description.contains("62%") && description.contains("最后成功更新于")
                        } == true } && refresh != null
                    }
                    var target = requireNotNull(refresh)
                    while (!target.isClickable && target.parent != null) target = target.parent
                    assertTrue("Refresh must have its own clickable accessibility target", target.isClickable)
                    assertTrue("TalkBack must focus the refresh control", target.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS))
                    await("TalkBack accessibility focus must be present") {
                        automation.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY) != null
                    }
                    saveDeviceScreenshot(context, "talkback-${if (slim) "slim" else "standard"}-focus",
                        requireNotNull(automation.takeScreenshot()))
                    val bounds = Rect().also(target::getBoundsInScreen)
                    assertFalse(bounds.isEmpty)
                    val before = app.usageRequests.get()
                    app.usedPercent = 17
                    doubleTap(bounds)
                    await("TalkBack double-tap must fetch a new quota") {
                        app.repository.state.value.snapshot?.windows?.singleOrNull()?.usedPercent == 17.0
                    }
                    await("Refreshed quota must reach the accessibility tree") {
                        automation.rootInActiveWindow?.let(::descendants)?.any { node ->
                            node.contentDescription?.toString()?.contains("83%") == true
                        } == true
                    }
                    assertEquals("One TalkBack activation shares one request", before + 1, app.usageRequests.get())
                    saveDeviceScreenshot(context, "talkback-${if (slim) "slim" else "standard"}-updated",
                        requireNotNull(automation.takeScreenshot()))
                }
            }
        } finally {
            for ((key, value) in listOf("enabled_accessibility_services" to previousServices, "accessibility_enabled" to previousEnabled)) {
                shell(if (value == "null") "settings delete secure $key" else "settings put secure $key $value")
            }
            shell(if (previousScale == "null") "settings delete system font_scale" else "settings put system font_scale $previousScale")
            runBlocking { app.repository.logout(); app.reconcileSync(); app.updateWidgets() }
            automation.dropShellPermissionIdentity()
        }
    }
}
