package app.offlinecounter

data class AccessibilitySettingsRequest(
    val detailsAction: String,
    val componentName: String,
    val fallbackAction: String,
) {
    companion object {
        fun forService(packageName: String, serviceClassName: String) = AccessibilitySettingsRequest(
            detailsAction = "android.settings.ACCESSIBILITY_DETAILS_SETTINGS",
            componentName = "$packageName/$serviceClassName",
            fallbackAction = "android.settings.ACCESSIBILITY_SETTINGS",
        )
    }
}
