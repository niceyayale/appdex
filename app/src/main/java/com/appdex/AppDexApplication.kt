package com.appdex

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AppDexApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupGlobalExceptionHandler()
    }

    private fun setupGlobalExceptionHandler() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AppDexCrash", "Uncaught exception on ${thread.name}", throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
