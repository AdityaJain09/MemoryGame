package com.stark.memorygame.di.module

import androidx.lifecycle.ViewModelProvider
import com.stark.memorygame.view.viewmodel.ViewModelFactory
import dagger.Binds
import dagger.Module

@Module
abstract class ViewModelFactoryModule {
    @Binds
    abstract fun bindViewModelFactory(viewModelFactory: ViewModelFactory): ViewModelProvider.Factory
}