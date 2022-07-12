package com.stark.memorygame.view.screens.registration

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stark.memorygame.R
import com.stark.memorygame.model.User
import com.stark.memorygame.view.extensions.createToast
import com.stark.memorygame.view.intent.RegistrationIntent
import com.stark.memorygame.view.screens.base.BaseActivity
import com.stark.memorygame.view.screens.main.MainActivity
import com.stark.memorygame.view.security.Encryption
import com.stark.memorygame.view.security.EncryptionState
import com.stark.memorygame.view.state.UserRegistrationState
import com.stark.memorygame.view.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import javax.inject.Inject

class RegistrationActivity : BaseActivity() {

    private lateinit var createBtn: Button
    private lateinit var userNameEt: EditText
    private lateinit var pb: ProgressBar
    private lateinit var passwordEt: EditText
    private lateinit var vm: RegistrationViewModel

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.activityComponent().create().inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        createBtn = findViewById(R.id.create_account_btn)
        userNameEt = findViewById(R.id.username_et)
        passwordEt = findViewById(R.id.password_et)
        pb = findViewById(R.id.new_user_pb)
        vm = ViewModelProvider(this, viewModelFactory)[RegistrationViewModel::class.java]
        observers()
        createBtn.setOnClickListener {
            val userName = userNameEt.text.toString()
            val password = passwordEt.text.toString()
            closeKeyboard()
            it.isEnabled = false
            lifecycleScope.launch {
                vm.userIntent.send(RegistrationIntent.OnCreateAccount(userName, password))
            }
        }
    }

    private fun closeKeyboard() {
        val inputManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun observers() {
        vm.userRegistrationState.observe(this@RegistrationActivity) { state ->
            when (state) {

                is UserRegistrationState.Idle -> {}

                is UserRegistrationState.OnRegistrationValidationFailure -> {
                    if (state.error == FieldError.USERNAME_ERROR) {
                        userNameEt.error = getString(R.string.username_validation_error)
                    } else {
                        passwordEt.error = getString(R.string.password_error)
                    }
                    createBtn.isEnabled = true
                }

                is UserRegistrationState.OnRegistrationValidationSuccess -> {
                    pb.visibility = View.VISIBLE
                    val encryption: EncryptionState = Encryption.encrypt(state.password)
                    val user = User(
                        username = state.name,
                        password = encryption.encryptedPassword,
                        salt = encryption.iv,
                        key = encryption.secretKey
                    )
                    try {
                        val enteredPassword = passwordEt.text.toString()
                        checkAndSaveUserName(user, enteredPassword)
                    } catch (e: Exception) {
                        resetElements()
                        Log.e(TAG, "user account creation failed = ${e.message}")
                    }
                }
            }
        }
    }

    private fun resetElements() {
        pb.visibility = View.GONE
        createBtn.isEnabled = true
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finishAfterTransition()
    }

    private fun checkAndSaveUserName(user: User, enteredPassword: String) {
        db.collection(USER_COLLECTION).document(user.username!!).get().addOnSuccessListener { doc ->
            if (doc != null && doc.data != null) {
                val oldUser = doc.toObject(User::class.java)
                if (oldUser == null) {
                    Log.i(TAG, "doc to object failed ")
                    return@addOnSuccessListener
                }
                try {
                    if (authenticatedUser(oldUser, enteredPassword)) {
                        createToast("Logged In as ${user.username}").show()
                        navigateToMain()
                    } else {
                        createToast("Username or Password is incorrect").show()
                        resetElements()
                    }
                } catch (e: Exception) {
                    Log.i(TAG, "failed to authenticate user = $e")
                }
            } else {
                try {
                    saveUserName(user)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create user account" )
                    createToast(getString(R.string.user_save_error)).show()
                }
            }
        }
    }

    private fun authenticatedUser(user: User, enteredPassword: String): Boolean {
        val decryptPassword = Encryption.decrypt(EncryptionState(user.salt!!, user.password!!, user.key!!))
        return decryptPassword == enteredPassword
    }

    private fun saveUserName(user: User) {
        db.collection(USER_COLLECTION).document(user.username!!).set(user)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.i(TAG, "saveUserName failed: ${task.exception} ")
                    createToast(getString(R.string.user_account_failure_message)).show()
                    resetElements()
                    task.exception?.let {
                        throw it
                    }
                    return@addOnCompleteListener
                }
                createToast(getString(R.string.user_created_message), Toast.LENGTH_SHORT).show()
                navigateToMain()
            }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.alert_title))
            .setMessage(getString(R.string.cancel_registration_process))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                navigateToMain()
                finishAfterTransition()
            }
            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                dialog.cancel()
            }.show()
    }

    private companion object {
        private const val TAG = "RegistrationActivity"
        private const val USER_COLLECTION = "users"
    }
}