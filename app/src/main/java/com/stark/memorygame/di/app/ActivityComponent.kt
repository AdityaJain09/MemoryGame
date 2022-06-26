package com.stark.memorygame.di.app

import android.app.Application
import com.stark.memorygame.di.module.ViewModelModule
import com.stark.memorygame.di.scopes.ActivityScope
import com.stark.memorygame.view.screens.custom_game.CustomGameActivity
import com.stark.memorygame.view.screens.main.MainActivity
import com.stark.memorygame.view.screens.registration.RegistrationActivity
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent(
    modules = [ViewModelModule::class]
)
@ActivityScope
interface ActivityComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(): ActivityComponent
    }

    fun inject(activity: MainActivity)
    fun inject(activity: CustomGameActivity)
    fun inject(activity: RegistrationActivity)
}