package com.stark.memorygame.view.state

import com.stark.memorygame.view.screens.registration.FieldError

sealed class UserRegistrationState {
    object Idle: UserRegistrationState()
    data class OnRegistrationValidationSuccess(val name: String, val password: String): UserRegistrationState()
    data class OnRegistrationValidationFailure(val error: FieldError): UserRegistrationState()
}