package com.stark.memorygame.view.screens.custom_game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stark.memorygame.view.intent.CustomGameIntent
import com.stark.memorygame.view.state.CustomGameState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class CustomGameViewModel @Inject constructor() : ViewModel() {

    private val customGameIntent = Channel<CustomGameIntent>(Channel.UNLIMITED)

    private val _customGameState: MutableStateFlow<CustomGameState> = MutableStateFlow(CustomGameState.Idle)
    val customGameState: StateFlow<CustomGameState> = _customGameState.asStateFlow()

    init {
        handleIntent()
    }

    private fun handleIntent() {
        viewModelScope.launch {
            customGameIntent.receiveAsFlow().collect {
                when(it) {
                    is CustomGameIntent.OnSaveCustomGame -> {
                        val state = if (!it.gameName.isNullOrBlank()) {
                            CustomGameState.SaveGame(it.gameName)
                        } else {
                            CustomGameState.Error("Game name can't be empty")
                        }
                        _customGameState.emit(state)
                    }

                    is CustomGameIntent.None -> {}
                }
            }
        }
    }
}