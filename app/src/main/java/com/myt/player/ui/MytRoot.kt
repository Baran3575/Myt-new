package com.myt.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.myt.player.AppState
import com.myt.player.data.model.Track
import com.myt.player.ui.components.PlayerBar
import com.myt.player.ui.screens.HomeScreen
import com.myt.player.ui.screens.LibraryScreen
import com.myt.player.ui.screens.NowPlayingScreen
import com.myt.player.ui.screens.SearchScreen

private object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val NOW_PLAYING = "nowPlaying"
}

@Composable
fun MytRoot() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val inNowPlaying = route == Routes.NOW_PLAYING

    // Start playback of a queue when a screen asks for it.
    fun playQueue(tracks: List<Track>, index: Int) {
        if (tracks.isEmpty()) return
        AppState.player.playQueue(tracks, index)
        AppState.onTrackPlayed(tracks[index])
        navController.navigate(Routes.NOW_PLAYING)
    }

    Scaffold(
        containerColor = com.myt.player.ui.theme.BackgroundBlack,
        bottomBar = {
            if (!inNowPlaying) {
                Column {
                    PlayerBar(onOpen = { navController.navigate(Routes.NOW_PLAYING) })
                    MytBottomBar(
                        currentRoute = route,
                        onSelect = { target ->
                            if (route != target) {
                                navController.navigate(target) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME
            ) {
                composable(Routes.HOME) {
                    HomeScreen(onPlay = { tracks, index -> playQueue(tracks, index) })
                }
                composable(Routes.SEARCH) {
                    SearchScreen(onPlay = { tracks, index -> playQueue(tracks, index) })
                }
                composable(Routes.LIBRARY) {
                    LibraryScreen(onPlay = { tracks, index -> playQueue(tracks, index) })
                }
                composable(Routes.NOW_PLAYING) {
                    NowPlayingScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun MytBottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    val items = listOf(
        Triple(Routes.HOME, "Home", Icons.Rounded.Home),
        Triple(Routes.SEARCH, "Search", Icons.Rounded.Search),
        Triple(Routes.LIBRARY, "Library", Icons.Rounded.LibraryMusic)
    )
    NavigationBar(
        containerColor = com.myt.player.ui.theme.SurfaceDark
    ) {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { onSelect(route) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}