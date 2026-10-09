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

class UsageRepositoryTest {
    private val server = MockWebServer()
    private val vault = FakeVault()
    private val dao = FakeDao()
    private val settings = FakeSettings()
    private lateinit var repository: UsageRepository
    @Before fun start() {
        server.start()
        vault.session = Session("access", "refresh", "test-account", 9000)
        repository = UsageRepository(CodexApi(usageUrl = server.url("/usage").toString(), now = { 1000 }), vault, dao, settings, now = { 1000 })
    }
    @After fun stop() { server.shutdown() }
    @Test fun concurrentRefreshesShareOneRequest() = runBlocking {
        server.enqueue(MockResponse().setBody(body).setBodyDelay(100, TimeUnit.MILLISECONDS))
        coroutineScope { listOf(async { repository.refresh(true) }, async { repository.refresh(true) }).awaitAll() }
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
    companion object { const val body = """{"account_id":"test-account","rate_limit":{"primary_window":{"used_percent":12,"limit_window_seconds":604800}}}""" }
}
