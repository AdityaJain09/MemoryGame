package com.stark.memorygame.view.state

sealed class UserRegistrationState {
    object Idle: UserRegistrationState()
    data class OnRegistrationValidationSuccess(val name: String): UserRegistrationState()
    object OnRegistrationValidationFailure: UserRegistrationState()
}