package com.example.offlinecounter

import android.app.Application
import com.google.android.material.color.DynamicColors

class OfflineCounterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
