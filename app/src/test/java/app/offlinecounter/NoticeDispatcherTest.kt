package app.offlinecounter

import org.junit.Assert.assertEquals
import org.junit.Test

class NoticeDispatcherTest {
    @Test
    fun successfulOverlayDoesNotDuplicateSystemToast() {
        val systemMessages = mutableListOf<String>()
        val overlayMessages = mutableListOf<String>()
        val dispatcher = NoticeDispatcher(systemMessages::add, overlayMessages::add)

        dispatcher.show("Подсчёт завершён")

        assertEquals(emptyList<String>(), systemMessages)
        assertEquals(listOf("Подсчёт завершён"), overlayMessages)
    }

    @Test
    fun failedOverlayFallsBackToSystemToast() {
        val messages = mutableListOf<String>()
        val dispatcher = NoticeDispatcher(messages::add) { error("Window unavailable") }

        dispatcher.show("Файл сохранён")

        assertEquals(listOf("Файл сохранён"), messages)
    }
}
