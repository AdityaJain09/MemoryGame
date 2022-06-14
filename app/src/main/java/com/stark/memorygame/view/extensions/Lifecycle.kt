package com.stark.memorygame.view.extensions

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun LifecycleOwner.launchWithRepeatOnLifecycle(block: CoroutineScope.() -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            block()
        }
    }
}

fun LifecycleOwner.launchWithLifecycle(block: CoroutineScope.() -> Unit) {
    lifecycleScope.launch {
            block()
        }
}