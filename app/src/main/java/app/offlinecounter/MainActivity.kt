package app.offlinecounter

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.versionText).text =
            getString(R.string.version_format, BuildConfig.VERSION_NAME)

        findViewById<MaterialButton>(R.id.enableAccessibility).setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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
}
