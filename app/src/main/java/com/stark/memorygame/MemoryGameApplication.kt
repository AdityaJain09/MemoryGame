package com.stark.memorygame

import android.app.Application
import com.stark.memorygame.di.app.AppComponent
import com.stark.memorygame.di.app.DaggerAppComponent

class MemoryGameApplication : Application() {

    val appComponent: AppComponent by lazy {
        DaggerAppComponent.factory().create(this)
    }
}
