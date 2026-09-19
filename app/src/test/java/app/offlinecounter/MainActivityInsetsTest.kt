package app.offlinecounter

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35, 36])
class MainActivityInsetsTest {
    @Test
    fun systemInsetsAreAppliedWithoutAccumulatingPadding() {
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val content = controller.get().findViewById<View>(R.id.rootContent)
            val density = content.resources.displayMetrics.density
            val top = (72 * density + 0.5f).toInt()
            val bottom = (24 * density + 0.5f).toInt()
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, 32))
                .build()
            repeat(2) { ViewCompat.dispatchApplyWindowInsets(content, insets) }
            assertEquals(top + 24, content.paddingTop)
            assertEquals(bottom + 32, content.paddingBottom)
        }
    }
}
