package com.stark.memorygame.di.module

import androidx.lifecycle.ViewModel
import com.stark.memorygame.di.ViewModelKey
import com.stark.memorygame.view.screens.custom_game.CustomGameViewModel
import com.stark.memorygame.view.screens.main.MainViewModel
import com.stark.memorygame.view.screens.registration.RegistrationViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindMemoryGameViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CustomGameViewModel::class)
    abstract fun bindCustomGameViewModel(viewModel: CustomGameViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RegistrationViewModel::class)
    abstract fun bindRegistrationViewModel(viewModel: RegistrationViewModel): ViewModel

}