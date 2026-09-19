package app.offlinecounter

class ClearConfirmation(private val windowMs: Long) {
    private var deadline: Long? = null

    fun confirm(now: Long): Boolean {
        val currentDeadline = deadline
        if (currentDeadline == null || now > currentDeadline) {
            deadline = now + windowMs
            return false
        }
        deadline = null
        return true
    }
}
