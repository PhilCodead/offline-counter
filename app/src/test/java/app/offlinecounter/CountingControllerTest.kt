package app.offlinecounter

import org.junit.Assert.assertEquals
import org.junit.Test

class CountingControllerTest {
    private val controller = CountingController()

    @Test
    fun startRewindsThenCollectsAtTop() {
        assertEquals(CountingCommand.ScrollBackward, controller.start())

        val command = controller.onFrame(frame(atTop = true, fingerprint = 1))

        assertEquals(CountingCommand.Wait, command)
        assertEquals(CountingPhase.Collecting, controller.state.phase)
    }

    @Test
    fun collectionDeduplicatesAndCompletesAtBottom() {
        controller.start()
        controller.onFrame(frame(atTop = true, fingerprint = 1))
        val person = Person("Анна Смирнова", "01.01.1990", "Ж", "")

        val command = controller.onFrame(
            frame(people = listOf(person, person), atBottom = true, fingerprint = 2),
        )

        assertEquals(CountingCommand.Complete, command)
        assertEquals(1, controller.state.total)
        assertEquals(1, controller.state.women)
        assertEquals(CountingPhase.Completed, controller.state.phase)
    }

    @Test
    fun stopKeepsResultsWhileClearRemovesThem() {
        val person = Person("Иван Петров", "02.02.1980", "М", "")
        controller.start()
        controller.onFrame(frame(atTop = true, fingerprint = 1))
        controller.onFrame(frame(listOf(person), fingerprint = 2))

        controller.stop()

        assertEquals(CountingPhase.Idle, controller.state.phase)
        assertEquals(1, controller.state.total)

        controller.clear()

        assertEquals(0, controller.state.total)
        assertEquals(0, controller.people.size)
    }

    @Test
    fun unchangedViewportDoesNotFinishBeforeTheListBottom() {
        controller.start()
        controller.onFrame(frame(atTop = true, fingerprint = 1))

        val firstPerson = Person("Анна Смирнова", "01.01.1990", "Ж", "")

        var command: CountingCommand = CountingCommand.Wait
        repeat(20) {
            command = controller.onFrame(frame(people = listOf(firstPerson), fingerprint = 2))
        }

        assertEquals(CountingCommand.ScrollForward, command)
        assertEquals(CountingPhase.Collecting, controller.state.phase)
        assertEquals(1, controller.state.total)
    }

    @Test
    fun unchangedViewportWaitsForRenderingBeforeRetryingTheScroll() {
        controller.start()
        controller.onFrame(frame(atTop = true, fingerprint = 1))

        assertEquals(CountingCommand.ScrollForward, controller.onFrame(frame(fingerprint = 2)))
        repeat(5) {
            assertEquals(CountingCommand.Wait, controller.onFrame(frame(fingerprint = 2)))
        }
        assertEquals(CountingCommand.ScrollForward, controller.onFrame(frame(fingerprint = 2)))
    }

    @Test
    fun repeatedUnavailableFramesEndWithRecoverableError() {
        controller.start()
        repeat(20) { controller.onSourceUnavailable() }
        assertEquals(CountingPhase.Error, controller.state.phase)
    }

    @Test
    fun startCollapsesExpandedPanel() {
        controller.setExpanded(true)
        controller.start()
        assertEquals(false, controller.state.expanded)
    }

    private fun frame(
        people: List<Person> = emptyList(),
        atTop: Boolean = false,
        atBottom: Boolean = false,
        fingerprint: Int = 0,
    ) = FrameSnapshot(people, atTop, atBottom, fingerprint)
}
