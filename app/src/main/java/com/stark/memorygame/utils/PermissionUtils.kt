package com.stark.memorygame.utils

import android.app.Activity
import android.content.Context
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.stark.memorygame.view.screens.main.MainActivity


fun hasPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED
}