package app.offlinecounter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29, 35, 36])
class OverlayPanelTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun closeIsDisabledWhileCountingAndRestoredAfterCompletion() {
        val panel = OverlayPanel(context, Actions())
        val close = panel.root.descendants().first {
            it.contentDescription == context.getString(R.string.overlay_disable)
        }
        panel.update(2, 1, 1, collecting = true)
        assertFalse(close.isEnabled)
        assertTrue(close.alpha < 1f)
        panel.update(2, 1, 1, collecting = false)
        assertTrue(close.isEnabled)
        assertEquals(1f, close.alpha, 0f)
    }

    @Test
    fun clearingDataRestoresCompactWidthAndHidesExport() {
        val panel = OverlayPanel(context, Actions())
        fun width(): Int {
            panel.root.measure(
                View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.AT_MOST),
            )
            return panel.root.measuredWidth
        }
        val initialWidth = width()
        val share = panel.root.descendants().first {
            it.contentDescription == context.getString(R.string.share_excel)
        }
        assertEquals(View.GONE, share.visibility)
        panel.update(12, 6, 6, collecting = false)
        assertEquals(View.VISIBLE, share.visibility)
        assertTrue(width() > initialWidth)
        panel.root.descendants().first {
            it.contentDescription == context.getString(R.string.overlay_expand)
        }.performClick()
        panel.update(0, 0, 0, collecting = false)
        panel.collapse()
        assertEquals(View.GONE, share.visibility)
        assertEquals(initialWidth, width())
    }

    private fun View.descendants(): Sequence<View> = sequence {
        yield(this@descendants)
        if (this@descendants is ViewGroup) {
            for (index in 0 until childCount) yieldAll(getChildAt(index).descendants())
        }
    }

    private class Actions : OverlayPanel.Actions {
        override fun onCount() = Unit
        override fun onExport() = Unit
        override fun onShare() = Unit
        override fun onStopOrRecount() = Unit
        override fun onClear() = Unit
        override fun onClose() = Unit
    }
}
