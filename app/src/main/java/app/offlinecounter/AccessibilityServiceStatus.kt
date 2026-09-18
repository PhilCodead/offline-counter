package app.offlinecounter

object AccessibilityServiceStatus {
    fun contains(enabled: String?, packageName: String, className: String): Boolean {
        return enabled.orEmpty().split(':').any { value ->
            val parts = value.split('/', limit = 2)
            if (parts.size != 2) return@any false
            val actualClass = if (parts[1].startsWith('.')) parts[0] + parts[1] else parts[1]
            parts[0] == packageName && actualClass == className
        }
    }
}
