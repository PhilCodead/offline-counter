package app.offlinecounter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

class OverlayPanel(context: Context, private val actions: Actions) {
    interface Actions {
        fun onCount()
        fun onExport()
        fun onShare()
        fun onStopOrRecount()
        fun onClear()
        fun onClose()
    }

    private val themed = ContextThemeWrapper(context, R.style.Theme_OfflineCounter_Overlay)
    private val surface = MaterialColors.getColor(themed, com.google.android.material.R.attr.colorSurface, Color.rgb(24, 31, 43))
    private val onSurface = MaterialColors.getColor(themed, com.google.android.material.R.attr.colorOnSurface, Color.WHITE)
    private val primary = MaterialColors.getColor(themed, com.google.android.material.R.attr.colorPrimary, Color.rgb(37, 99, 235))
    private val secondary = MaterialColors.getColor(themed, com.google.android.material.R.attr.colorSecondary, Color.rgb(26, 175, 172))

    val root = MaterialCardView(themed)
    val dragHandle = ImageButton(themed)
    private val countButton = MaterialButton(themed)
    private val excelButton = MaterialButton(themed)
    private val shareButton = MaterialButton(themed)
    private val expandButton = MaterialButton(themed)
    private val closeButton = MaterialButton(themed)
    private val details = LinearLayout(themed)
    private val statusText = TextView(themed)
    private val totalsText = TextView(themed)
    private val stopButton = MaterialButton(themed)
    private val clearButton = MaterialButton(themed)
    private var expanded = false
    private var blurAvailable = false

    init {
        root.radius = themed.dp(18f)
        root.cardElevation = themed.dp(7f)
        root.strokeWidth = themed.dp(1f).toInt()
        root.strokeColor = ColorUtils.setAlphaComponent(onSurface, 28)
        root.setCardBackgroundColor(ColorUtils.setAlphaComponent(surface, 88))
        root.clipToOutline = true

        val content = LinearLayout(themed).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(themed.dp(4f).toInt(), themed.dp(4f).toInt(), themed.dp(4f).toInt(), themed.dp(4f).toInt())
        }
        root.addView(content)

        val compact = LinearLayout(themed).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        content.addView(compact)

        configureDragHandle()
        compact.addView(dragHandle, fixed(themed.dp(38f).toInt(), themed.dp(48f).toInt()))

        configureTextButton(countButton, "Подсчёт", primary, actions::onCount)
        compact.addView(countButton, wrapWithStartMargin(2))

        configureTextButton(excelButton, "В Excel", secondary, actions::onExport)
        compact.addView(excelButton, wrapWithStartMargin(4))

        configureIconButton(shareButton, R.drawable.ic_share, "Передать Excel", actions::onShare)
        compact.addView(shareButton, fixedWithStartMargin(48, 48, 4))

        configureIconButton(expandButton, R.drawable.ic_expand_more, "Развернуть панель") { setExpanded(!expanded) }
        compact.addView(expandButton, fixedWithStartMargin(48, 48, 4))

        configureIconButton(closeButton, R.drawable.ic_close, "Отключить панель", actions::onClose)
        compact.addView(closeButton, fixedWithStartMargin(48, 48, 4))

        details.orientation = LinearLayout.VERTICAL
        details.minimumWidth = themed.dp(336f).toInt()
        details.visibility = View.GONE
        details.setPadding(themed.dp(10f).toInt(), themed.dp(8f).toInt(), themed.dp(10f).toInt(), themed.dp(8f).toInt())
        content.addView(details)

        statusText.setTextColor(ColorUtils.setAlphaComponent(onSurface, 220))
        statusText.textSize = 13f
        statusText.text = "Готов к сбору"
        details.addView(statusText)

        totalsText.setTextColor(onSurface)
        totalsText.textSize = 15f
        totalsText.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        totalsText.setPadding(0, themed.dp(6f).toInt(), 0, themed.dp(8f).toInt())
        totalsText.text = "Всего: 0   Ж: 0   М: 0"
        details.addView(totalsText)

        val detailActions = LinearLayout(themed).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        details.addView(detailActions)

        configureTextButton(stopButton, "Пересчитать", Color.rgb(185, 28, 28), actions::onStopOrRecount)
        detailActions.addView(stopButton)
        configureTextButton(clearButton, "Очистить", Color.rgb(71, 85, 105), actions::onClear)
        detailActions.addView(clearButton, wrapWithStartMargin(8))
    }

    fun update(total: Int, women: Int, men: Int, collecting: Boolean) {
        if (collecting || total > 0) countButton.text = "Всего: $total" else countButton.text = "Подсчёт"
        totalsText.text = "Всего: $total   Ж: $women   М: $men"
        stopButton.text = if (collecting) "Остановить" else "Пересчитать"
    }

    fun setStatus(text: String, reveal: Boolean = false) {
        statusText.text = text
        if (reveal) setExpanded(true)
    }

    fun setExpanded(value: Boolean) {
        if (expanded == value) return
        android.transition.TransitionManager.beginDelayedTransition(root)
        expanded = value
        details.visibility = if (value) View.VISIBLE else View.GONE
        expandButton.contentDescription = if (value) "Свернуть панель" else "Развернуть панель"
        expandButton.animate().rotation(if (value) 180f else 0f).setDuration(180).start()
        val mode = glassMode(value, blurAvailable)
        val alpha = when (mode) {
            GlassMode.CompactBlur -> 118
            GlassMode.ExpandedBlur -> 210
            GlassMode.CompactOpaque -> 232
            GlassMode.ExpandedOpaque -> 248
        }
        root.setCardBackgroundColor(ColorUtils.setAlphaComponent(surface, alpha))
        root.strokeColor = ColorUtils.setAlphaComponent(onSurface, if (value) 38 else 28)
    }

    fun setBlurAvailable(available: Boolean) {
        blurAvailable = available
        val current = expanded
        expanded = !current
        setExpanded(current)
    }

    private fun configureDragHandle() {
        dragHandle.setImageResource(R.drawable.ic_drag_handle)
        dragHandle.setColorFilter(ColorUtils.setAlphaComponent(onSurface, 190))
        dragHandle.setBackgroundColor(Color.TRANSPARENT)
        dragHandle.contentDescription = "Переместить"
        dragHandle.setPadding(themed.dp(7f).toInt(), themed.dp(8f).toInt(), themed.dp(7f).toInt(), themed.dp(8f).toInt())
    }

    private fun configureTextButton(button: MaterialButton, text: String, color: Int, action: () -> Unit) {
        button.text = text
        button.textSize = 13f
        button.isAllCaps = false
        button.minHeight = themed.dp(48f).toInt()
        button.minimumHeight = themed.dp(48f).toInt()
        button.minWidth = 0
        button.minimumWidth = 0
        button.cornerRadius = themed.dp(14f).toInt()
        button.insetTop = 0
        button.insetBottom = 0
        button.setPadding(themed.dp(10f).toInt(), 0, themed.dp(10f).toInt(), 0)
        button.backgroundTintList = ColorStateList.valueOf(color)
        button.setTextColor(Color.WHITE)
        button.setOnClickListener { it.nativeTap(); action() }
    }

    private fun configureIconButton(button: MaterialButton, icon: Int, description: String, action: () -> Unit) {
        button.text = ""
        button.setIconResource(icon)
        button.iconTint = ColorStateList.valueOf(onSurface)
        button.iconPadding = 0
        button.iconSize = themed.dp(22f).toInt()
        button.contentDescription = description
        button.cornerRadius = themed.dp(14f).toInt()
        button.insetTop = 0
        button.insetBottom = 0
        button.minWidth = 0
        button.minimumWidth = 0
        button.minHeight = 0
        button.minimumHeight = 0
        button.setPadding(themed.dp(13f).toInt(), themed.dp(13f).toInt(), themed.dp(13f).toInt(), themed.dp(13f).toInt())
        button.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(onSurface, 28))
        button.setOnClickListener { it.nativeTap(); action() }
    }

    private fun fixed(width: Int, height: Int) = LinearLayout.LayoutParams(width, height)

    private fun fixedWithStartMargin(widthDp: Int, heightDp: Int, marginDp: Int) =
        LinearLayout.LayoutParams(themed.dp(widthDp.toFloat()).toInt(), themed.dp(heightDp.toFloat()).toInt()).apply {
            marginStart = themed.dp(marginDp.toFloat()).toInt()
        }

    private fun wrapWithStartMargin(marginDp: Int) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, themed.dp(48f).toInt()).apply {
            marginStart = themed.dp(marginDp.toFloat()).toInt()
        }
}

private fun Context.dp(value: Float): Float = value * resources.displayMetrics.density

private fun View.nativeTap() {
    performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
}
