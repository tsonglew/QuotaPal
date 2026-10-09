package com.tsonglew.quotapal

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.tsonglew.quotapal.data.CodexApi
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Protocol
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.atomic.AtomicInteger

/** Device tests never send synthetic credentials to a real service. */
class QuotaTestRunner : AndroidJUnitRunner() {
    override fun newApplication(loader: ClassLoader, name: String, context: Context): Application =
        super.newApplication(loader, TestQuotaApplication::class.java.name, context)
}

class TestQuotaApplication : QuotaPalApplication() {
    val usageRequests = AtomicInteger()
    @Volatile var usedPercent = 38
    @Volatile var responseDelayMillis = 0L
    @Volatile var usageStatusCode = 200
    @Volatile var usageRetryAfter: String? = null
    @Volatile var usageTransportFailure: java.io.IOException? = null
    override val api by lazy {
        CodexApi(OkHttpClient.Builder().addInterceptor { chain ->
            if (chain.request().url.encodedPath != "/backend-api/wham/usage") {
                throw java.io.IOException("No external network in device tests")
            }
            usageRequests.incrementAndGet()
            Thread.sleep(responseDelayMillis)
            usageTransportFailure?.let { throw it }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(usageStatusCode).message("Test response")
                .apply { usageRetryAfter?.let { header("Retry-After", it) } }
                .body("""{"rate_limit":{"primary_window":{"used_percent":$usedPercent,"limit_window_seconds":604800}}}"""
                    .toResponseBody("application/json".toMediaType())).build()
        }.build())
    }
}
