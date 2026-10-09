package com.tsonglew.quotapal.widget

import com.tsonglew.quotapal.data.SyncResult
import kotlinx.coroutines.withTimeoutOrNull

/** A user click gets an immediate attempt instead of waiting for an ordinary scheduled job. */
internal suspend fun refreshFromWidget(
    refresh: suspend () -> SyncResult,
    enqueue: suspend () -> Unit,
    update: suspend () -> Unit,
    timeoutMillis: Long = 8_000,
) {
    val result = withTimeoutOrNull(timeoutMillis) { refresh() }
    if (result == null || result == SyncResult.RETRY || result == SyncResult.DEFERRED) enqueue()
    update()
}
