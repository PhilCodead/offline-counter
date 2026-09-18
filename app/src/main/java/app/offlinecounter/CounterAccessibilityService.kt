package app.offlinecounter

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ContextThemeWrapper
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import java.util.LinkedHashMap

class CounterAccessibilityService : AccessibilityService(), OverlayPanel.Actions {
    private enum class Phase { Idle, Rewinding, Collecting }

    private val handler = Handler(Looper.getMainLooper())
    private val parser = PersonParser()
    private val scroll = ScrollTracker()
    private val people = LinkedHashMap<String, Person>()
    private val step = Runnable(::runCycle)

    private lateinit var windowManager: WindowManager
    private lateinit var panel: OverlayPanel
    private lateinit var windowParams: WindowManager.LayoutParams

    private var phase = Phase.Idle
    private var lastPackage: String? = null
    private var targetPackage: String? = null
    private var lastFingerprint: Int? = null
    private var stableFrames = 0
    private var stalePasses = 0
    private var rewindSteps = 0
    private var scrollSteps = 0
    private var unavailableTicks = 0
    private var lastExport: Uri? = null

    override fun onServiceConnected() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        panel = OverlayPanel(this, this)
        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 8
            y = 180
        }
        panel.dragHandle.setOnTouchListener(DragListener())
        windowManager.addView(panel.root, windowParams)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName || packageName == "com.android.systemui" || packageName == "com.android.settings") return

        lastPackage = packageName
        scroll.update(event)

        if (phase != Phase.Idle && packageName == targetPackage) schedule(70)
    }

    override fun onInterrupt() = stop("Остановлено")

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::panel.isInitialized) runCatching { windowManager.removeView(panel.root) }
        super.onDestroy()
    }

    override fun onCount() = startCount()

    override fun onExport() {
        runCatching { ExcelExporter.export(this, people.values) }
            .onSuccess { uri ->
                lastExport = uri
                panel.setStatus("Excel сохранён", true)
                openExportLocation(uri)
            }
            .onFailure { panel.setStatus("Не удалось сохранить Excel", true) }
    }

    override fun onShare() {
        val uri = lastExport ?: return panel.setStatus("Сначала сохраните Excel", true)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(Intent.createChooser(intent, "Передать Excel").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onStopOrRecount() {
        if (phase == Phase.Idle) startCount() else stop("Остановлено")
    }

    override fun onClear() {
        phase = Phase.Idle
        handler.removeCallbacks(step)
        people.clear()
        panel.update(0, 0, 0, false)
        panel.setStatus("Очищено")
    }

    override fun onClose() {
        handler.removeCallbacksAndMessages(null)
        phase = Phase.Idle
        disableSelf()
    }

    private fun startCount() {
        val packageName = lastPackage ?: return panel.setStatus("Сначала откройте список", true)
        targetPackage = packageName
        people.clear()
        phase = Phase.Rewinding
        lastFingerprint = null
        stableFrames = 0
        stalePasses = 0
        rewindSteps = 0
        scrollSteps = 0
        unavailableTicks = 0
        scroll.reset()
        panel.update(0, 0, 0, true)
        panel.setStatus("Возврат к началу списка…")
        schedule(0)
    }

    private fun runCycle() {
        if (phase == Phase.Idle) return
        val root = rootInActiveWindow
        if (root == null || root.packageName?.toString() != targetPackage) {
            if (++unavailableTicks >= 20) stop("Список недоступен — нажмите «Пересчитать»") else schedule(250)
            return
        }
        unavailableTicks = 0

        val fingerprint = parser.fingerprint(root)
        stableFrames = if (fingerprint == lastFingerprint) stableFrames + 1 else 0
        lastFingerprint = fingerprint

        when (phase) {
            Phase.Rewinding -> rewind(root)
            Phase.Collecting -> collect(root)
            Phase.Idle -> Unit
        }
    }

    private fun rewind(root: AccessibilityNodeInfo) {
        if (rewindSteps > 0 && scroll.isAtTop()) return beginCollection()
        val scrollable = findScrollable(root) ?: return beginCollection()
        if (stableFrames >= 2) return beginCollection()
        if (!scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) return beginCollection()
        if (++rewindSteps >= 350) return beginCollection()
        schedule(180)
    }

    private fun beginCollection() {
        phase = Phase.Collecting
        stableFrames = 0
        stalePasses = 0
        lastFingerprint = null
        panel.setStatus("Сбор данных…")
        schedule(90)
    }

    private fun collect(root: AccessibilityNodeInfo) {
        val before = people.size
        parser.parse(root).forEach { person -> people.putIfAbsent(person.key, person) }
        val added = people.size - before
        panel.update(people.size, people.values.count { it.sex == "Ж" }, people.values.count { it.sex == "М" }, true)

        if (scroll.isAtBottom()) return finishCount()
        val scrollable = findScrollable(root) ?: return finishCount()
        if (!scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return finishCount()

        scrollSteps++
        stalePasses = if (added == 0) stalePasses + 1 else 0
        if (added > 0) stableFrames = 0

        if (stableFrames >= 2 || stalePasses >= 15 || scrollSteps >= 1000) return finishCount()
        schedule(220)
    }

    private fun finishCount() {
        stop("Готово")
        showCompletionFeedback()
    }

    private fun stop(status: String) {
        phase = Phase.Idle
        handler.removeCallbacks(step)
        panel.update(people.size, people.values.count { it.sex == "Ж" }, people.values.count { it.sex == "М" }, false)
        panel.setStatus(status)
    }

    private fun schedule(delayMs: Long) {
        handler.removeCallbacks(step)
        handler.postDelayed(step, delayMs)
    }

    private fun findScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (index in 0 until node.childCount) findScrollable(node.getChild(index))?.let { return it }
        return null
    }

    private fun showCompletionFeedback() {
        runCatching {
            val context = DynamicColors.wrapContextIfAvailable(ContextThemeWrapper(this, R.style.Theme_OfflineCounter_Overlay))
            val onSurface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, android.graphics.Color.WHITE)
            val surface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceInverse, android.graphics.Color.rgb(31, 41, 55))
            val card = MaterialCardView(context).apply {
                radius = 18f * resources.displayMetrics.density
                cardElevation = 8f * resources.displayMetrics.density
                setCardBackgroundColor(ColorUtils.setAlphaComponent(surface, 246))
            }
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(11), dp(18), dp(11))
            }
            row.addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_done)
                setColorFilter(onSurface)
            }, LinearLayout.LayoutParams(dp(22), dp(22)))
            row.addView(TextView(context).apply {
                text = "Готово — подсчёт успешно завершён"
                setTextColor(onSurface)
                textSize = 14f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(10)
            })
            card.addView(row)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = dp(72)
            }
            windowManager.addView(card, params)
            handler.postDelayed({ runCatching { windowManager.removeView(card) } }, 2800)
        }.onFailure {
            Toast.makeText(this, "Готово", Toast.LENGTH_LONG).show()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun openExportLocation(uri: Uri) {
        val folder = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FOfflineCounter")
        val folderIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(folder, "vnd.android.document/directory")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (folderIntent.resolveActivity(packageManager) != null) {
            startActivity(folderIntent)
            return
        }
        val fileIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (fileIntent.resolveActivity(packageManager) != null) startActivity(fileIntent)
    }

    private inner class DragListener : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var touchX = 0f
        private var touchY = 0f

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    startX = windowParams.x
                    startY = windowParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    windowParams.x = startX + (event.rawX - touchX).toInt()
                    windowParams.y = startY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(panel.root, windowParams)
                }
            }
            return true
        }
    }
}
