package com.stark.memorygame.view.common.network_handler

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build


enum class NetworkStatus {
    CONNECTED, NOT_CONNECTED
}

class NetworkBroadCastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        NetworkLiveData.setNetworkStatus(
            if (isNetworkAvailable(context))
                NetworkStatus.CONNECTED
            else
                NetworkStatus.NOT_CONNECTED
        )
    }


    private fun isNetworkAvailable(context: Context?): Boolean {
        val connectivityManager = (context?.applicationContext)?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
             when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            connectivityManager.activeNetworkInfo?.isAvailable ?: return false
        }
    }
}