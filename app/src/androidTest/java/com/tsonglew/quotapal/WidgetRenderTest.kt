package com.tsonglew.quotapal

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.tsonglew.quotapal.data.UsageParser
import com.tsonglew.quotapal.data.absoluteTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WidgetRenderTest {
    @Test fun slimWidgetsRenderOneRowQuotaStatesAndIndependentSettings() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.quotaApp
        fun shell(command: String) { instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        } }
        shell("appwidget grantbind --package ${context.packageName} --user 0")
        instrumentation.uiAutomation.adoptShellPermissionIdentity(android.Manifest.permission.BIND_APPWIDGET)
        try {
            runBlocking { app.repository.demo() }
            val cases = listOf(
                Triple(130, 70, "light"), Triple(140, 70, "dark"), Triple(280, 70, "light"), Triple(260, 50, "dark"),
            )
            cases.forEach { (width, height, theme) ->
                ActivityScenario.launch<WidgetTestHostActivity>(Intent(context, WidgetTestHostActivity::class.java)
                    .putExtra("width", width).putExtra("height", height).putExtra("slim", true)).use { scenario ->
                    var id = 0
                    scenario.onActivity { activity ->
                        id = activity.widgetId
                        val info = android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetInfo(id)
                        if (android.os.Build.VERSION.SDK_INT >= 31) {
                            assertEquals(2, info.targetCellWidth)
                            assertEquals(1, info.targetCellHeight)
                        }
                    }
                    runBlocking { app.settings.saveWidget(id, true, theme); app.updateWidgets() }
                    fun awaitText(expected: String) {
                        val deadline = System.currentTimeMillis() + 20_000
                        var matched = false
                        while (!matched && System.currentTimeMillis() < deadline) {
                            instrumentation.waitForIdleSync()
                            scenario.onActivity { activity ->
                                matched = text(activity.widgetView).contains(expected) &&
                                    texts(activity.widgetView).filter { it.text.isNotEmpty() }.all { it.layout != null }
                            }
                            if (!matched) Thread.sleep(100)
                        }
                        var actual = ""
                        scenario.onActivity { actual = text(it.widgetView) }
                        assertTrue("Slim ${width}x$height missing $expected; actual $actual", matched)
                    }
                    fun capture(name: String) {
                        lateinit var bitmap: Bitmap
                        val copied = CountDownLatch(1)
                        var copyResult = -1
                        scenario.onActivity { activity ->
                            val view = activity.widgetView
                            for (label in texts(view).filter { it.text.isNotEmpty() }) {
                                val visible = Rect()
                                assertTrue("Slim text must be visible: ${label.text}", label.getLocalVisibleRect(visible))
                                assertEquals("Slim text clipped at ${width}x$height: ${label.text}", label.height, visible.height())
                                val layout = requireNotNull(label.layout)
                                assertTrue("Slim text ellipsized: ${label.text}", (0 until layout.lineCount).all { layout.getEllipsisCount(it) == 0 })
                            }
                            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                            val location = IntArray(2)
                            view.getLocationInWindow(location)
                            PixelCopy.request(activity.window, Rect(location[0], location[1], location[0] + view.width, location[1] + view.height),
                                bitmap, { result -> copyResult = result; copied.countDown() }, Handler(Looper.getMainLooper()))
                        }
                        assertTrue(copied.await(5, TimeUnit.SECONDS)); assertEquals(PixelCopy.SUCCESS, copyResult)
                        saveDeviceScreenshot(context, name, bitmap)
                    }
                    awaitText("62%")
                    awaitText("示例")
                    scenario.onActivity { assertFalse("Only the primary quota should be shown", text(it.widgetView).contains("36%")) }
                    capture("widget-slim-${width}x$height-$theme")
                    runBlocking { app.settings.saveWidget(id, false, theme); app.updateWidgets() }
                    awaitText("38%")
                    awaitText("已用")
                    // A second provider instance must retain its own theme and percentage preference.
                    run {
                        var standardId = 0
                        scenario.onActivity { standardId = it.addStandardWidget() }
                        runBlocking {
                            app.settings.saveWidget(standardId, true, "light"); app.updateWidgets()
                            assertEquals(false to theme, app.settings.widgetSettings(id))
                        }
                        val deadline = System.currentTimeMillis() + 20_000
                        var standardMatched = false
                        while (!standardMatched && System.currentTimeMillis() < deadline) {
                            instrumentation.waitForIdleSync()
                            scenario.onActivity { standardMatched = text(requireNotNull(it.secondaryWidgetView)).contains("62%") }
                            if (!standardMatched) Thread.sleep(100)
                        }
                        assertTrue("Both widget providers must receive shared snapshot updates", standardMatched)
                        scenario.onActivity { it.removeStandardWidget() }
                        runBlocking { app.settings.deleteWidget(standardId) }
                    }
                    awaitText("38%")
                    val states = listOf(
                        "available" to """{"rate_limit":{"allowed":true}}""",
                        "unreported" to """{"plan_type":"pro","rate_limit":null}""",
                        "review-only" to """{"rate_limit":null,"additional_rate_limits":[{"metered_feature":"review","rate_limit":{"primary_window":{"used_percent":20}}}]}""",
                        "unknown" to """{"rate_limit":{"primary_window":{"limit_window_seconds":18000}}}""",
                        "restricted" to """{"rate_limit":{"allowed":false,"primary_window":{"used_percent":20}}}""",
                    )
                    val expected = listOf("当前可用", "未提供周期额度", "未提供周期额度", "暂不可用", "使用受限")
                    states.forEachIndexed { index, (name, body) ->
                        val fetchedAt = 1000L + index * 60
                        runBlocking { app.repository.demo(UsageParser.parse(body, "demo", fetchedAt)); app.updateWidgets() }
                        awaitText(absoluteTime(fetchedAt, true))
                        awaitText(expected[index])
                        scenario.onActivity { assertFalse("Do not show a percentage for $name", text(it.widgetView).contains("%")) }
                        capture("widget-slim-$name-${width}x$height")
                    }
                    runBlocking { app.repository.logout(); app.updateWidgets() }
                    awaitText("未连接")
                    awaitText("轻点连接账号")
                    capture("widget-slim-logged-out-${width}x$height")
                    runBlocking { app.settings.deleteWidget(id); app.repository.demo() }
                }
            }
        } finally {
            runBlocking { app.repository.logout(); app.reconcileSync() }
            instrumentation.uiAutomation.dropShellPermissionIdentity()
            shell("appwidget revokebind --package ${context.packageName} --user 0")
        }
    }

    @Test fun missingQuotaStatesRenderAcrossAllWidgetSizes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.quotaApp
        fun shell(command: String) { instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        } }
        val fixtures = listOf(
            Triple("no-windows", """{"plan_type":"pro","rate_limit":{"allowed":true,"limit_reached":false}}""", "当前可用"),
            Triple("unreported", """{"plan_type":"pro","rate_limit":null}""", "未提供周期额度"),
            Triple("weekly-only", """{"plan_type":"pro","rate_limit":{"secondary_window":{"used_percent":38,"limit_window_seconds":604800,"reset_at":2000}}}""", "无短周期数据"),
            Triple("unknown-usage", """{"rate_limit":{"primary_window":{"limit_window_seconds":18000}}}""", "暂不可用"),
            Triple("restricted", """{"rate_limit":{"allowed":false,"limit_reached":true}}""", "当前使用受限"),
        )
        shell("appwidget grantbind --package ${context.packageName} --user 0")
        instrumentation.uiAutomation.adoptShellPermissionIdentity(android.Manifest.permission.BIND_APPWIDGET)
        try {
            for ((name, body, expected) in fixtures) {
                runBlocking { app.repository.demo(UsageParser.parse(body, "demo", 1000)) }
                for ((width, height) in listOf(140 to 150, 280 to 150, 140 to 230)) {
                    ActivityScenario.launch<WidgetTestHostActivity>(Intent(context, WidgetTestHostActivity::class.java)
                        .putExtra("width", width).putExtra("height", height)).use { scenario ->
                        var id = 0
                        scenario.onActivity { id = it.widgetId }
                        runBlocking { app.settings.saveWidget(id, true, "light"); app.updateWidgets() }
                        val deadline = System.currentTimeMillis() + 20_000
                        var matched = false
                        while (!matched && System.currentTimeMillis() < deadline) {
                            instrumentation.waitForIdleSync()
                            scenario.onActivity { activity ->
                                matched = text(activity.widgetView).contains(expected) && text(activity.widgetView).contains("示例数据") &&
                                    texts(activity.widgetView).filter { it.text.isNotEmpty() }.all { it.layout != null }
                            }
                            if (!matched) Thread.sleep(100)
                        }
                        assertTrue("$name widget ${width}x$height missing $expected", matched)
                        lateinit var bitmap: Bitmap
                        val copied = CountDownLatch(1)
                        var copyResult = -1
                        scenario.onActivity { activity ->
                            val view = activity.widgetView
                            val labels = texts(view)
                            for (label in labels.filter { it.text.contains(expected) || it.text.startsWith("示例数据") }) {
                                val visible = Rect()
                                assertTrue(label.getLocalVisibleRect(visible))
                                assertEquals("$name label must not be clipped at ${width}x$height", label.height, visible.height())
                                val layout = requireNotNull(label.layout)
                                assertTrue("$name text must not be ellipsized", (0 until layout.lineCount).all { layout.getEllipsisCount(it) == 0 })
                            }
                            val visible = text(view)
                            if (name == "weekly-only") assertTrue(visible.contains("62%"))
                            else assertFalse("Do not invent a percentage", visible.contains("%"))
                            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                            val location = IntArray(2)
                            view.getLocationInWindow(location)
                            PixelCopy.request(activity.window, Rect(location[0], location[1], location[0] + view.width, location[1] + view.height),
                                bitmap, { result -> copyResult = result; copied.countDown() }, Handler(Looper.getMainLooper()))
                        }
                        assertTrue(copied.await(5, TimeUnit.SECONDS))
                        assertEquals(PixelCopy.SUCCESS, copyResult)
                        saveDeviceScreenshot(context, "widget-$name-${width}x$height", bitmap)
                        runBlocking { app.settings.deleteWidget(id) }
                    }
                }
            }
        } finally {
            runBlocking { app.repository.logout(); app.reconcileSync() }
            instrumentation.uiAutomation.dropShellPermissionIdentity()
            shell("appwidget revokebind --package ${context.packageName} --user 0")
        }
    }

    @Test fun realWidgetsRenderCompactWideTallAndClearOnLogout() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.quotaApp
        fun shell(command: String) { instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        } }
        shell("appwidget grantbind --package ${context.packageName} --user 0")
        instrumentation.uiAutomation.adoptShellPermissionIdentity(android.Manifest.permission.BIND_APPWIDGET)
        try {
            runBlocking { app.repository.demo() }
            listOf(Triple(140, 150, "light"), Triple(280, 150, "dark"), Triple(140, 230, "light")).forEach { (width, height, theme) ->
                ActivityScenario.launch<WidgetTestHostActivity>(Intent(context, WidgetTestHostActivity::class.java)
                    .putExtra("width", width).putExtra("height", height)).use { scenario ->
                    var id = 0
                    scenario.onActivity { id = it.widgetId }
                    runBlocking { app.settings.saveWidget(id, true, theme); app.updateWidgets() }
                    fun awaitText(expected: String) {
                        val deadline = System.currentTimeMillis() + 20_000
                        var matched = false
                        while (!matched && System.currentTimeMillis() < deadline) {
                            instrumentation.waitForIdleSync()
                            scenario.onActivity { activity ->
                                matched = text(activity.widgetView).contains(expected) &&
                                    texts(activity.widgetView).filter { it.text.isNotEmpty() }.all { it.layout != null }
                            }
                            if (!matched) Thread.sleep(100)
                        }
                        var actual = ""
                        scenario.onActivity { actual = "${it.widgetView.width}×${it.widgetView.height}: ${text(it.widgetView)}" }
                        assertTrue("Widget ${width}x$height missing $expected; actual $actual", matched)
                    }
                    awaitText("示例数据")
                    if (width >= 280 || height >= 230) awaitText("36%")
                    lateinit var bitmap: Bitmap
                    val copied = CountDownLatch(1)
                    var copyResult = -1
                    scenario.onActivity { activity ->
                        val visible = text(activity.widgetView)
                        val view = activity.widgetView
                        assertTrue("Missing primary quota: $visible", visible.contains("62%"))
                        if (width >= 280 || height >= 230) assertTrue("Missing secondary quota: $visible", visible.contains("36%"))
                        val timestamp = texts(view).single { it.text.startsWith("示例数据") }
                        val visibleTimestamp = Rect()
                        assertTrue(timestamp.getLocalVisibleRect(visibleTimestamp))
                        assertEquals("Timestamp must not be clipped at ${width}x$height", timestamp.height, visibleTimestamp.height())
                        bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                        val location = IntArray(2)
                        view.getLocationInWindow(location)
                        PixelCopy.request(activity.window, Rect(location[0], location[1], location[0] + view.width, location[1] + view.height),
                            bitmap, { result -> copyResult = result; copied.countDown() }, Handler(Looper.getMainLooper()))
                    }
                    assertTrue("Native widget capture timed out", copied.await(5, TimeUnit.SECONDS))
                    assertEquals(PixelCopy.SUCCESS, copyResult)
                    saveDeviceScreenshot(context, "widget-${width}x$height-$theme", bitmap)
                    runBlocking { app.repository.logout(); app.updateWidgets() }
                    awaitText("连接你的 Codex")
                    runBlocking { app.settings.deleteWidget(id); app.repository.demo() }
                }
            }
        } finally {
            runBlocking { app.repository.logout(); app.reconcileSync() }
            instrumentation.uiAutomation.dropShellPermissionIdentity()
            shell("appwidget revokebind --package ${context.packageName} --user 0")
        }
    }

    private fun text(view: View): String = when (view) {
        is TextView -> view.text.toString()
        is ViewGroup -> (0 until view.childCount).joinToString(" ") { text(view.getChildAt(it)) }
        else -> ""
    }
    private fun texts(view: View): List<TextView> = when (view) {
        is TextView -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
        else -> emptyList()
    }
}
