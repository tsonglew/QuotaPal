package com.tsonglew.quotapal

import androidx.glance.GlanceId
import androidx.glance.action.actionParametersOf
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.tsonglew.quotapal.widget.RefreshWidgetAction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class WidgetRefreshActionDeviceTest {
    @Test fun realRemoteViewsClickFetchesOnceForThreeWidgets() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.quotaApp as TestQuotaApplication
        val manager = WorkManager.getInstance(context)
        instrumentation.uiAutomation.adoptShellPermissionIdentity(android.Manifest.permission.BIND_APPWIDGET)
        try {
            runBlocking {
                app.repository.logout()
                app.usedPercent = 38
                app.repository.connect(com.tsonglew.quotapal.data.Session("test-access", "test-refresh", "test-account", Long.MAX_VALUE))
            }
            androidx.test.core.app.ActivityScenario.launch<WidgetTestHostActivity>(
                android.content.Intent(context, WidgetTestHostActivity::class.java).putExtra("slim", true)
                    .putExtra("width", 280).putExtra("height", 70)).use { scenario ->
                scenario.onActivity { it.addStandardWidget(); it.addThirdWidget() }
                runBlocking { app.updateWidgets(); app.reconcileSync() }
                fun labels(view: android.view.View): List<android.widget.TextView> = when (view) {
                    is android.widget.TextView -> listOf(view)
                    is android.view.ViewGroup -> (0 until view.childCount).flatMap { labels(view.getChildAt(it)) }
                    else -> emptyList()
                }
                var touchDescription = "not tapped"
                fun awaitBoth(expected: String, timeoutMillis: Long = 20_000) {
                    val deadline = android.os.SystemClock.elapsedRealtime() + timeoutMillis
                    var matched = false
                    while (!matched && android.os.SystemClock.elapsedRealtime() < deadline) {
                        scenario.onActivity { activity ->
                            matched = listOf(activity.widgetView, requireNotNull(activity.secondaryWidgetView), requireNotNull(activity.thirdWidgetView)).all {
                                labels(it).any { label -> label.text.contains(expected) }
                            }
                        }
                        if (!matched) Thread.sleep(50)
                    }
                    var actual = ""
                    scenario.onActivity { activity ->
                        actual = listOf(activity.widgetView, requireNotNull(activity.secondaryWidgetView), requireNotNull(activity.thirdWidgetView))
                            .joinToString(" | ") { labels(it).joinToString { label -> label.text.toString() } }
                    }
                    org.junit.Assert.assertTrue("All three widgets must show $expected; actual=$actual; touch=$touchDescription; requests=${app.usageRequests.get()}; state=${app.repository.state.value}; diagnostics=${app.diagnostics.report()}", matched)
                }
                awaitBoth("62%")
                val before = app.usageRequests.get()
                app.usedPercent = 17
                val responseGate = java.util.concurrent.CountDownLatch(1)
                app.usageResponseGate = responseGate
                runBlocking { app.settings.syncResult(null, lastAttemptAt = 0) }
                // RemoteViews applies asynchronously and semantics may belong to a
                // wrapper rather than its TextView. Wait for the actual action view.
                fun descendants(view: android.view.View): List<android.view.View> =
                    listOf(view) + if (view is android.view.ViewGroup) {
                        (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) }
                    } else emptyList()
                val clickDeadline = android.os.SystemClock.elapsedRealtime() + 20_000
                var refreshBounds: android.graphics.Rect? = null
                var lastTree = ""
                while (refreshBounds == null && android.os.SystemClock.elapsedRealtime() < clickDeadline) {
                    scenario.onActivity { activity ->
                        val views = descendants(activity.widgetView)
                        lastTree = views.joinToString("\n") {
                            "${it.javaClass.simpleName}: description=${it.contentDescription}, clickable=${it.hasOnClickListeners()}"
                        }
                        val actions = views.filter { it.contentDescription?.toString() == "刷新额度" }
                        org.junit.Assert.assertTrue("Refresh semantics must be unique\n$lastTree", actions.size <= 1)
                        actions.singleOrNull()?.let { action ->
                            val bounds = android.graphics.Rect()
                            if (activity.hasWindowFocus() && action.isShown && !action.isLayoutRequested &&
                                action.getLocalVisibleRect(bounds) && !bounds.isEmpty) {
                                // Input injection takes screen coordinates. GlobalVisibleRect
                                // is relative to the root View and can omit the window offset.
                                val screen = IntArray(2)
                                action.getLocationOnScreen(screen)
                                bounds.offset(screen[0], screen[1])
                                refreshBounds = bounds
                                touchDescription = "screenBounds=$bounds; action=${action.javaClass.simpleName}; tree=$lastTree"
                            }
                        }
                    }
                    if (refreshBounds == null) Thread.sleep(50)
                }
                org.junit.Assert.assertNotNull("Refresh action must become visible\n$lastTree", refreshBounds)
                val bounds = requireNotNull(refreshBounds)
                fun tapRefresh() {
                    val downTime = android.os.SystemClock.uptimeMillis()
                    for (eventAction in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
                        val event = android.view.MotionEvent.obtain(downTime, android.os.SystemClock.uptimeMillis(),
                            eventAction, bounds.exactCenterX(), bounds.exactCenterY(), 0)
                        event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
                        try {
                            org.junit.Assert.assertTrue("Real refresh touch must be delivered",
                                instrumentation.uiAutomation.injectInputEvent(event, true))
                        } finally { event.recycle() }
                    }
                }
                // A real touch targets Glance's action even when its semantic wrapper
                // and PendingIntent live on different nodes. Ancestor performClick can
                // accidentally invoke the widget's open-App action instead.
                tapRefresh()
                tapRefresh()
                try {
                    // Observe actual progress before permitting HTTP completion, within
                    // the production action's eight-second immediate-request budget.
                    awaitBoth("…", 6_000)
                    assertEquals("Progress shares one pending request", before + 1, app.usageRequests.get())
                } finally {
                    responseGate.countDown()
                    app.usageResponseGate = null
                }
                awaitBoth("83%")
                assertEquals("Repeated clicks share one request", before + 1, app.usageRequests.get())
                assertEquals(1, manager.getWorkInfosForUniqueWork("codex-periodic-sync").get(10, TimeUnit.SECONDS)
                    .count { !it.state.isFinished })
            }
        } finally {
            app.usageResponseGate?.countDown()
            app.usageResponseGate = null
            app.responseDelayMillis = 0
            runBlocking { app.repository.logout(); app.reconcileSync() }
            instrumentation.uiAutomation.dropShellPermissionIdentity()
        }
    }

    @Test fun callbackCompletesImmediatelyWithoutOrdinaryBackgroundJob() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.quotaApp
        val manager = WorkManager.getInstance(context)
        manager.cancelUniqueWork("codex-manual-sync").result.get(10, TimeUnit.SECONDS)
        try {
            app.repository.demo()
            val before = manager.getWorkInfosForUniqueWork("codex-manual-sync").get(10, TimeUnit.SECONDS).map { it.id }.toSet()
            RefreshWidgetAction().onAction(context, object : GlanceId {}, actionParametersOf())
            val after = manager.getWorkInfosForUniqueWork("codex-manual-sync").get(10, TimeUnit.SECONDS).map { it.id }.toSet()
            assertEquals("A completed widget action must not wait for scheduled background execution", before, after)
        } finally {
            app.repository.logout()
        }
    }
}
