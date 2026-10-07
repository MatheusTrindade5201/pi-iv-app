package br.univesp.pi.expedientes.core.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}

@Singleton
class ConnectivityNetworkMonitor
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : NetworkMonitor {
        private val manager = context.getSystemService(ConnectivityManager::class.java)
        private val online = MutableStateFlow(hasInternet(manager.getNetworkCapabilities(manager.activeNetwork)))
        override val isOnline: StateFlow<Boolean> = online.asStateFlow()

        init {
            manager.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities,
                    ) {
                        online.value = hasInternet(capabilities)
                    }

                    override fun onLost(network: Network) {
                        online.value = false
                    }
                },
            )
        }

        private fun hasInternet(capabilities: NetworkCapabilities?): Boolean =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModule {
    @Binds
    abstract fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor
}
