package com.stark.memorygame.view.screens.registration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                    validateUserName(it.userName)
                }
            }
        }
    }

    private suspend fun validateUserName(userName: String) {
        _userRegisterState.value = if (userName.length < MAX_USERNAME_LENGTH) {
            UserRegistrationState.OnRegistrationValidationFailure
        } else {
            userDataSource.setUserName(userName)
            UserRegistrationState.OnRegistrationValidationSuccess(userName)
        }
    }

    companion object {
        private const val MAX_USERNAME_LENGTH = 4
    }
}