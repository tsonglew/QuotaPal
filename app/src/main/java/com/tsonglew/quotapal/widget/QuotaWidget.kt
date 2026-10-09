package com.tsonglew.quotapal.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.tsonglew.quotapal.MainActivity
import com.tsonglew.quotapal.data.*
import com.tsonglew.quotapal.quotaApp
import com.tsonglew.quotapal.sync.SyncScheduler
import kotlinx.coroutines.launch

class QuotaWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(140.dp, 150.dp), DpSize(140.dp, 230.dp), DpSize(280.dp, 150.dp)))
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.quotaApp
        app.repository.initialize()
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val preferences = app.settings.widgetSettingsFlow(widgetId)
        val initialPreferences = app.settings.widgetSettings(widgetId)
        provideContent {
            val state by app.repository.state.collectAsState()
            val config by preferences.collectAsState(initialPreferences)
            Content(state, config.first, config.second, context)
        }
    }

    @Composable
    private fun Content(state: AppState, remaining: Boolean, theme: String, context: Context) {
        val size = LocalSize.current
        val systemDark = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val dark = theme == "dark" || theme == "system" && systemDark
        val background = ColorProvider(if (dark) Color(0xFF111215) else Color(0xFFF9FAFC))
        val foreground = ColorProvider(if (dark) Color(0xFFF2F3F5) else Color(0xFF17191D))
        val muted = ColorProvider(if (dark) Color(0xFFA4AAB5) else Color(0xFF747C88))
        val track = ColorProvider(if (dark) Color(0xFF282C33) else Color(0xFFE6E9EF))
        val notice = state.snapshot?.quotaNotice()
        Column(GlanceModifier.fillMaxSize().appWidgetBackground().background(background).cornerRadius(28.dp)
            .padding(horizontal = 12.dp, vertical = if (notice != null) 8.dp else 12.dp)
            .clickable(actionStartActivity<MainActivity>())) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Codex", style = TextStyle(color = foreground, fontSize = 15.sp, fontWeight = FontWeight.Medium), modifier = GlanceModifier.defaultWeight())
                Text("↻", style = TextStyle(color = muted, fontSize = 20.sp), modifier = GlanceModifier.padding(horizontal = 8.dp)
                    .clickable(actionRunCallback<RefreshWidgetAction>()))
            }
            Spacer(GlanceModifier.height(8.dp))
            if (state.snapshot == null) {
                Text("连接你的 Codex", style = TextStyle(color = foreground, fontSize = 16.sp, fontWeight = FontWeight.Medium))
                Spacer(GlanceModifier.height(8.dp))
                Text(state.failure?.userMessage() ?: "轻点打开 QuotaPal", style = TextStyle(color = muted, fontSize = 11.sp))
            } else {
                val windows = state.snapshot.windows.take(if (size.width >= 280.dp || size.height >= 230.dp) 2 else 1)
                if (windows.isEmpty()) {
                    Text(notice?.title ?: "未提供周期额度", style = TextStyle(color = foreground, fontSize = 16.sp, fontWeight = FontWeight.Medium), maxLines = 2)
                    Spacer(GlanceModifier.height(6.dp))
                    Text(notice?.description ?: "请以账号实际权益为准。", style = TextStyle(color = muted, fontSize = 10.sp), maxLines = 3)
                } else if (size.width >= 280.dp && windows.size > 1) {
                    Row(GlanceModifier.fillMaxWidth()) {
                        windows.forEachIndexed { index, window ->
                            if (index > 0) Spacer(GlanceModifier.width(18.dp))
                            Column(GlanceModifier.defaultWeight()) { Window(window, remaining, foreground, muted, track, notice?.widgetLabel) }
                        }
                    }
                } else windows.forEachIndexed { index, window ->
                    if (index > 0) Spacer(GlanceModifier.height(12.dp))
                    Window(window, remaining, foreground, muted, track, notice?.widgetLabel)
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(if (state.demo) "示例数据 · ${absoluteTime(state.snapshot.fetchedAt)}" else
                    "更新于 ${absoluteTime(state.snapshot.fetchedAt, true)}${if (state.failure != null) " · 待刷新" else ""}",
                    style = TextStyle(color = muted, fontSize = 10.sp), maxLines = 2)
            }
        }
    }

    @Composable
    private fun Window(window: QuotaWindow, remaining: Boolean, foreground: ColorProvider, muted: ColorProvider, track: ColorProvider, note: String?) {
        Column {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(window.name, style = TextStyle(color = foreground, fontSize = 11.sp), modifier = GlanceModifier.defaultWeight(), maxLines = 2)
            Text("${window.percentageLabel(remaining)}${if (window.usedPercent != null) "%" else ""}",
                style = TextStyle(color = foreground, fontSize = if (window.usedPercent == null) 13.sp else 25.sp, fontWeight = FontWeight.Medium))
        }
        Spacer(GlanceModifier.height(4.dp))
        if (window.usedPercent != null) {
        Row(GlanceModifier.fillMaxWidth()) {
            // Glance containers support at most ten direct children.
            repeat(4) { group ->
                Row(GlanceModifier.defaultWeight()) {
                    repeat(7) { offset ->
                        val index = group * 7 + offset
                        Box(GlanceModifier.defaultWeight().height(10.dp).padding(horizontal = 1.dp)) {
                            Box(GlanceModifier.fillMaxSize().background(
                                if (window.displayedPercent(remaining)?.let { index < it / 100 * 28 } == true) ColorProvider(Color(0xFF4796EF)) else track)) {}
                        }
                    }
                }
            }
        }
        } else Text("用量数据未返回", style = TextStyle(color = muted, fontSize = 10.sp))
        Spacer(GlanceModifier.height(4.dp))
        Text("${note ?: if (remaining) "剩余" else "已用"} · ${resetLabel(window)}", style = TextStyle(color = muted, fontSize = 9.sp), maxLines = 2)
        }
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        SyncScheduler.refresh(context)
    }
}

class QuotaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QuotaWidget()
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.quotaApp.scope.launch { context.quotaApp.reconcileSync() }
    }
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        context.quotaApp.scope.launch {
            appWidgetIds.forEach { context.quotaApp.settings.deleteWidget(it) }
            context.quotaApp.reconcileSync()
        }
    }
}
