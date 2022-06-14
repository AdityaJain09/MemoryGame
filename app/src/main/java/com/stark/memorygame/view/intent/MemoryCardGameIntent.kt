package com.stark.memorygame.view.intent

sealed class MemoryCardGameIntent {
    data class OnGameCardClick(val position: Int) : MemoryCardGameIntent()
    object OnRefresh : MemoryCardGameIntent()
    object None: MemoryCardGameIntent()
}

sealed class CustomGameIntent {
    object None: CustomGameIntent()
    data class OnSaveCustomGame(val gameName: String?): CustomGameIntent()
}