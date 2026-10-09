package com.tsonglew.quotapal

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class WidgetRenderTest {
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
                            scenario.onActivity { matched = text(it.widgetView).contains(expected) }
                            if (!matched) Thread.sleep(100)
                        }
                        assertTrue("Widget missing $expected", matched)
                    }
                    awaitText("示例数据")
                    if (width >= 280 || height >= 230) awaitText("36%")
                    scenario.onActivity { activity ->
                        val visible = text(activity.widgetView)
                        val view = activity.widgetView
                        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                        view.draw(Canvas(bitmap))
                        saveDeviceScreenshot(context, "widget-${width}x$height-$theme", bitmap)
                        assertTrue("Missing primary quota: $visible", visible.contains("62%"))
                        if (width >= 280 || height >= 230) assertTrue("Missing secondary quota: $visible", visible.contains("36%"))
                    }
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
}
