package com.farmvision.agrirover.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interface for binding application socket traffic to the Wi-Fi interface of the rover SoftAP.
 * Overcomes Android's default behavior of routing traffic through cellular data when connected
 * to an AP without upstream internet access.
 */
interface RoverNetworkBinder {
    val isBoundToRoverWifi: StateFlow<Boolean>
    val boundNetworkInfo: StateFlow<String?>
    val activeNetwork: Network? get() = null

    fun startListening()
    fun stopListening()
    fun unbind()
}

/**
 * Android implementation of [RoverNetworkBinder] using [ConnectivityManager.bindProcessToNetwork]
 * and exposing [activeNetwork] for per-request socket binding (Default D5).
 */
class AndroidRoverNetworkBinder(
    private val context: Context
) : RoverNetworkBinder {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isBoundToRoverWifi = MutableStateFlow(false)
    override val isBoundToRoverWifi: StateFlow<Boolean> = _isBoundToRoverWifi.asStateFlow()

    private val _boundNetworkInfo = MutableStateFlow<String?>(null)
    override val boundNetworkInfo: StateFlow<String?> = _boundNetworkInfo.asStateFlow()

    @Volatile
    private var _activeNetwork: Network? = null
    override val activeNetwork: Network? get() = _activeNetwork

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun startListening() {
        val cm = connectivityManager ?: return
        if (networkCallback != null) return

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                try {
                    val capabilities = cm.getNetworkCapabilities(network)
                    if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                        _activeNetwork = network
                        // Bind process socket connections to this Wi-Fi interface
                        cm.bindProcessToNetwork(network)
                        _isBoundToRoverWifi.value = true
                        _boundNetworkInfo.value = "Bound to Wi-Fi Network ($network)"
                    }
                } catch (_: Exception) {
                    // Graceful fallback if permission or OS restricts binding
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                _activeNetwork = null
                try {
                    cm.bindProcessToNetwork(null)
                } catch (_: Exception) { }
                _isBoundToRoverWifi.value = false
                _boundNetworkInfo.value = null
            }
        }

        networkCallback = callback
        try {
            cm.registerNetworkCallback(request, callback)
        } catch (_: Exception) {
            // Graceful fallback for restricted test environments
        }
    }

    override fun stopListening() {
        val cm = connectivityManager ?: return
        networkCallback?.let { callback ->
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Exception) { }
            networkCallback = null
        }
        unbind()
    }

    override fun unbind() {
        _activeNetwork = null
        val cm = connectivityManager ?: return
        try {
            cm.bindProcessToNetwork(null)
        } catch (_: Exception) { }
        _isBoundToRoverWifi.value = false
        _boundNetworkInfo.value = null
    }
}

/**
 * Fake in-memory implementation of [RoverNetworkBinder] for deterministic unit testing.
 */
class FakeRoverNetworkBinder(
    initialBound: Boolean = false
) : RoverNetworkBinder {
    private val _isBound = MutableStateFlow(initialBound)
    override val isBoundToRoverWifi: StateFlow<Boolean> = _isBound.asStateFlow()

    private val _info = MutableStateFlow<String?>(if (initialBound) "Fake Wi-Fi" else null)
    override val boundNetworkInfo: StateFlow<String?> = _info.asStateFlow()

    override fun startListening() {
        _isBound.value = true
        _info.value = "Fake Wi-Fi Bound"
    }

    override fun stopListening() {
        unbind()
    }

    override fun unbind() {
        _isBound.value = false
        _info.value = null
    }

    fun setBound(bound: Boolean, info: String? = null) {
        _isBound.value = bound
        _info.value = info
    }
}
