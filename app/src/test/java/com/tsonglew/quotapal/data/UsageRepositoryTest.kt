package com.tsonglew.quotapal.data

import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.Base64

class UsageRepositoryTest {
    private val server = MockWebServer()
    private val vault = FakeVault()
    private val dao = FakeDao()
    private val settings = FakeSettings()
    private lateinit var repository: UsageRepository
    @Before fun start() {
        server.start()
        vault.session = Session("access", "refresh", "test-account", 9000)
        repository = UsageRepository(CodexApi(authBase = server.url("/").toString().trimEnd('/'), usageUrl = server.url("/usage").toString(), now = { 1000 }), vault, dao, settings, now = { 1000 })
    }
    @After fun stop() { server.shutdown() }
    @Test fun concurrentRefreshesShareOneRequest() = runBlocking {
        server.enqueue(MockResponse().setBody(body).setBodyDelay(100, TimeUnit.MILLISECONDS))
        val outcomes = coroutineScope { listOf(async { repository.refresh(true) }, async { repository.refresh(true) }).awaitAll() }
        assertEquals(listOf(SyncResult.SUCCESS, SyncResult.SUCCESS), outcomes)
        assertEquals(1, server.requestCount)
        assertEquals("88", repository.state.value.snapshot!!.windows.single().percentageLabel(true))
    }
    @Test fun failedRequestPreservesSnapshotAndSuccessfulTimestamp() = runBlocking {
        dao.record = SnapshotRecord(accountId = "test-account", json = AppJson.encodeToString(demoSnapshot(500).copy(accountId = "test-account")))
        server.enqueue(MockResponse().setResponseCode(503))
        repository.refresh(true)
        assertEquals(500L, repository.state.value.snapshot!!.fetchedAt)
        assertEquals(FailureKind.SERVER, repository.state.value.failure)
        assertEquals(1000L, settings.value.lastAttemptAt)
    }
    @Test fun validWindowlessResponseReplacesOldMeteredCache() = runBlocking {
        dao.record = SnapshotRecord(accountId = "test-account", json = AppJson.encodeToString(demoSnapshot(500).copy(accountId = "test-account")))
        server.enqueue(MockResponse().setBody("""{"plan_type":"pro","rate_limit":{"allowed":true,"limit_reached":false}}"""))
        assertEquals(SyncResult.SUCCESS, repository.refresh(true))
        val snapshot = repository.state.value.snapshot!!
        assertTrue(snapshot.windows.isEmpty())
        assertNull(repository.state.value.failure)
        assertEquals("当前可用", snapshot.quotaNotice()!!.title)
        assertEquals(snapshot, AppJson.decodeFromString<UsageSnapshot>(dao.record!!.json))
    }
    @Test fun logoutRejectsAnInflightResponse() = runBlocking {
        server.enqueue(MockResponse().setBody(body).setBodyDelay(200, TimeUnit.MILLISECONDS))
        coroutineScope {
            val refresh = async { repository.refresh(true) }
            withContext(Dispatchers.IO) { server.takeRequest(5, TimeUnit.SECONDS) }
            repository.logout()
            refresh.await()
        }
        assertNull(repository.state.value.snapshot)
        assertFalse(repository.state.value.connected)
        assertNull(vault.session); assertNull(dao.record)
    }
    @Test fun cachedDataFromAnotherAccountIsNeverDisplayed() = runBlocking {
        dao.record = SnapshotRecord(accountId = "other", json = AppJson.encodeToString(demoSnapshot(500)))
        repository.initialize()
        assertNull(repository.state.value.snapshot)
    }
    @Test fun serverBackoffSurvivesRepositoryInitialization() = runBlocking {
        settings.value = settings.value.copy(retryAt = 1200, lastFailure = FailureKind.LIMITED)
        assertEquals(SyncResult.DEFERRED, repository.refresh(true))
        assertEquals(0, server.requestCount)
    }
    @Test fun concurrentExpiredSessionsRenewOnlyOnceAndSaveRotation() = runBlocking {
        vault.session = vault.session!!.copy(expiresAt = 900)
        server.enqueue(MockResponse().setBody(renewed).setBodyDelay(100, TimeUnit.MILLISECONDS))
        server.enqueue(MockResponse().setBody(body))
        coroutineScope { listOf(async { repository.refresh(true) }, async { repository.refresh(true) }).awaitAll() }
        assertEquals(2, server.requestCount)
        assertEquals("/oauth/token", server.takeRequest().path)
        assertEquals("/usage", server.takeRequest().path)
        assertEquals("rotated-refresh", vault.session!!.refreshToken)
        assertEquals(9000L, vault.session!!.expiresAt)
        assertNotNull(repository.state.value.snapshot)
    }
    @Test fun unauthorizedUsageRenewsAndRetriesExactlyOnce() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setBody(renewed))
        server.enqueue(MockResponse().setResponseCode(401))
        assertEquals(SyncResult.AUTH_REQUIRED, repository.refresh(true))
        assertEquals(3, server.requestCount)
        assertEquals(FailureKind.AUTH, repository.state.value.failure)
        assertEquals("rotated-refresh", vault.session!!.refreshToken)
        assertNull(repository.state.value.snapshot)
    }
    private class FakeVault : CredentialStore {
        var session: Session? = null
        override fun read() = session
        override fun write(session: Session) { this.session = session }
        override fun clear() { session = null }
    }
    private class FakeDao : SnapshotDao {
        var record: SnapshotRecord? = null
        override suspend fun read() = record
        override suspend fun write(record: SnapshotRecord) { this.record = record }
        override suspend fun clear() { record = null }
    }
    private class FakeSettings : SyncSettings {
        var value = PreferencesState()
        override suspend fun read() = value
        override suspend fun demo(value: Boolean) { this.value = this.value.copy(demo = value) }
        override suspend fun syncResult(kind: FailureKind?, retryAt: Long, lastAttemptAt: Long?) {
            value = value.copy(lastFailure = kind, retryAt = retryAt, lastAttemptAt = lastAttemptAt ?: value.lastAttemptAt)
        }
    }
    companion object {
        const val body = """{"account_id":"test-account","rate_limit":{"primary_window":{"used_percent":12,"limit_window_seconds":604800}}}"""
        private val claims = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"https://api.openai.com/auth":{"chatgpt_account_id":"test-account"},"exp":9000}""".toByteArray())
        val renewed = """{"access_token":"header.$claims.signature","refresh_token":"rotated-refresh"}"""
    }
}
