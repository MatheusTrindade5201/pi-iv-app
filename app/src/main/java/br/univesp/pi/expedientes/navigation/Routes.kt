package br.univesp.pi.expedientes.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import br.univesp.pi.expedientes.R
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data object LoginRoute

@Serializable
data object HomeRoute

@Serializable
data object HistoryRoute

@Serializable
data object ProfileRoute

enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
) {
    Home(HomeRoute, HomeRoute::class, R.string.nav_home, R.drawable.ic_home),
    History(HistoryRoute, HistoryRoute::class, R.string.nav_history, R.drawable.ic_history),
    Profile(ProfileRoute, ProfileRoute::class, R.string.nav_profile, R.drawable.ic_profile),
}
