package com.stark.memorygame.model

import com.google.firebase.firestore.PropertyName

data class UserCustomGameImages(
    @PropertyName("images") val images: List<String>? = null,
    @PropertyName("creator") val creator: String? = null,
    @PropertyName("gameType") val gameType: String? = null,
    @PropertyName("taggedUsers") val taggedUsers: List<String>? = null,
    @PropertyName("shareType") val shareType: String? = null,
)
