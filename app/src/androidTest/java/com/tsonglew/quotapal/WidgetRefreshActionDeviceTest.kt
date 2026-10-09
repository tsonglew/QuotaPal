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
