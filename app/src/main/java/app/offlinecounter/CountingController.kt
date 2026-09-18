package app.offlinecounter

import java.util.LinkedHashMap

class CountingController {
    private val collected = LinkedHashMap<String, Person>()
    private var stalePasses = 0
    private var rewindSteps = 0
    private var scrollSteps = 0
    private var unavailableTicks = 0

    var state = OverlayUiState()
        private set

    val people: List<Person>
        get() = collected.values.toList()

    fun start(): CountingCommand {
        collected.clear()
        stalePasses = 0
        rewindSteps = 0
        scrollSteps = 0
        unavailableTicks = 0
        state = OverlayUiState(phase = CountingPhase.Rewinding, status = "Возврат к началу списка…", expanded = false)
        return CountingCommand.ScrollBackward
    }

    fun onFrame(frame: FrameSnapshot): CountingCommand = when (state.phase) {
        CountingPhase.Rewinding, CountingPhase.Collecting -> {
            unavailableTicks = 0
            if (state.phase == CountingPhase.Rewinding) rewind(frame) else collect(frame)
        }
        else -> CountingCommand.Wait
    }

    fun stop() {
        state = state.copy(phase = CountingPhase.Idle, status = "Остановлено")
    }

    fun clear() {
        collected.clear()
        stalePasses = 0
        state = OverlayUiState(status = "Очищено")
    }

    fun setExpanded(expanded: Boolean) {
        state = state.copy(expanded = expanded)
    }

    fun onSourceUnavailable(): CountingCommand {
        unavailableTicks++
        if (unavailableTicks < MAX_UNAVAILABLE_TICKS) return CountingCommand.Wait
        val message = "Список недоступен — попробуйте снова"
        state = state.copy(phase = CountingPhase.Error, status = message, expanded = true)
        return CountingCommand.Fail(message)
    }

    private fun rewind(frame: FrameSnapshot): CountingCommand {
        if (frame.atTop || rewindSteps >= MAX_REWIND_STEPS) {
            state = state.copy(phase = CountingPhase.Collecting, status = "Сбор данных…")
            return CountingCommand.Wait
        }
        rewindSteps++
        return CountingCommand.ScrollBackward
    }

    private fun collect(frame: FrameSnapshot): CountingCommand {
        val previousSize = collected.size
        frame.people.forEach { collected.putIfAbsent(it.key, it) }
        stalePasses = if (collected.size == previousSize) stalePasses + 1 else 0
        updateCounts()

        if (frame.atBottom || stalePasses >= MAX_STALE_PASSES || scrollSteps >= MAX_SCROLL_STEPS) {
            state = state.copy(phase = CountingPhase.Completed, status = "Готово")
            return CountingCommand.Complete
        }
        scrollSteps++
        return CountingCommand.ScrollForward
    }

    private fun updateCounts() {
        state = state.copy(
            total = collected.size,
            women = collected.values.count { it.sex == "Ж" },
            men = collected.values.count { it.sex == "М" },
        )
    }

    private companion object {
        const val MAX_REWIND_STEPS = 350
        const val MAX_SCROLL_STEPS = 1000
        const val MAX_STALE_PASSES = 15
        const val MAX_UNAVAILABLE_TICKS = 20
    }
}
