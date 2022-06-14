package com.stark.memorygame.view.extensions

import android.app.Activity
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar


fun View.showSnackBar(message: String, duration: Int = Snackbar.LENGTH_SHORT): Snackbar {
    return Snackbar.make(this, message, duration)
}

fun Activity.createToast(message: String, length: Int = Toast.LENGTH_LONG): Toast {
    return Toast.makeText(this, message, length)
}