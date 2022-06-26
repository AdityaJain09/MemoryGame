package com.stark.memorygame.view.screens.base

import android.content.IntentFilter
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.ParametersBuilder
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.stark.memorygame.MemoryGameApplication
import com.stark.memorygame.utils.FirebaseConstants
import com.stark.memorygame.utils.FirebaseConstants.USER_COUNTRY
import com.stark.memorygame.utils.FirebaseConstants.USER_LANGUAGE
import com.stark.memorygame.view.common.network_handler.NetworkBroadCastReceiver
import com.stark.memorygame.view.common.network_handler.NetworkLiveData
import com.stark.memorygame.view.common.network_handler.NetworkStatus
import com.stark.memorygame.view.extensions.createToast
import com.stark.memorygame.view.extensions.launchWithLifecycle
import com.stark.memorygame.view.screens.main.MainActivity
import com.stark.memorygame.view.viewmodel.ViewModelFactory
import java.util.*
import javax.inject.Inject

open class BaseActivity : AppCompatActivity() {
    private lateinit var networkBroadCastReceiver: NetworkBroadCastReceiver
    val appComponent get() = (application as MemoryGameApplication).appComponent
    protected var isNetworkAvailable: Boolean = false

    protected val db by lazy { Firebase.firestore }
    protected val remoteConfig by lazy { Firebase.remoteConfig }
    protected val firebaseAnalytics by lazy { Firebase.analytics }

    protected companion object {
        private const val TAG = "BaseActivity"
        const val DATABASE_NAME = "memory"
        const val ROOT_COLLECTION_NAME = "images"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        remoteConfig.setDefaultsAsync(
            mapOf(
                "about_link" to "https://www.instagram.com/shahaditya62/",
                "scaled_height" to 250L,
                "compress_quality" to 60L
            )
        )
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "Fetch/activate succeeded, did config get updated? ${task.result}")
                } else {
                    Log.w(TAG, "Remote config fetch failed")
                }
            }
        if (!::networkBroadCastReceiver.isInitialized) {
            registerNetworkReceiver()
        }
        observer()
    }

    private fun observer() {
        launchWithLifecycle {
            NetworkLiveData.observe(this@BaseActivity) { status ->
                when (status) {
                    NetworkStatus.CONNECTED -> {
                        isNetworkAvailable = true
                    }
                    NetworkStatus.NOT_CONNECTED -> {
                        isNetworkAvailable = false
                    }
                    else -> createToast("Network Issue").show()
                }
            }
        }
    }

    private fun registerNetworkReceiver() {
        val connectivityIntent = IntentFilter()
        connectivityIntent.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        networkBroadCastReceiver = NetworkBroadCastReceiver()
        registerReceiver(networkBroadCastReceiver, connectivityIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(networkBroadCastReceiver)
    }

}