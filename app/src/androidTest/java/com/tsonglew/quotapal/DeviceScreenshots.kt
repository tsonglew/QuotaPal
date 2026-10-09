package com.tsonglew.quotapal

import android.content.Context
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

internal fun saveDeviceScreenshot(context: Context, name: String, bitmap: Bitmap) {
    val folder = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
    val file = File(folder, "$name.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    // AGP uninstalls the tested app on completion, so preserve artifacts before teardown.
    val command = "mkdir -p /data/local/tmp/quotapal-screenshots && cp '${file.absolutePath}' /data/local/tmp/quotapal-screenshots/"
    InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use { descriptor ->
        java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
    }
}
