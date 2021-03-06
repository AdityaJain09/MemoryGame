package com.stark.memorygame.model

import androidx.annotation.DrawableRes

data class MemoryCard(
    val identifier: Int,
    var imageUrl : String? = null,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)