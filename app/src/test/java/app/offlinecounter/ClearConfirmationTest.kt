package app.offlinecounter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClearConfirmationTest {
    @Test
    fun firstTapAtBootTimeRequiresConfirmation() {
        val confirmation = ClearConfirmation(windowMs = 3_500L)
        assertFalse(confirmation.confirm(now = 0L))
        assertTrue(confirmation.confirm(now = 1L))
    }

    @Test
    fun successfulConfirmationIsConsumed() {
        val confirmation = ClearConfirmation(windowMs = 3_500L)

        assertFalse(confirmation.confirm(now = 1_000L))
        assertTrue(confirmation.confirm(now = 2_000L))
        assertFalse(confirmation.confirm(now = 2_100L))
    }

    @Test
    fun expiredConfirmationStartsANewWindow() {
        val confirmation = ClearConfirmation(windowMs = 3_500L)

        assertFalse(confirmation.confirm(now = 1_000L))
        assertFalse(confirmation.confirm(now = 4_501L))
        assertTrue(confirmation.confirm(now = 5_000L))
    }
}
