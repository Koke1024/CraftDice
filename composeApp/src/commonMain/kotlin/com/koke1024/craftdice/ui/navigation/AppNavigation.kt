package com.koke1024.craftdice.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.koke1024.craftdice.ui.battle.BattleScreen
import com.koke1024.craftdice.ui.home.HomeScreen

object Routes {
    const val HOME = "home"
    const val BATTLE = "battle"
    const val CRAFT = "craft"
    const val DUNGEON = "dungeon"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToBattle = { navController.navigate(Routes.BATTLE) },
                onNavigateToCraft = { navController.navigate(Routes.CRAFT) },
                onNavigateToDungeon = { navController.navigate(Routes.DUNGEON) },
            )
        }
        composable(Routes.BATTLE) {
            BattleScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Routes.CRAFT) {
            PlaceholderScreen("Craft")
        }
        composable(Routes.DUNGEON) {
            PlaceholderScreen("Dungeon")
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = title)
    }
}
