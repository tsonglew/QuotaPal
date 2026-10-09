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
import java.util.concurrent.atomic.AtomicInteger
import com.tsonglew.quotapal.widget.QuotaWidgetReceiver
import com.tsonglew.quotapal.widget.SlimQuotaWidgetReceiver

/** Debug-only native host for rendering the real RemoteViews in device tests. */
class WidgetTestHostActivity : Activity() {
    companion object { private val hostIds = AtomicInteger(501) }
    lateinit var widgetView: AppWidgetHostView
    var secondaryWidgetView: AppWidgetHostView? = null
        private set
    var thirdWidgetView: AppWidgetHostView? = null
        private set
    private var secondaryWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var host: AppWidgetHost
    private lateinit var root: FrameLayout
    var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val width = intent.getIntExtra("width", 280)
        val height = intent.getIntExtra("height", 150)
        host = AppWidgetHost(this, hostIds.incrementAndGet())
        val (id, view) = bindWidget(width, height, intent.getBooleanExtra("slim", false))
        widgetId = id
        widgetView = view
        root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(218, 222, 230))
            addView(view, widgetLayout(view, width, height, Gravity.CENTER))
        }
        setContentView(root)
        host.startListening()
    }

    private fun bindWidget(width: Int, height: Int, slim: Boolean): Pair<Int, AppWidgetHostView> {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
            putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
            if (android.os.Build.VERSION.SDK_INT >= 31)
                putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, arrayListOf(SizeF(width.toFloat(), height.toFloat())))
        }
        val id = host.allocateAppWidgetId()
        val manager = AppWidgetManager.getInstance(this)
        val provider = if (slim) SlimQuotaWidgetReceiver::class.java else QuotaWidgetReceiver::class.java
        check(manager.bindAppWidgetIdIfAllowed(id, ComponentName(this, provider), options))
        return id to host.createView(this, id, manager.getAppWidgetInfo(id))
    }

    private fun widgetLayout(view: AppWidgetHostView, width: Int, height: Int, gravity: Int): FrameLayout.LayoutParams {
        val info = view.appWidgetInfo
        val padding = AppWidgetHostView.getDefaultPaddingForWidget(this, info.provider, null)
        val density = resources.displayMetrics.density
        return FrameLayout.LayoutParams(ceil(width * density).toInt() + padding.left + padding.right,
            ceil(height * density).toInt() + padding.top + padding.bottom, gravity)
    }

    fun resizePrimaryWidget(width: Int, height: Int) {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            widgetView.updateAppWidgetSize(Bundle(), listOf(SizeF(width.toFloat(), height.toFloat())))
        } else {
            @Suppress("DEPRECATION")
            widgetView.updateAppWidgetSize(Bundle(), width, height, width, height)
        }
        widgetView.layoutParams = widgetLayout(widgetView, width, height, Gravity.CENTER)
    }

    fun addStandardWidget(): Int {
        check(secondaryWidgetView == null)
        val (id, view) = bindWidget(140, 150, false)
        secondaryWidgetId = id
        secondaryWidgetView = view
        root.addView(view, widgetLayout(view, 140, 150, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        return id
    }

    fun addThirdWidget(): Int {
        check(thirdWidgetView == null)
        val (id, view) = bindWidget(280, 70, true)
        thirdWidgetView = view
        root.addView(view, widgetLayout(view, 280, 70, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
        return id
    }

    fun removeStandardWidget() {
        secondaryWidgetView?.let(root::removeView)
        host.deleteAppWidgetId(secondaryWidgetId)
        secondaryWidgetView = null
        secondaryWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    override fun onDestroy() {
        if (::host.isInitialized) { host.stopListening(); host.deleteHost() }
        super.onDestroy()
    }
}
