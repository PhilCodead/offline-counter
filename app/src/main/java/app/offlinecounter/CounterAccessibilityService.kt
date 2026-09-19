package app.offlinecounter

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.annotation.StringRes

class CounterAccessibilityService : AccessibilityService(), OverlayPanel.Actions {
    private val handler = Handler(Looper.getMainLooper())
    private val parser = PersonParser()
    private val scroll = ScrollTracker()
    private val controller = CountingController()
    private val step = Runnable(::runCycle)

    private lateinit var windowManager: WindowManager
    private lateinit var panel: OverlayPanel
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var preferences: OverlayPreferences
    private lateinit var overlayNotice: OverlayNotice
    private lateinit var noticeDispatcher: NoticeDispatcher
    private var lastPackage: String? = null
    private var targetPackage: String? = null
    private var lastExport: Uri? = null
    private var clearConfirmationUntil = 0L

    override fun onServiceConnected() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        preferences = OverlayPreferences(this)
        panel = OverlayPanel(this, this)
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = dp(180) }
        panel.dragHandle.setOnTouchListener(DragListener())
        windowManager.addView(panel.root, params)
        overlayNotice = OverlayNotice(this, windowManager, handler)
        noticeDispatcher = NoticeDispatcher(::showSystemToast, overlayNotice::show)
        panel.root.post(::restorePosition)
        render()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName || packageName == "com.android.systemui" || packageName == "com.android.settings") return
        lastPackage = packageName
        scroll.update(event)
        if (controller.state.phase in setOf(CountingPhase.Rewinding, CountingPhase.Collecting) && packageName == targetPackage) schedule(70)
    }

    override fun onInterrupt() { controller.stop(); render() }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::overlayNotice.isInitialized) overlayNotice.destroy()
        if (::panel.isInitialized) runCatching { windowManager.removeView(panel.root) }
        super.onDestroy()
    }

    override fun onCount() = startCount()

    override fun onExport() {
        runCatching { ExcelExporter.export(this, controller.people) }
            .onSuccess { lastExport = it; notifyUser(R.string.export_saved); openExport(it) }
            .onFailure { notifyUser(R.string.export_failed) }
    }

    override fun onShare() {
        val uri = lastExport ?: return notifyUser(R.string.save_before_share)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, getString(R.string.share_excel)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onStopOrRecount() {
        if (controller.state.phase in setOf(CountingPhase.Rewinding, CountingPhase.Collecting)) { controller.stop(); handler.removeCallbacks(step); render() } else startCount()
    }

    override fun onClear() {
        val now = SystemClock.elapsedRealtime()
        if (now > clearConfirmationUntil) { clearConfirmationUntil = now + 3500; return notifyUser(R.string.confirm_clear) }
        handler.removeCallbacks(step); controller.clear(); render(); notifyUser(R.string.results_cleared)
    }

    override fun onClose() {
        disableSelf()
    }

    private fun startCount() {
        targetPackage = lastPackage ?: return notifyUser(R.string.open_list_first)
        scroll.reset(); controller.start(); render(); schedule(0)
    }

    private fun runCycle() {
        val root = rootInActiveWindow
        if (root == null || root.packageName?.toString() != targetPackage) {
            val command = controller.onSourceUnavailable(); render()
            if (command is CountingCommand.Fail) notifyUser(command.message) else schedule(250)
            return
        }
        val snapshot = FrameSnapshot(parser.parse(root), scroll.isAtTop(), scroll.isAtBottom(), parser.fingerprint(root))
        execute(controller.onFrame(snapshot), root)
    }

    private fun execute(command: CountingCommand, root: AccessibilityNodeInfo) {
        render()
        when (command) {
            CountingCommand.ScrollBackward -> if (findScrollable(root)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) == true) schedule(180) else execute(controller.onFrame(FrameSnapshot(emptyList(), true, false, 0)), root)
            CountingCommand.ScrollForward -> if (findScrollable(root)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) == true) schedule(220) else execute(controller.onFrame(FrameSnapshot(emptyList(), false, true, 0)), root)
            CountingCommand.Wait -> schedule(90)
            CountingCommand.Complete -> { render(); notifyUser(R.string.count_complete) }
            is CountingCommand.Fail -> { render(); notifyUser(command.message) }
        }
    }

    private fun render() {
        val state = controller.state
        panel.update(state.total, state.women, state.men, state.phase in setOf(CountingPhase.Rewinding, CountingPhase.Collecting))
        if (state.status.isNotEmpty()) panel.setStatus(state.status)
    }

    private fun notifyUser(@StringRes message: Int) = notifyUser(getString(message))

    private fun notifyUser(message: String) {
        if (::noticeDispatcher.isInitialized) {
            noticeDispatcher.show(message)
        } else {
            showSystemToast(message)
        }
    }

    private fun showSystemToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    private fun schedule(delay: Long) { handler.removeCallbacks(step); handler.postDelayed(step, delay) }
    private fun findScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (index in 0 until node.childCount) findScrollable(node.getChild(index))?.let { return it }
        return null
    }

    private fun bounds() = OverlayBounds(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels, panel.root.measuredWidth.coerceAtLeast(dp(280)), panel.root.measuredHeight.coerceAtLeast(dp(68)), dp(24), dp(32))
    private fun restorePosition() { val (saved, edge) = preferences.load(); val b = bounds(); val p = OverlayPlacement.denormalize(saved, b); params.x = if (edge == OverlayEdge.Start) 0 else b.width - b.panelWidth; params.y = p.y; updateWindow() }
    private fun updateWindow() { if (::panel.isInitialized) runCatching { windowManager.updateViewLayout(panel.root, params) } }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun openExport(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK) }
        runCatching { startActivity(intent) }
    }

    private inner class DragListener : View.OnTouchListener {
        private var start = OverlayPosition(0, 0); private var touchX = 0f; private var touchY = 0f
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { start = OverlayPosition(params.x, params.y); touchX = event.rawX; touchY = event.rawY }
                MotionEvent.ACTION_MOVE -> { val p = OverlayPlacement.clamp(OverlayPosition(start.x + (event.rawX - touchX).toInt(), start.y + (event.rawY - touchY).toInt()), bounds()); params.x = p.x; params.y = p.y; updateWindow() }
                MotionEvent.ACTION_UP -> { finishDrag(view); view.performClick() }
                MotionEvent.ACTION_CANCEL -> finishDrag(view)
            }
            return true
        }

        private fun finishDrag(view: View) {
            val b = bounds()
            val p = OverlayPlacement.clamp(OverlayPosition(params.x, params.y), b)
            val edge = OverlayPlacement.nearestEdge(p, b)
            params.x = if (edge == OverlayEdge.Start) 0 else b.width - b.panelWidth
            params.y = p.y
            updateWindow()
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            preferences.save(OverlayPlacement.normalize(OverlayPosition(params.x, params.y), b), edge)
        }
    }
}
