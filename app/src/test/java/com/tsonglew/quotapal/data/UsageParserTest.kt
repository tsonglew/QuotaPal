package com.tsonglew.quotapal.data

import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class UsageParserTest {
    private fun parse(body: String) = UsageParser.parse(body, "test-account", 1000)
    @Test fun singleWindowIsNotInventedIntoTwo() {
        val data = parse("""{"rate_limit":{"primary_window":{"used_percent":38,"limit_window_seconds":604800,"reset_at":2000}},"unknown":true}""")
        assertEquals(1, data.windows.size)
        assertEquals("每周额度", data.windows.single().name)
        assertEquals("62", data.windows.single().percentageLabel(true))
    }
    @Test fun weeklyWindowIsOrderedFirst() {
        val data = parse("""{"rate_limit":{"primary_window":{"used_percent":64,"limit_window_seconds":18000},"secondary_window":{"used_percent":38,"limit_window_seconds":604800}}}""")
        assertEquals("每周额度", data.windows.first().name)
        assertEquals("5 小时额度", data.windows.last().name)
    }
    @Test fun missingPercentageStaysUnknown() {
        assertNull(parse("""{"rate_limit":{"primary_window":{"limit_window_seconds":900}}}""").windows.single().usedPercent)
    }
    @Test fun zeroAndFullUsageArePreserved() {
        val data = parse("""{"rate_limit":{"primary_window":{"used_percent":0},"secondary_window":{"used_percent":100}}}""")
        assertEquals(listOf("100", "0"), data.windows.map { it.percentageLabel(true) })
    }
    @Test fun invalidPercentageIsNotClampedIntoCredibleValue() {
        val data = parse("""{"rate_limit":{"primary_window":{"used_percent":-1},"secondary_window":{"used_percent":101}}}""")
        assertTrue(data.windows.all { it.usedPercent == null })
    }
    @Test fun additionalWindowsRemainAvailable() {
        val data = parse("""{"rate_limit":{"primary_window":{"used_percent":10}},"additional_rate_limits":[{"metered_feature":"review","limit_name":"Review","rate_limit":{"primary_window":{"used_percent":20}}}]}""")
        assertEquals(2, data.windows.size)
        assertTrue(data.windows.last().id.startsWith("review:"))
    }
    @Test fun relativeResetUsesFetchTime() {
        val window = parse("""{"rate_limit":{"primary_window":{"used_percent":10,"reset_after_seconds":300}}}""").windows.single()
        assertEquals(1300L, window.resetsAt)
    }
    @Test fun elapsedResetDoesNotRestoreQuota() {
        val window = QuotaWindow("a", "每周额度", 100.0, 604800, 900)
        assertEquals("重置待确认 · 请刷新", resetLabel(window, 1000))
        assertEquals("0", window.percentageLabel(true))
    }
    @Test fun conflictingAccountIsRejected() {
        try { parse("""{"account_id":"other","rate_limit":{"primary_window":{"used_percent":10}}}"""); fail() }
        catch (error: ApiFailure) { assertEquals(FailureKind.FORBIDDEN, error.kind) }
    }
    @Test fun emptyOrMalformedResponseIsExplicitFailure() {
        for (body in listOf("{}", "null", "<html>")) {
            try { parse(body); fail() } catch (error: ApiFailure) { assertEquals(FailureKind.PROTOCOL, error.kind) }
        }
    }
    @Test fun absoluteTimeUsesCurrentTimezone() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC")); assertEquals("00:00", absoluteTime(0))
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai")); assertEquals("08:00", absoluteTime(0))
        } finally { TimeZone.setDefault(original) }
    }
    @Test fun credentialStringNeverContainsTokens() {
        val session = Session("sensitive-access", "sensitive-refresh", "account", 2000)
        assertFalse(session.toString().contains("sensitive"))
    }
}
