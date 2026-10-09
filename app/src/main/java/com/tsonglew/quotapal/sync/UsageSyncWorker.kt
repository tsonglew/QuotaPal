package com.tsonglew.quotapal.sync

import android.content.Context
import androidx.work.*
import com.tsonglew.quotapal.data.SyncResult
import com.tsonglew.quotapal.quotaApp
import java.util.concurrent.TimeUnit

class UsageSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext.quotaApp
        val outcome = app.repository.refresh(inputData.getBoolean("force", false))
        app.updateWidgets()
        return if (outcome == SyncResult.RETRY || outcome == SyncResult.DEFERRED && inputData.getBoolean("force", false)) Result.retry() else Result.success()
    }
}

object SyncScheduler {
    private const val PERIODIC = "codex-periodic-sync"
    private const val MANUAL = "codex-manual-sync"
    fun schedule(context: Context, minutes: Long): Operation {
        val request = PeriodicWorkRequestBuilder<UsageSyncWorker>(minutes, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        return WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
    fun refresh(context: Context): Operation {
        val request = OneTimeWorkRequestBuilder<UsageSyncWorker>().setInputData(workDataOf("force" to true))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .apply {
                // Older systems require a foreground notification for expedited work.
                if (android.os.Build.VERSION.SDK_INT >= 31)
                    setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }.build()
        // A new user attempt must not be hidden behind a previous retry's backoff.
        return WorkManager.getInstance(context).enqueueUniqueWork(MANUAL, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        WorkManager.getInstance(context).cancelUniqueWork(MANUAL)
    }
}
