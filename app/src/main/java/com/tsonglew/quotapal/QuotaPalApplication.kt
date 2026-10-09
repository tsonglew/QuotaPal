package com.tsonglew.quotapal

import android.app.Application
import android.content.Context
import android.content.ComponentName
import android.appwidget.AppWidgetManager
import androidx.room.Room
import com.tsonglew.quotapal.data.*
import com.tsonglew.quotapal.sync.SyncScheduler
import com.tsonglew.quotapal.widget.QuotaWidget
import com.tsonglew.quotapal.widget.QuotaWidgetReceiver
import com.tsonglew.quotapal.widget.SlimQuotaWidget
import com.tsonglew.quotapal.widget.SlimQuotaWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

open class QuotaPalApplication : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableWallClockRevision = MutableStateFlow(0L)
    val wallClockRevision = mutableWallClockRevision.asStateFlow()
    fun wallClockChanged() { mutableWallClockRevision.update { it + 1 } }
    open val api by lazy { CodexApi() }
    val settings by lazy { SettingsStore(this) }
    val repository by lazy {
        UsageRepository(api, CredentialVault(this), Room.databaseBuilder(this, QuotaDatabase::class.java, "quota.db").build().snapshots(), settings)
    }
    override fun onCreate() {
        super.onCreate()
        scope.launch { repository.initialize() }
    }
    suspend fun updateWidgets() { QuotaWidget().updateAll(this); SlimQuotaWidget().updateAll(this) }
    suspend fun reconcileSync() {
        repository.initialize()
        val state = repository.state.value
        val manager = AppWidgetManager.getInstance(this)
        val hasWidget = listOf(QuotaWidgetReceiver::class.java, SlimQuotaWidgetReceiver::class.java).any {
            manager.getAppWidgetIds(ComponentName(this, it)).isNotEmpty()
        }
        if (state.connected && !state.demo && hasWidget) SyncScheduler.schedule(this, settings.read().refreshMinutes)
        else SyncScheduler.cancel(this)
    }
}

val Context.quotaApp: QuotaPalApplication get() = applicationContext as QuotaPalApplication
