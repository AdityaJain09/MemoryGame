package com.stark.memorygame.view.state

sealed class CustomGameState {
    object Idle: CustomGameState()
    data class SaveGame(val name: String): CustomGameState()
    data class Error(val error: String?): CustomGameState()
}