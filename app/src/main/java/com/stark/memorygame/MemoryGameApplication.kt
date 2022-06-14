package com.stark.memorygame

import android.app.Application
import com.stark.memorygame.di.app.AppComponent
import com.stark.memorygame.di.app.DaggerAppComponent

class MemoryGameApplication : Application() {

    val appComponent: AppComponent by lazy {
        DaggerAppComponent.factory().create(this)
    }

    // problems
    // TODO: after custom game created size is shrinking of views.

    // features
    // TODO: add firebase crashlytic to app
    // TODO: add firebase analytics to app
    // TODO: add authentication so that user can see all his boards
    // TODO: add ability to see all the boards creating by users.
    // TODO: Internet availability check
}