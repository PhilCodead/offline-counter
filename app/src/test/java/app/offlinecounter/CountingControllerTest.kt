package app.offlinecounter

import org.junit.Assert.*
import org.junit.Test

class CountingControllerTest {
    private fun people(count: Int) = List(count) {
        Person("Человек $it", "01.01.1990", if (it % 2 == 0) "Ж" else "М", "")
    }

    @Test
    fun repeatedRunsFromEveryStartingPageCountTheEntireOverlappingList() {
        val controller = CountingController()
        val all = people(37)
        for (startPage in 0..16) {
            repeat(5) {
                var offset = startPage * 2
                controller.start()
                var command: CountingCommand = CountingCommand.Wait
                var cycles = 0
                while (command != CountingCommand.Complete && cycles++ < 200) {
                    val visible = all.drop(offset).take(5)
                    command = controller.onFrame(FrameSnapshot(visible, offset))
                    command = when (command) {
                        CountingCommand.ScrollBackward -> {
                            val moved = offset > 0
                            offset = (offset - 2).coerceAtLeast(0)
                            controller.onScrollResult(moved)
                        }
                        CountingCommand.ScrollForward -> {
                            val moved = offset + 5 < all.size
                            offset = (offset + 2).coerceAtMost(all.size - 5)
                            controller.onScrollResult(moved)
                        }
                        else -> command
                    }
                }
                assertEquals("start page $startPage", CountingCommand.Complete, command)
                assertEquals(all, controller.people)
                assertEquals(37, controller.state.total)
                assertEquals(19, controller.state.women)
                assertEquals(18, controller.state.men)
            }
        }
    }

    @Test
    fun pendingScrollCannotBeOvertakenByAnotherFrame() {
        val controller = CountingController()
        controller.start()
        assertEquals(CountingCommand.ScrollBackward, controller.onFrame(FrameSnapshot(emptyList(), 1)))
        repeat(10) {
            assertEquals(CountingCommand.Wait, controller.onFrame(FrameSnapshot(emptyList(), 2)))
        }
        assertEquals(CountingPhase.Rewinding, controller.state.phase)
        controller.onScrollResult(false)
        assertEquals(CountingPhase.Collecting, controller.state.phase)
    }

    @Test
    fun stationaryRewindAndForwardUseVersion12Thresholds() {
        val controller = CountingController()
        controller.start()
        repeat(2) {
            assertEquals(CountingCommand.ScrollBackward, controller.onFrame(FrameSnapshot(emptyList(), 7)))
            controller.onScrollResult(true)
        }
        assertEquals(CountingCommand.Wait, controller.onFrame(FrameSnapshot(emptyList(), 7)))
        assertEquals(CountingPhase.Collecting, controller.state.phase)
        val frame = FrameSnapshot(people(3), 7)
        repeat(2) {
            assertEquals(CountingCommand.ScrollForward, controller.onFrame(frame))
            assertEquals(CountingCommand.Wait, controller.onScrollResult(true))
        }
        controller.onFrame(frame)
        assertEquals(CountingCommand.Complete, controller.onScrollResult(true))
        assertEquals(3, controller.state.total)
    }

    @Test
    fun finalScreenIsCollectedBeforeRejectedScrollCompletes() {
        val controller = CountingController()
        controller.start()
        controller.onFrame(FrameSnapshot(emptyList(), 0))
        controller.onScrollResult(false)
        val rows = people(3)
        controller.onFrame(FrameSnapshot(rows + rows, 1))
        assertEquals(CountingCommand.Complete, controller.onScrollResult(false))
        assertEquals(rows, controller.people)
    }

    @Test
    fun stopAndClearRejectLateScrollResults() {
        val controller = CountingController()
        controller.start()
        controller.onFrame(FrameSnapshot(emptyList(), 0))
        controller.stop()
        assertEquals(CountingCommand.Wait, controller.onScrollResult(false))
        assertEquals(CountingPhase.Idle, controller.state.phase)
        controller.clear()
        assertEquals(0, controller.state.total)
        assertTrue(controller.people.isEmpty())
    }

    @Test
    fun unavailableSourceDoesNotClaimSuccess() {
        val controller = CountingController()
        controller.start()
        repeat(20) { controller.onSourceUnavailable() }
        assertEquals(CountingPhase.Error, controller.state.phase)
    }

    @Test
    fun rewindLimitReportsIncompleteInsteadOfCountingFromMiddle() {
        val controller = CountingController()
        controller.start()
        repeat(400) {
            controller.onFrame(FrameSnapshot(emptyList(), it))
            controller.onScrollResult(true)
        }
        assertTrue(controller.onFrame(FrameSnapshot(emptyList(), 401)) is CountingCommand.Fail)
        assertEquals(CountingPhase.Error, controller.state.phase)
    }
    @Test
    fun scrollingPastUnrecognizedScreensDoesNotSilentlyDropLaterPatients() {
        val controller = CountingController()
        controller.start()
        controller.onFrame(FrameSnapshot(emptyList(), 0))
        controller.onScrollResult(false)
        controller.onFrame(FrameSnapshot(listOf(Person("Первая Пациентка", "01.01.1990", "Ж", "")), 1))
        assertEquals(CountingCommand.Wait, controller.onScrollResult(true))
        repeat(6) { page ->
            assertEquals(CountingCommand.ScrollForward, controller.onFrame(FrameSnapshot(emptyList(), page + 2)))
            assertEquals(CountingCommand.Wait, controller.onScrollResult(true))
        }
        controller.onFrame(FrameSnapshot(listOf(Person("Второй Пациент", "02.02.1990", "М", "")), 8))
        assertEquals(CountingCommand.Complete, controller.onScrollResult(false))
        assertEquals(2, controller.state.total)
    }

}
