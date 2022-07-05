package com.stark.memorygame.view.screens.custom_game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stark.memorygame.data.UserDataSource
import com.stark.memorygame.view.intent.CustomGameIntent
import com.stark.memorygame.view.state.CustomGameState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class CustomGameViewModel @Inject constructor(
    private val userDataSource: UserDataSource
) : ViewModel() {

    var creator: String? = null
        private set

    private val customGameIntent = Channel<CustomGameIntent>(Channel.UNLIMITED)

    private val _customGameState: MutableStateFlow<CustomGameState> = MutableStateFlow(CustomGameState.Idle)
    val customGameState: StateFlow<CustomGameState> = _customGameState.asStateFlow()

    init {
        viewModelScope.launch {
            creator = userDataSource.getUserName().firstOrNull()
            handleIntent()
        }
    }

    private suspend fun handleIntent() {
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

enum class GameSharingState(val option: Int) {
    ONLY_ME(0), SELECTED_USERS(1), EVERYONE(2)
}