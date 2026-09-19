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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.transition.TransitionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {
    private lateinit var rootContent: LinearLayout
    private lateinit var serviceCard: MaterialCardView
    private lateinit var serviceStatus: TextView
    private lateinit var setupGuide: LinearLayout
    private lateinit var serviceExpandIcon: ImageView
    private lateinit var accessibilityButton: MaterialButton
    private var guideExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        bindViews()
        applySystemInsets()

        findViewById<TextView>(R.id.versionText).text =
            getString(R.string.version_format, BuildConfig.VERSION_NAME)

        serviceCard.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            setGuideExpanded(!guideExpanded)
        }

        accessibilityButton.setOnClickListener {
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
        serviceStatus.setText(if (enabled) R.string.service_enabled else R.string.service_disabled)
        accessibilityButton.setText(if (enabled) R.string.manage_accessibility else R.string.enable_accessibility)
    }

    private fun bindViews() {
        rootContent = findViewById(R.id.rootContent)
        serviceCard = findViewById(R.id.serviceCard)
        serviceStatus = findViewById(R.id.serviceStatus)
        setupGuide = findViewById(R.id.setupGuide)
        serviceExpandIcon = findViewById(R.id.serviceExpandIcon)
        accessibilityButton = findViewById(R.id.enableAccessibility)
    }

    private fun applySystemInsets() {
        val left = rootContent.paddingLeft
        val top = rootContent.paddingTop
        val right = rootContent.paddingRight
        val bottom = rootContent.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(rootContent) { view, insets ->
            val system = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            view.setPadding(left + system.left, top + system.top, right + system.right, bottom + system.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(rootContent)
    }

    private fun setGuideExpanded(expanded: Boolean) {
        if (guideExpanded == expanded) return
        guideExpanded = expanded
        TransitionManager.beginDelayedTransition(rootContent)
        setupGuide.visibility = if (expanded) View.VISIBLE else View.GONE
        serviceExpandIcon.setImageResource(
            if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
        )
        serviceCard.contentDescription = getString(
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
        runCatching { startActivity(details) }
            .recoverCatching { startActivity(fallback) }
    }
}
