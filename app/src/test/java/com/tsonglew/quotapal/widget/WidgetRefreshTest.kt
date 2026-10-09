package com.tsonglew.quotapal.widget

import com.tsonglew.quotapal.data.SyncResult
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class WidgetRefreshTest {
    @Test fun successfulClickRefreshesBeforeUpdatingWithoutScheduledWork() = runTest {
        val events = mutableListOf<String>()
        refreshFromWidget({ events += "request"; SyncResult.SUCCESS }, { events += "enqueue" }, { events += "update" })
        assertEquals(listOf("request", "update"), events)
    }
    @Test fun timedOutRequestIsCancelledAndHandedOffBeforeUpdating() = runTest {
        val events = mutableListOf<String>()
        refreshFromWidget({ try { delay(20_000); SyncResult.SUCCESS } finally { events += "cancel" } },
            { events += "enqueue" }, { events += "update" })
        assertEquals(listOf("cancel", "enqueue", "update"), events)
    }
    @Test fun transientFailuresAndServerWaitCanContinueButAuthDoesNotLoop() = runTest {
        for (result in SyncResult.entries) {
            var enqueued = false
            var updated = false
            refreshFromWidget({ result }, { enqueued = true }, { updated = true })
            assertEquals(result in listOf(SyncResult.RETRY, SyncResult.DEFERRED), enqueued)
            assertTrue(updated)
        }
    }
    @Test fun externalCancellationDoesNotStartAnotherJob() = runTest {
        var enqueued = false
        val job = launch { refreshFromWidget({ awaitCancellation() }, { enqueued = true }, {}) }
        yield(); job.cancelAndJoin()
        assertFalse(enqueued)
    }
}
