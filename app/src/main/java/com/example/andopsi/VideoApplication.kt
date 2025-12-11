package com.example.andopsi

import android.app.Application
import com.example.andopsi.data.AppContainer
import com.example.andopsi.data.DefaultAppContainer

class VideoApplication : Application() {
    /** AppContainer instance used by the rest of classes to obtain dependencies */
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)
    }
}
