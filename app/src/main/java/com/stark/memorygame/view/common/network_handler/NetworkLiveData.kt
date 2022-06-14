package com.stark.memorygame.view.common.network_handler

import androidx.lifecycle.LiveData
import com.stark.memorygame.view.common.network_handler.NetworkStatus

object NetworkLiveData : LiveData<NetworkStatus>() {

    fun setNetworkStatus(status: NetworkStatus) {
        this.value = status
    }
}