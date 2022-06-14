package com.stark.memorygame.model

import com.google.firebase.remoteconfig.FirebaseRemoteConfig

enum class BoardSize(val pairs: Int) {
    EASY(8), MEDIUM(18), HARD(24);

    companion object {
        fun getBoardSizeValue(size: Int) = values().first { it.pairs == size }
    }

    fun getWidth(): Int {
        return when (this) {
            EASY -> 2
            MEDIUM -> 3
            HARD -> 4
        }
    }

    fun getHeight() = pairs / getWidth()

    fun getTotalPairs() = pairs / 2
}