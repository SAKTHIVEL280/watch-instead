package com.watchinstead.app

import android.app.Application
import com.watchinstead.app.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WatchInsteadApplication : Application() {
    @Inject lateinit var widgetUpdater: WidgetUpdater

    override fun onCreate() {
        super.onCreate()
        widgetUpdater.start()
    }
}
