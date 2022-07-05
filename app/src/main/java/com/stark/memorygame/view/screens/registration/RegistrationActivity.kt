package com.stark.memorygame.view.screens.registration

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stark.memorygame.R
import com.stark.memorygame.model.User
import com.stark.memorygame.view.extensions.createToast
import com.stark.memorygame.view.extensions.launchWithLifecycle
import com.stark.memorygame.view.intent.RegistrationIntent
import com.stark.memorygame.view.screens.base.BaseActivity
import com.stark.memorygame.view.screens.main.MainActivity
import com.stark.memorygame.view.state.UserRegistrationState
import com.stark.memorygame.view.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import javax.inject.Inject

class RegistrationActivity : BaseActivity() {

    private lateinit var createBtn: Button
    private lateinit var userNameEt: EditText
    private lateinit var pb: ProgressBar
    private lateinit var vm: RegistrationViewModel

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.activityComponent().create().inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        createBtn = findViewById(R.id.create_account_btn)
        userNameEt = findViewById(R.id.username_et)
        pb = findViewById(R.id.new_user_pb)
        vm = ViewModelProvider(this, viewModelFactory)[RegistrationViewModel::class.java]
        observers()
        createBtn.setOnClickListener {
            val userName = userNameEt.text.toString()
            closeKeyboard()
            it.isEnabled = false
            lifecycleScope.launch {
                vm.userIntent.send(RegistrationIntent.OnCreateAccount(userName))
            }
        }
    }

    private fun closeKeyboard() {
        val inputManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun observers() {
        vm.userRegistrationState.observe(this@RegistrationActivity) { state ->
            when (state) {

                is UserRegistrationState.Idle -> {}

                is UserRegistrationState.OnRegistrationValidationFailure -> {
                    userNameEt.error = getString(R.string.username_validation_error)
                    createBtn.isEnabled = true
                }

                is UserRegistrationState.OnRegistrationValidationSuccess -> {
                    pb.visibility = View.VISIBLE
                    try {
                        checkAndSaveUserName(state.name)
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

    private fun checkAndSaveUserName(name: String) {
        db.collection(USER_COLLECTION).document(name).get().addOnSuccessListener { account ->
           if (account != null && account.data != null) {
                Log.i(TAG, "isUserNameAlreadyTaken")
                userNameEt.error = getString(R.string.username_conflict)
                resetElements()
            } else {
                saveUserName(name)
            }
        }.addOnFailureListener {
            Log.i(TAG, "Duplicate UserName Error: ${it}")
            resetElements()
            createToast(getString(R.string.server_error)).show()
        }
    }

    private fun saveUserName(userName: String) {
        val user = User(username = userName)
        db.collection(USER_COLLECTION).document(userName).set(user)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.i(TAG, "saveUserName failed: ${task.exception} ")
                    createToast(getString(R.string.user_account_failure_message)).show()
                    resetElements()
                    // TODO: send error to analytics and crashlytics when account didn't created
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