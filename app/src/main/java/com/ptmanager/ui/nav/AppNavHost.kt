package com.ptmanager.ui.nav

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ptmanager.ui.add.AddTorrentScreen
import com.ptmanager.ui.detail.TorrentDetailScreen
import com.ptmanager.ui.servers.ServersScreen
import com.ptmanager.ui.torrents.TorrentListScreen

object Routes {
    const val SERVERS = "servers"
    const val TORRENTS = "torrents/{serverId}"
    const val DETAIL = "detail/{serverId}/{torrentHash}"
    const val ADD = "add/{serverId}"

    fun torrents(serverId: String) = "torrents/$serverId"
    fun detail(serverId: String, hash: String) = "detail/$serverId/${Uri.encode(hash)}"
    fun add(serverId: String) = "add/$serverId"
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.SERVERS) {
        composable(Routes.SERVERS) {
            ServersScreen(
                onServerOpened = { nav.navigate(Routes.torrents(it)) }
            )
        }
        composable(
            Routes.TORRENTS,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) { entry ->
            val serverId = entry.arguments?.getString("serverId").orEmpty()
            TorrentListScreen(
                serverId = serverId,
                onBack = { nav.popBackStack() },
                onOpenDetail = { hash -> nav.navigate(Routes.detail(serverId, hash)) },
                onAddTorrent = { nav.navigate(Routes.add(serverId)) },
            )
        }
        composable(
            Routes.DETAIL,
            arguments = listOf(
                navArgument("serverId") { type = NavType.StringType },
                navArgument("torrentHash") { type = NavType.StringType },
            )
        ) { entry ->
            TorrentDetailScreen(
                serverId = entry.arguments?.getString("serverId").orEmpty(),
                torrentHash = entry.arguments?.getString("torrentHash").orEmpty(),
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.ADD,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) { entry ->
            AddTorrentScreen(
                serverId = entry.arguments?.getString("serverId").orEmpty(),
                onBack = { nav.popBackStack() },
            )
        }
    }
}