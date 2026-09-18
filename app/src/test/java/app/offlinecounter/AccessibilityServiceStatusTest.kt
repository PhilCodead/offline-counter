package app.offlinecounter

import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityServiceStatusTest {
    @Test
    fun matchesFlattenedEnabledServiceName() {
        assertTrue(AccessibilityServiceStatus.contains(
            "other.pkg/.Service:app.offlinecounter/.CounterAccessibilityService",
            "app.offlinecounter",
            "app.offlinecounter.CounterAccessibilityService",
        ))
    }
}
