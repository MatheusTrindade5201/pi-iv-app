package br.univesp.pi.expedientes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme
import br.univesp.pi.expedientes.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            ExpedientesTheme {
                val isLoggedIn by appViewModel.isLoggedIn.collectAsStateWithLifecycle()
                when (val loggedIn = isLoggedIn) {
                    null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    else -> AppNavHost(isLoggedIn = loggedIn)
                }
            }
        }
    }
}
