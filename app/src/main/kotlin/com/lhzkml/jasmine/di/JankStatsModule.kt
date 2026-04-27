package com.lhzkml.jasmine.di

import android.app.Activity
import com.lhzkml.jasmine.core.data.log.FileLogger
import android.view.Window
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.JankStats.OnFrameListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
object JankStatsModule {
    @Provides
    fun providesOnFrameListener(): OnFrameListener {
        var lastLogTime = 0L
        val minIntervalMs = 500L
        return OnFrameListener { frameData ->
            if (frameData.isJank) {
                val now = System.currentTimeMillis()
                if (now - lastLogTime >= minIntervalMs) {
                    lastLogTime = now
                    FileLogger.log("Jank", frameData.toString())
                }
            }
        }
    }

    @Provides
    fun providesWindow(activity: Activity): Window = activity.window

    @Provides
    fun providesJankStats(
        window: Window,
        frameListener: OnFrameListener,
    ): JankStats = JankStats.createAndTrack(window, frameListener)
}
