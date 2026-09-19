package app.offlinecounter

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilitySettingsRequestTest {
    @Test
    fun targetsThisServiceAndFallsBackToAccessibilityList() {
        val request = AccessibilitySettingsRequest.forService(
            packageName = "app.offlinecounter",
            serviceClassName = "app.offlinecounter.CounterAccessibilityService",
        )

        assertEquals("android.settings.ACCESSIBILITY_DETAILS_SETTINGS", request.detailsAction)
        assertEquals(
            "app.offlinecounter/app.offlinecounter.CounterAccessibilityService",
            request.componentName,
        )
        assertEquals("android.settings.ACCESSIBILITY_SETTINGS", request.fallbackAction)
    }
}
