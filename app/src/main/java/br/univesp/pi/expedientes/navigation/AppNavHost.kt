package br.univesp.pi.expedientes.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.univesp.pi.expedientes.R
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme
import br.univesp.pi.expedientes.feature.auth.LoginEntry
import br.univesp.pi.expedientes.feature.auth.RegisterEntry
import br.univesp.pi.expedientes.feature.history.HistoryScreen
import br.univesp.pi.expedientes.feature.home.HomeEntry
import br.univesp.pi.expedientes.feature.profile.ProfileEntry

@Composable
fun AppNavHost(
    isLoggedIn: Boolean,
    navController: NavHostController = rememberNavController(),
    loginContent: @Composable (onRegisterClick: () -> Unit) -> Unit = { LoginEntry(onRegisterClick = it) },
    registerContent: @Composable (onLoginClick: () -> Unit) -> Unit = { RegisterEntry(onLoginClick = it) },
    homeContent: @Composable () -> Unit = { HomeEntry() },
    profileContent: @Composable () -> Unit = { ProfileEntry() },
) {
    val startDestination = remember { if (isLoggedIn) HomeRoute else LoginRoute }
    LaunchedEffect(isLoggedIn) {
        val destination = navController.currentDestination ?: return@LaunchedEffect
        val onAuth = destination.hasRoute(LoginRoute::class) || destination.hasRoute(RegisterRoute::class)
        when {
            isLoggedIn && onAuth -> navController.resetTo(HomeRoute)
            !isLoggedIn && !onAuth -> navController.resetTo(LoginRoute)
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentTab = TopLevelDestination.entries.firstOrNull { currentDestination.isOn(it) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppHeader() },
        bottomBar = {
            if (currentTab != null) {
                BottomBar(
                    current = currentTab,
                    onSelect = { navController.navigateToTab(it) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
        ) {
            composable<LoginRoute> {
                loginContent { navController.navigate(RegisterRoute) { launchSingleTop = true } }
            }
            composable<RegisterRoute> {
                registerContent { navController.popBackStack() }
            }
            composable<HomeRoute> { homeContent() }
            composable<HistoryRoute> { HistoryScreen() }
            composable<ProfileRoute> { profileContent() }
        }
    }
}

@Composable
private fun AppHeader() {
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
private fun BottomBar(
    current: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
) {
    val colors =
        NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = ExpedientesTheme.colors.navUnselected,
            unselectedTextColor = ExpedientesTheme.colors.navUnselected,
            indicatorColor = Color.Transparent,
        )
    Column {
        HorizontalDivider(color = ExpedientesTheme.colors.textSecondary, thickness = 1.dp)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TopLevelDestination.entries.forEach { destination ->
                val label = stringResource(destination.label)
                NavigationBarItem(
                    selected = destination == current,
                    onClick = { onSelect(destination) },
                    icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                    label = { Text(label) },
                    colors = colors,
                    modifier = Modifier.testTag("tab_${destination.name}"),
                )
            }
        }
    }
}

private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true

private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.resetTo(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
    }
}
