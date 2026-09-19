package app.offlinecounter

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
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
    private val darkTheme = themed.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    private val surface = if (darkTheme) Color.rgb(30, 38, 49) else Color.rgb(246, 248, 250)
    private val onSurface = if (darkTheme) Color.rgb(245, 247, 250) else Color.rgb(30, 38, 49)
    private val primary = themed.materialColor(
        com.google.android.material.R.attr.colorPrimary,
        Color.rgb(37, 99, 235),
    )
    private val secondary = themed.materialColor(
        com.google.android.material.R.attr.colorSecondary,
        Color.rgb(26, 175, 172),
    )

    val root = LinearLayout(themed)
    val dragHandle = DragHandleButton(themed)
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
    private var dataActionsVisible = false

    init {
        root.orientation = LinearLayout.VERTICAL
        root.elevation = themed.dp(7f)
        root.background = panelBackground(false)
        root.foreground = null
        root.clipToOutline = true

        val content = LinearLayout(themed).apply {
            orientation = LinearLayout.VERTICAL
            val padding = themed.dp(4f).toInt()
            setPadding(padding, padding, padding, padding)
        }
        root.addView(content, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        val compact = LinearLayout(themed).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        content.addView(compact)

        configureDragHandle()
        compact.addView(dragHandle, fixed(themed.dp(38f).toInt(), themed.dp(48f).toInt()))

        configureTextButton(countButton, themed.getString(R.string.overlay_count), primary, actions::onCount)
        compact.addView(countButton, wrapWithStartMargin(2))

        configureTextButton(excelButton, themed.getString(R.string.overlay_export), secondary, actions::onExport)
        excelButton.visibility = View.GONE
        compact.addView(excelButton, wrapWithStartMargin(4))

        configureIconButton(shareButton, R.drawable.ic_share, themed.getString(R.string.share_excel), actions::onShare)
        shareButton.visibility = View.GONE
        compact.addView(shareButton, fixedWithStartMargin(48, 48, 4))

        configureIconButton(
            expandButton,
            R.drawable.ic_expand_more,
            themed.getString(R.string.overlay_expand),
        ) { setExpanded(!expanded) }
        compact.addView(expandButton, fixedWithStartMargin(48, 48, 4))

        configureIconButton(closeButton, R.drawable.ic_close, themed.getString(R.string.overlay_disable), actions::onClose)
        compact.addView(closeButton, fixedWithStartMargin(48, 48, 4))

        details.orientation = LinearLayout.VERTICAL
        details.visibility = View.GONE
        details.setPadding(
            themed.dp(10f).toInt(),
            themed.dp(8f).toInt(),
            themed.dp(10f).toInt(),
            themed.dp(8f).toInt(),
        )
        content.addView(details)

        statusText.setTextColor(ColorUtils.setAlphaComponent(onSurface, 220))
        statusText.textSize = 13f
        statusText.setText(R.string.overlay_ready)
        details.addView(statusText)

        totalsText.setTextColor(onSurface)
        totalsText.textSize = 15f
        totalsText.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        totalsText.setPadding(0, themed.dp(6f).toInt(), 0, themed.dp(8f).toInt())
        totalsText.text = themed.getString(R.string.overlay_totals, 0, 0, 0)
        details.addView(totalsText)

        val detailActions = LinearLayout(themed).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        details.addView(detailActions)

        configureTextButton(
            stopButton,
            themed.getString(R.string.overlay_recount),
            Color.rgb(185, 28, 28),
            actions::onStopOrRecount,
        )
        detailActions.addView(stopButton)
        configureTextButton(
            clearButton,
            themed.getString(R.string.overlay_clear),
            Color.rgb(71, 85, 105),
            actions::onClear,
        )
        detailActions.addView(clearButton, wrapWithStartMargin(8))
    }

    fun update(total: Int, women: Int, men: Int, collecting: Boolean) {
        closeButton.isEnabled = !collecting
        closeButton.alpha = if (collecting) 0.35f else 1f
        setDataActionsVisible(OverlayPresentation.forTotal(total).showDataActions)
        countButton.text = if (collecting || total > 0) {
            themed.getString(R.string.overlay_total, total)
        } else {
            themed.getString(R.string.overlay_count)
        }
        totalsText.text = themed.getString(R.string.overlay_totals, total, women, men)
        stopButton.text = themed.getString(if (collecting) R.string.overlay_stop else R.string.overlay_recount)
    }

    fun setStatus(text: String, reveal: Boolean = false) {
        statusText.text = text
        if (reveal) setExpanded(true)
    }

    fun setExpanded(value: Boolean) {
        if (expanded == value) return
        expanded = value
        details.visibility = if (value) View.VISIBLE else View.GONE
        expandButton.contentDescription = themed.getString(
            if (value) R.string.overlay_collapse else R.string.overlay_expand,
        )
        expandButton.setIconResource(if (value) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
        root.background = panelBackground(value)
        root.requestLayout()
    }

    fun collapse() = setExpanded(false)

    private fun setDataActionsVisible(visible: Boolean) {
        if (dataActionsVisible == visible) return
        dataActionsVisible = visible
        listOf(excelButton, shareButton).forEach { button ->
            button.animate().cancel()
            button.visibility = if (visible) View.VISIBLE else View.GONE
            button.alpha = if (visible) 0f else 1f
            if (visible) button.animate().alpha(1f).setDuration(180).start()
        }
        root.requestLayout()
    }

    private fun panelBackground(expanded: Boolean) = GradientDrawable().apply {
        cornerRadius = themed.dp(18f)
        if (darkTheme) {
            setColor(ColorUtils.setAlphaComponent(surface, if (expanded) 248 else 232))
        } else {
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = if (expanded) {
                intArrayOf(Color.argb(248, 252, 253, 255), Color.argb(242, 236, 241, 246))
            } else {
                intArrayOf(Color.argb(200, 252, 253, 255), ColorUtils.setAlphaComponent(surface, 184))
            }
        }
    }

    private fun configureDragHandle() {
        dragHandle.setImageResource(R.drawable.ic_drag_handle)
        dragHandle.setColorFilter(ColorUtils.setAlphaComponent(onSurface, 190))
        dragHandle.setBackgroundColor(Color.TRANSPARENT)
        dragHandle.contentDescription = themed.getString(R.string.overlay_move)
        dragHandle.setPadding(
            themed.dp(7f).toInt(),
            themed.dp(8f).toInt(),
            themed.dp(7f).toInt(),
            themed.dp(8f).toInt(),
        )
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
        button.setOnClickListener {
            it.nativeTap()
            action()
        }
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
        val padding = themed.dp(13f).toInt()
        button.setPadding(padding, padding, padding, padding)
        button.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(onSurface, 28))
        button.setOnClickListener {
            it.nativeTap()
            action()
        }
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

private fun Context.materialColor(attribute: Int, fallback: Int): Int =
    MaterialColors.getColor(this, attribute, fallback)

private fun View.nativeTap() {
    performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
}
