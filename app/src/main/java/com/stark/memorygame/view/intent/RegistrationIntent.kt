package com.stark.memorygame.view.intent

sealed class RegistrationIntent {
    data class OnCreateAccount(val userName: String, val password: String): RegistrationIntent()
}