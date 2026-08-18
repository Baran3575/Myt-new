package com.myt.player

import android.app.Application

class MytApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppState.init(this)
    }
}