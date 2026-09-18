package app.offlinecounter

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.transition.TransitionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {
    private var guideExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.versionText).text =
            getString(R.string.version_format, BuildConfig.VERSION_NAME)

        findViewById<MaterialCardView>(R.id.serviceCard).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            setGuideExpanded(!guideExpanded)
        }

        findViewById<MaterialButton>(R.id.enableAccessibility).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            openAccessibilitySettings()
        }
    }

    override fun onResume() {
        super.onResume()
        val enabled = AccessibilityServiceStatus.contains(
            Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            packageName,
            CounterAccessibilityService::class.java.name,
        )
        findViewById<TextView>(R.id.serviceStatus).setText(if (enabled) R.string.service_enabled else R.string.service_disabled)
        findViewById<MaterialButton>(R.id.enableAccessibility).setText(if (enabled) R.string.manage_accessibility else R.string.enable_accessibility)
    }

    private fun setGuideExpanded(expanded: Boolean) {
        if (guideExpanded == expanded) return
        guideExpanded = expanded
        TransitionManager.beginDelayedTransition(findViewById(R.id.rootContent))
        findViewById<LinearLayout>(R.id.setupGuide).visibility = if (expanded) View.VISIBLE else View.GONE
        findViewById<ImageView>(R.id.serviceExpandIcon).animate()
            .rotation(if (expanded) 180f else 0f)
            .setDuration(180)
            .start()
        findViewById<MaterialCardView>(R.id.serviceCard).contentDescription = getString(
            if (expanded) R.string.collapse_setup_guide else R.string.expand_setup_guide,
        )
    }

    private fun openAccessibilitySettings() {
        val request = AccessibilitySettingsRequest.forService(
            packageName = packageName,
            serviceClassName = CounterAccessibilityService::class.java.name,
        )
        val fallback = Intent(request.fallbackAction)
        val details = Intent(request.detailsAction).putExtra(Intent.EXTRA_COMPONENT_NAME, request.componentName)
        val target = if (details.resolveActivity(packageManager) != null) details else fallback
        runCatching { startActivity(target) }
            .recoverCatching { startActivity(fallback) }
    }
}
