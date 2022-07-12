package com.stark.memorygame.view.state

import com.stark.memorygame.model.MemoryCard

sealed class MemoryCardGameState {
    object Idle : MemoryCardGameState()
    object Loading : MemoryCardGameState()
    object OnFetchingUsername: MemoryCardGameState()
    data class OnGameReset(val cards: List<MemoryCard>): MemoryCardGameState()
    data class CardStateChange(val position: Int) : MemoryCardGameState()
    data class Error (val error: String?): MemoryCardGameState()
}

sealed class GameStatus {
    object Idle: GameStatus()
    data class MovesAndPairs(val moves: Int, val pairs: Int ): GameStatus()
}