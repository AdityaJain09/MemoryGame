package com.stark.memorygame.model

enum class BoardSize(val pairs: Int) {
    EASY(8), MEDIUM(18), HARD(24), VERY_HARD(28);

    companion object {
        fun getBoardSizeValue(size: Int) = values().first { it.pairs == size }
    }

    fun getWidth(): Int {
        return when (this) {
            EASY -> 2
            MEDIUM -> 3
            HARD -> 4
            else -> 4
        }
    }

    fun getHeight() = pairs / getWidth()

    fun getTotalPairs() = pairs / 2

    fun getTimerInMillis(): Long {
        return when(getTotalPairs()) {
            4 -> 15_000L
            9 -> 30_000L
            12 -> 55_000L
            14 -> 85_000L
            else -> throw Exception("Timer can't be set")
        }
    }
}