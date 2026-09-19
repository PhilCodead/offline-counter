package app.offlinecounter

import org.junit.Assert.assertEquals
import org.junit.Test

class NoticeDispatcherTest {
    @Test
    fun everyMessageUsesSystemAndOverlayChannels() {
        val systemMessages = mutableListOf<String>()
        val overlayMessages = mutableListOf<String>()
        val dispatcher = NoticeDispatcher(systemMessages::add, overlayMessages::add)

        dispatcher.show("Подсчёт завершён")

        assertEquals(listOf("Подсчёт завершён"), systemMessages)
        assertEquals(listOf("Подсчёт завершён"), overlayMessages)
    }
}
