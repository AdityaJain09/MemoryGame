package com.stark.memorygame.model

import com.google.firebase.firestore.PropertyName
import java.util.*

data class User(
    @PropertyName("uid") val userId: String = UUID.randomUUID().toString(),
    @PropertyName("username") val username: String? = null,
    @PropertyName("password") val password: String? = null,
    @PropertyName("salt") val salt: String? = null,
    @PropertyName("key") val key: String? = null
)