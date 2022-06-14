package com.stark.memorygame.di.module

import androidx.lifecycle.ViewModel
import com.stark.memorygame.di.ViewModelKey
import com.stark.memorygame.view.screens.custom_game.CustomGameViewModel
import com.stark.memorygame.view.screens.main.MemoryGameViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(MemoryGameViewModel::class)
    abstract fun bindMemoryGameViewModel(memoryGameViewModel: MemoryGameViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CustomGameViewModel::class)
    abstract fun bindCustomGameViewModel(customGameViewModel: CustomGameViewModel): ViewModel

}