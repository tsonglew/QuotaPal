package com.tsonglew.quotapal

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import android.util.SizeF
import android.view.Gravity
import android.widget.FrameLayout
import kotlin.math.ceil
import com.tsonglew.quotapal.widget.QuotaWidgetReceiver

/** Debug-only native host for rendering the real RemoteViews in device tests. */
class WidgetTestHostActivity : Activity() {
    lateinit var widgetView: AppWidgetHostView
    private lateinit var host: AppWidgetHost
    var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val width = intent.getIntExtra("width", 280)
        val height = intent.getIntExtra("height", 150)
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
            putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
            if (android.os.Build.VERSION.SDK_INT >= 31)
                putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, arrayListOf(SizeF(width.toFloat(), height.toFloat())))
        }
        host = AppWidgetHost(this, 501)
        widgetId = host.allocateAppWidgetId()
        val manager = AppWidgetManager.getInstance(this)
        check(manager.bindAppWidgetIdIfAllowed(widgetId, ComponentName(this, QuotaWidgetReceiver::class.java), options))
        val info = manager.getAppWidgetInfo(widgetId)
        widgetView = host.createView(this, widgetId, info)
        val padding = AppWidgetHostView.getDefaultPaddingForWidget(this, info.provider, null)
        val density = resources.displayMetrics.density
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(218, 222, 230))
            addView(widgetView, FrameLayout.LayoutParams(ceil(width * density).toInt() + padding.left + padding.right,
                ceil(height * density).toInt() + padding.top + padding.bottom, Gravity.CENTER))
        }
        setContentView(root)
        host.startListening()
    }

    override fun onDestroy() {
        if (::host.isInitialized) { host.stopListening(); host.deleteHost() }
        super.onDestroy()
    }
}
