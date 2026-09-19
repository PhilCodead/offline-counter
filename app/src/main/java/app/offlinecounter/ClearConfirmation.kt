package app.offlinecounter

class ClearConfirmation(private val windowMs: Long) {
    private var deadline = 0L

    fun confirm(now: Long): Boolean {
        if (now > deadline) {
            deadline = now + windowMs
            return false
        }
        deadline = 0L
        return true
    }
}
