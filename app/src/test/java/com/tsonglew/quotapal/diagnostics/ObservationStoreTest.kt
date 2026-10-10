package com.tsonglew.quotapal.diagnostics

import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

class ObservationStoreTest {
    @get:Rule val temp = TemporaryFolder()
    private fun store() = ObservationStore(File(temp.root, "observations.csv"))

    @Test fun countsRealRequestsSeparatelyFromRefreshesAndPollingResponses() {
        val s = store()
        s.record(DiagnosticEvent.REFRESH_START, longArrayOf(1), 3600)
        s.record(DiagnosticEvent.SKIP_FRESH, longArrayOf(), 3600)
        s.record(DiagnosticEvent.HTTP_START, longArrayOf(0), 3600)
        s.record(DiagnosticEvent.HTTP_START, longArrayOf(1), 3600)
        s.record(DiagnosticEvent.HTTP_END, longArrayOf(1, 403), 3600)
        s.record(DiagnosticEvent.HTTP_NETWORK_FAILURE, longArrayOf(0, 0), 3600)
        assertTrue(s.report(3600).contains("1970-01-01T01:00:00Z 1 1 1 1 0 0 0"))
    }

    @Test fun recordsSuccessGapAcrossHoursAndProcessRestart() {
        store().record(DiagnosticEvent.SNAPSHOT_SAVED, longArrayOf(), 3600)
        store().record(DiagnosticEvent.SNAPSHOT_SAVED, longArrayOf(), 7200)
        assertTrue(store().report(7200).contains("1970-01-01T02:00:00Z 0 0 0 0 1 7200 3600"))
    }

    @Test fun boundsHistoryAndExpiresOldRowsWithoutNewEvents() {
        repeat(200) { store().record(DiagnosticEvent.HTTP_START, longArrayOf(0), (it + 1) * 3600L) }
        val report = store().report(200 * 3600L)
        assertEquals(168, report.lines().count { it.startsWith("1970-") })
        assertFalse(store().report(400 * 3600L).contains("1970-"))
    }

    @Test fun ignoresCorruptRowsAndFutureHistoryAfterClockRollback() {
        File(temp.root, "observations.csv").writeText("broken\n1,1,0,0,0,0,0,-1\n")
        store().record(DiagnosticEvent.SNAPSHOT_SAVED, longArrayOf(), 7200)
        store().record(DiagnosticEvent.SNAPSHOT_SAVED, longArrayOf(), 3600)
        assertFalse(store().report(3600).contains("02:00:00"))
        assertTrue(store().report(3600).contains(" 1 3600 0"))
    }

    @Test fun clearRemovesEventsAndStatisticsTogether() {
        val diagnostics = Diagnostics(File(temp.root, "events.log"))
        diagnostics.record(DiagnosticEvent.HTTP_START, 0)
        assertTrue(File(temp.root, "observations.csv").exists())
        diagnostics.clear()
        assertFalse(File(temp.root, "observations.csv").exists())
        assertFalse(File(temp.root, "events.log").exists())
    }
}
