package com.stark.memorygame.model

import com.google.firebase.firestore.PropertyName

data class UserCustomGameImages(
    @PropertyName("images") val images: List<String>? = null
)
