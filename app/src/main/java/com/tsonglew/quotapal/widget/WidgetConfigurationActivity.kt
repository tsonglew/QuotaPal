package com.tsonglew.quotapal.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.tsonglew.quotapal.quotaApp
import com.tsonglew.quotapal.ui.QuotaTheme
import kotlinx.coroutines.launch

class WidgetConfigurationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(RESULT_CANCELED)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        lifecycleScope.launch {
            val config = quotaApp.settings.widgetSettings(widgetId)
            setContent {
                var remaining by remember { mutableStateOf(config.first) }
                var theme by remember { mutableStateOf(config.second) }
                var saving by remember { mutableStateOf(false) }
                QuotaTheme(theme) {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Spacer(Modifier.height(32.dp)); Text("桌面上的 Codex", style = MaterialTheme.typography.headlineMedium)
                            Text("这份设置只影响当前小组件。")
                            Row { Text("显示剩余额度", Modifier.weight(1f)); Switch(remaining, { remaining = it }) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("system" to "系统", "light" to "浅色", "dark" to "深色").forEach { (value, label) ->
                                    FilterChip(theme == value, { theme = value }, label = { Text(label) })
                                }
                            }
                            Text("可在桌面长按并缩放。后台更新可能延迟，最后更新时间始终可见。", style = MaterialTheme.typography.bodyMedium)
                            Button(enabled = !saving, onClick = {
                                saving = true
                                lifecycleScope.launch {
                                    quotaApp.settings.saveWidget(widgetId, remaining, theme)
                                    quotaApp.updateWidgets()
                                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                                    finish()
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("保存小组件") }
                            TextButton(onClick = { finish() }) { Text("取消") }
                        }
                    }
                }
            }
        }
    }
}
