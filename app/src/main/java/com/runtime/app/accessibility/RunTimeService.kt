package com.runtime.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Path
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.Display
import android.view.WindowManager
import android.view.WindowInsets
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.runtime.app.MainActivity
import com.runtime.app.core.Box
import com.runtime.app.core.Detection
import com.runtime.app.core.Element
import com.runtime.app.core.Point
import com.runtime.app.core.Role
import com.runtime.app.core.ScreenKind
import com.runtime.app.core.ScreenSnapshot
import com.runtime.app.core.TouchDecision
import com.runtime.app.core.YouTubeShortsDetector
import com.runtime.app.data.RunTimeSettings
import com.runtime.app.data.RunTimeState

class RunTimeService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val detector = YouTubeShortsDetector()
    private lateinit var settings: RunTimeSettings
    private lateinit var guard: GuardOverlay
    private lateinit var overlayManager: WindowManager
    private var snapshot: ScreenSnapshot? = null
    private var detection: Detection? = null
    private var connected = false
    private var tapInFlight = false
    private var injectionGeneration = 0

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        cancelInjection()
        evaluate()
    }
    private val evaluateTask = Runnable { evaluate() }
    private val watchdog = object : Runnable {
        override fun run() {
            if (!connected) return
            evaluate()
            handler.postDelayed(this, 750)
        }
    }
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                cancelInjection()
                release("Phone locked", "Guard released")
            } else scheduleEvaluation()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        settings = RunTimeSettings(this)
        val display = getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        val overlayContext = createDisplayContext(display)
            .createWindowContext(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, null)
        overlayManager = overlayContext.getSystemService(WindowManager::class.java)
        guard = GuardOverlay(overlayContext, overlayManager, ::seekAt, ::handleDecision, ::openSettings) { settings.haptics }
        settings.preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }, Context.RECEIVER_NOT_EXPORTED)
        connected = true
        RunTimeState.connection(true)
        handler.post(watchdog)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!connected || event == null) return
        val windowChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        val packageName = event.packageName?.toString()
        // Other apps' event text/source nodes are never read.
        if (windowChange && packageName != null && packageName != YouTubeShortsDetector.PACKAGE && packageName != this.packageName) {
            cancelInjection()
            release("Outside YouTube", "Guard released")
        }
        if (windowChange || packageName == YouTubeShortsDetector.PACKAGE) scheduleEvaluation()
    }

    private fun scheduleEvaluation() {
        handler.removeCallbacks(evaluateTask)
        handler.post(evaluateTask)
    }

    private fun evaluate() {
        if (!connected) return
        if (!settings.shouldProtect) {
            cancelInjection()
            release("Protection paused", "Turn on protection in RunTime")
            return
        }
        if (!getSystemService(PowerManager::class.java).isInteractive ||
            getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
            cancelInjection()
            release("Phone locked", "Guard released")
            return
        }
        if (getSystemService(AccessibilityManager::class.java).isTouchExplorationEnabled) {
            cancelInjection()
            release("Unsupported accessibility configuration", "Touch exploration is active; guard released")
            return
        }
        if (tapInFlight) return
        try {
            val frame = readForeground()
            if (frame == null) {
                release("Outside supported viewer", "Guard released")
                return
            }
            val result = detector.detect(frame)
            snapshot = frame
            detection = result
            if (!result.guardable) {
                guard.hide()
                RunTimeState.screen(if (result.kind == ScreenKind.UNSUPPORTED) "Unsupported screen" else "Waiting for a Short",
                    result.reason, false, result.markerIds)
                return
            }
            val installed = guard.show(result.guardRegions())
            RunTimeState.screen(
                if (installed) "One video protected" else "Protection unavailable",
                if (installed) result.reason else "Could not attach the touch guard",
                installed, result.markerIds,
            )
        } catch (_: RuntimeException) {
            release("Protection unavailable", "Could not read the current screen")
        }
    }

    /** Read target UI only after checking foreground metadata. System windows take priority. */
    private fun readForeground(): ScreenSnapshot? {
        val windows = windows
        if (windows.any { it.type == AccessibilityWindowInfo.TYPE_SYSTEM && (it.isFocused || it.isActive) }) return null
        val applications = windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        val foreground = applications.firstOrNull { it.isFocused } ?: applications.firstOrNull { it.isActive } ?: return null
        val root = foreground.root ?: return null
        try {
            if (root.packageName?.toString() != YouTubeShortsDetector.PACKAGE) return null
            val metrics = overlayManager.currentWindowMetrics
            val displayBounds = metrics.bounds.toBox()
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val safeBounds = Box(displayBounds.left + insets.left, displayBounds.top + insets.top,
                displayBounds.right - insets.right, displayBounds.bottom - insets.bottom)
            return ScreenReader.read(root, displayBounds,
                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT,
                applications.size == 1)?.let { it.copy(appBounds = it.appBounds.intersect(safeBounds)) }
        } finally { root.recycleCompat() }
    }

    private fun seekAt(point: Point): Element? = snapshot?.elements
        ?.filter { it.enabled && it.role == Role.SEEK && it.bounds.contains(point) }
        ?.minByOrNull { it.bounds.area }

    private fun handleDecision(decision: TouchDecision) {
        when (decision) {
            TouchDecision.Block -> RunTimeState.blocked()
            TouchDecision.Ignore -> Unit
            is TouchDecision.Seek -> setProgress(decision.element, decision.fraction)
            is TouchDecision.Tap -> {
                val frame = snapshot ?: return
                if (frame.elements.any { it.role == Role.VIDEO_NAVIGATION && it.bounds.contains(decision.point) }) {
                    RunTimeState.blocked()
                    return
                }
                val seek = seekAt(decision.point)
                if (seek != null) {
                    setProgress(seek, ((decision.point.x - seek.bounds.left) / seek.bounds.width.coerceAtLeast(1)).coerceIn(0f, 1f))
                    return
                }
                val button = frame.elements.filter {
                    it.enabled && it.clickable && it.bounds.contains(decision.point) &&
                        it.bounds.area < frame.appBounds.area * 0.3
                }.minByOrNull { it.bounds.area }
                if (button != null && performNodeAction(button, AccessibilityNodeInfo.ACTION_CLICK, null)) {
                    RunTimeState.tapped()
                    scheduleEvaluation()
                } else forwardTap(decision.point)
            }
        }
    }

    private fun performNodeAction(element: Element, action: Int, arguments: Bundle?): Boolean {
        val previous = snapshot ?: return false
        val root = windows.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused }?.root ?: return false
        try {
            if (root.packageName?.toString() != YouTubeShortsDetector.PACKAGE || root.windowId != previous.windowId) return false
            val node = ScreenReader.atPath(root, element.path) ?: return false
            try {
                val id = node.viewIdResourceName?.substringAfter(":id/").orEmpty()
                if (id != element.id || !node.isVisibleToUser || !node.isEnabled) return false
                // Child positions can change as controls appear. Require a matching rectangle too.
                if (android.graphics.Rect().also(node::getBoundsInScreen).toBox() != element.bounds) return false
                return node.performAction(action, arguments)
            } finally { node.recycleCompat() }
        } catch (_: RuntimeException) {
            return false
        } finally { root.recycleCompat() }
    }

    private fun setProgress(element: Element, fraction: Float) {
        val range = element.range ?: return
        val arguments = Bundle().apply {
            putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, range.min + fraction * (range.max - range.min))
        }
        if (performNodeAction(element, AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id, arguments)) {
            RunTimeState.sought()
        } else RunTimeState.failed("Native seek action unavailable")
        scheduleEvaluation()
    }

    /** Short-tap fallback only. Never inject any movement captured by the guard. */
    private fun forwardTap(point: Point) {
        val previous = snapshot ?: return
        if (!connected || tapInFlight || !settings.shouldProtect) return
        tapInFlight = true
        val generation = ++injectionGeneration
        guard.hide()
        RunTimeState.screen("Forwarding a control tap", "Guard briefly released for the native tap", false)
        handler.postDelayed({
            if (!connected || !tapInFlight || generation != injectionGeneration || !settings.shouldProtect) return@postDelayed
            val current = try { readForeground() } catch (_: RuntimeException) { null }
            if (current == null || current.windowId != previous.windowId || !detector.detect(current).guardable) {
                finishTap(generation, false)
                return@postDelayed
            }
            val path = Path().apply { moveTo(point.x, point.y) }
            val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 1)).build()
            val dispatched = try {
                dispatchGesture(gesture, object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) { finishTap(generation, true) }
                    override fun onCancelled(gestureDescription: GestureDescription?) { finishTap(generation, false) }
                }, handler)
            } catch (_: RuntimeException) { false }
            if (!dispatched) finishTap(generation, false)
        }, 32)
        handler.postDelayed({
            if (tapInFlight && generation == injectionGeneration) finishTap(generation, false)
        }, 350)
    }

    private fun finishTap(generation: Int, success: Boolean) {
        if (generation != injectionGeneration || !tapInFlight) return
        tapInFlight = false
        if (success) RunTimeState.tapped() else RunTimeState.failed("Native tap forwarding failed")
        scheduleEvaluation()
    }

    private fun cancelInjection() {
        injectionGeneration++
        tapInFlight = false
    }

    private fun release(screen: String, detail: String) {
        if (::guard.isInitialized) guard.hide()
        snapshot = null
        detection = null
        RunTimeState.screen(screen, detail, false)
    }

    private fun openSettings() {
        cancelInjection()
        release("RunTime settings", "Guard released")
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        cancelInjection()
        release("Display changed", "Rechecking screen compatibility")
        if (connected) scheduleEvaluation()
    }

    override fun onInterrupt() {
        cancelInjection()
        release("Service interrupted", "Guard released")
    }

    private fun disconnect() {
        if (!connected) return
        connected = false
        cancelInjection()
        handler.removeCallbacksAndMessages(null)
        guard.hide()
        snapshot = null
        settings.preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        try { unregisterReceiver(screenReceiver) } catch (_: IllegalArgumentException) { }
        RunTimeState.connection(false)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        disconnect()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }
}
