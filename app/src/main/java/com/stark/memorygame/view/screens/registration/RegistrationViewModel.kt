package com.stark.memorygame.view.screens.registration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.auth.User
import com.stark.memorygame.data.UserDataSource
import com.stark.memorygame.view.intent.RegistrationIntent
import com.stark.memorygame.view.state.UserRegistrationState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class RegistrationViewModel @Inject constructor(
    private val userDataSource: UserDataSource
) : ViewModel() {

    val userIntent = Channel<RegistrationIntent>(Channel.UNLIMITED)

    private val _userRegisterState: MutableLiveData<UserRegistrationState> = MutableLiveData()
    val userRegistrationState: LiveData<UserRegistrationState> = _userRegisterState

    init {
        viewModelScope.launch {
            handleIntent()
        }
    }

    private suspend fun handleIntent() {
        userIntent.consumeAsFlow().collect {
            when (it) {
                is RegistrationIntent.OnCreateAccount -> {
                    validateUserName(it.userName, it.password)
                }
            }
        }
    }

    private suspend fun validateUserName(userName: String, password: String) {
        _userRegisterState.value = if (userName.length < MIN_USERNAME_LENGTH) {
            UserRegistrationState.OnRegistrationValidationFailure(FieldError.USERNAME_ERROR)
        } else if (password.length < MIN_PASSWORD_LENGTH) {
            UserRegistrationState.OnRegistrationValidationFailure(FieldError.PASSWORD_ERROR)
        } else {
            userDataSource.setUserName(userName)
            UserRegistrationState.OnRegistrationValidationSuccess(userName, password)
        }
    }

    companion object {
        private const val MIN_USERNAME_LENGTH = 4
        private const val MIN_PASSWORD_LENGTH = 5
    }
}
enum class FieldError {
    PASSWORD_ERROR, USERNAME_ERROR,
}