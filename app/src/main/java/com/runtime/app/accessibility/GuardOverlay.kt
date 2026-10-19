package com.runtime.app.accessibility

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import com.runtime.app.core.Box
import com.runtime.app.core.Element
import com.runtime.app.core.GestureTrace
import com.runtime.app.core.Point
import com.runtime.app.core.TouchDecision

/** Real separate windows leave the recognized comments panel natively touchable; no hidden APIs. */
class GuardOverlay(
    private val context: Context,
    private val manager: WindowManager,
    private val seekAt: (Point) -> Element?,
    private val onDecision: (TouchDecision) -> Unit,
    private val onSettings: () -> Unit,
    private val hapticsEnabled: () -> Boolean,
) {
    private val views = mutableListOf<View>()
    private var regions = emptyList<Box>()
    val active get() = views.isNotEmpty()

    // These are physical display coordinates, independent of text direction.
    @android.annotation.SuppressLint("RtlHardcoded")
    fun show(next: List<Box>): Boolean {
        if (next == regions && active) return true
        hide()
        if (next.isEmpty()) return false
        try {
            var badgePlaced = false
            for (bounds in next) {
                val showBadge = !badgePlaced && bounds.width >= dp(190) && bounds.height >= dp(60)
                val view = GuardView(context, bounds, showBadge)
                if (showBadge) badgePlaced = true
                val params = WindowManager.LayoutParams(
                    bounds.width, bounds.height, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP or Gravity.LEFT
                    x = bounds.left
                    y = bounds.top
                    title = "RunTime video guard"
                    setFitInsetsTypes(0)
                }
                manager.addView(view, params)
                views += view
            }
            regions = next
            return active
        } catch (_: RuntimeException) {
            hide()
            return false
        }
    }

    fun hide() {
        views.forEach { view ->
            try { manager.removeViewImmediate(view) } catch (_: IllegalArgumentException) { }
        }
        views.clear()
        regions = emptyList()
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    private inner class GuardView(context: Context, private val bounds: Box, private val badge: Boolean) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
        private var trace: GestureTrace? = null
        private val badgeBounds = Box(bounds.left + dp(12), bounds.top + dp(10), bounds.left + dp(184), bounds.top + dp(42))

        init {
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = true
            // Do not exclude system gestures: Back and Home must retain priority.
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (!badge) return
            val left = dp(12).toFloat()
            val top = dp(10).toFloat()
            paint.color = Color.rgb(20, 29, 20)
            canvas.drawRoundRect(left, top, dp(184).toFloat(), dp(42).toFloat(), dp(16).toFloat(), dp(16).toFloat(), paint)
            paint.color = Color.rgb(212, 255, 115)
            paint.textSize = dp(11).toFloat()
            paint.isFakeBoldText = true
            canvas.drawText("ONE VIDEO  ·  RunTime", dp(24).toFloat(), dp(30).toFloat(), paint)
        }

        @Suppress("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val point = Point(event.rawX, event.rawY)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> trace = GestureTrace(point, event.eventTime, seekAt(point))
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> {
                    for (i in 0 until event.historySize) {
                        trace?.observe(Point(event.getHistoricalX(0, i) + bounds.left, event.getHistoricalY(0, i) + bounds.top), event.pointerCount)
                    }
                    trace?.observe(point, event.pointerCount)
                }
                MotionEvent.ACTION_UP -> {
                    val gesture = trace
                    trace = null
                    val decision = gesture?.finish(point, event.eventTime, slop, ViewConfiguration.getLongPressTimeout().toLong())
                        ?: TouchDecision.Ignore
                    if (decision is TouchDecision.Tap && badge && badgeBounds.contains(decision.point)) {
                        onSettings()
                    } else {
                        if (decision == TouchDecision.Block && hapticsEnabled()) {
                            performHapticFeedback(HapticFeedbackConstants.REJECT)
                        }
                        onDecision(decision)
                    }
                }
                MotionEvent.ACTION_CANCEL -> trace = null
            }
            return true
        }
    }
}
