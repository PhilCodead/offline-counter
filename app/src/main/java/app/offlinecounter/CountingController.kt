package app.offlinecounter

class CountingController {
    private val collected = LinkedHashMap<String, Person>()
    private var rewindSteps = 0
    private var scrollSteps = 0
    private var unavailableTicks = 0
    private var lastFingerprint: Int? = null
    private var sameScreen = 0
    private var pendingScroll: CountingCommand? = null

    var state = OverlayUiState()
        private set

    val people: List<Person>
        get() = collected.values.toList()

    fun start(): CountingCommand {
        collected.clear()
        rewindSteps = 0
        scrollSteps = 0
        unavailableTicks = 0
        resetProgress()
        state = OverlayUiState(phase = CountingPhase.Rewinding, status = "Возврат к началу списка…")
        return CountingCommand.Wait
    }

    fun onFrame(frame: FrameSnapshot): CountingCommand {
        if (state.phase != CountingPhase.Rewinding && state.phase != CountingPhase.Collecting) {
            return CountingCommand.Wait
        }
        if (pendingScroll != null) return CountingCommand.Wait
        unavailableTicks = 0
        sameScreen = if (frame.fingerprint == lastFingerprint) sameScreen + 1 else 0
        lastFingerprint = frame.fingerprint
        if (state.phase == CountingPhase.Rewinding) {
            if (sameScreen >= 2) return beginCollection()
            if (rewindSteps >= 400) return fail("Не удалось вернуться к началу списка")
            rewindSteps++
            pendingScroll = CountingCommand.ScrollBackward
            return CountingCommand.ScrollBackward
        }
        frame.people.forEach { collected.putIfAbsent(it.key, it) }
        state = state.copy(
            total = collected.size,
            women = collected.values.count { it.sex == "Ж" },
            men = collected.values.count { it.sex == "М" },
        )
        if (scrollSteps >= 600) return fail("Достигнут предел прокрутки — результат неполный")
        scrollSteps++
        pendingScroll = CountingCommand.ScrollForward
        return CountingCommand.ScrollForward
    }

    fun onScrollResult(scrolled: Boolean): CountingCommand {
        val command = pendingScroll ?: return CountingCommand.Wait
        pendingScroll = null
        if (command == CountingCommand.ScrollBackward) {
            return if (scrolled) CountingCommand.Wait else beginCollection()
        }
        if (!scrolled || sameScreen >= 2) {
            state = state.copy(phase = CountingPhase.Completed, status = "Готово")
            return CountingCommand.Complete
        }
        return CountingCommand.Wait
    }

    fun stop() {
        pendingScroll = null
        state = state.copy(phase = CountingPhase.Idle, status = "Остановлено")
    }

    fun clear() {
        collected.clear()
        resetProgress()
        state = OverlayUiState(status = "Очищено")
    }

    fun onSourceUnavailable(): CountingCommand {
        unavailableTicks++
        return if (unavailableTicks < 20) CountingCommand.Wait
        else fail("Список недоступен — попробуйте снова")
    }

    private fun beginCollection(): CountingCommand {
        resetProgress()
        state = state.copy(phase = CountingPhase.Collecting, status = "Сбор данных…")
        return CountingCommand.Wait
    }

    private fun resetProgress() {
        lastFingerprint = null
        sameScreen = 0
        pendingScroll = null
    }

    private fun fail(message: String): CountingCommand {
        pendingScroll = null
        state = state.copy(phase = CountingPhase.Error, status = message)
        return CountingCommand.Fail(message)
    }
}
