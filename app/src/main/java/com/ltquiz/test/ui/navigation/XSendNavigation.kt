package com.ltquiz.test.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ltquiz.test.ui.animations.AnimationUtils
import com.ltquiz.test.ui.screens.*

@Composable
fun XSendNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "launch"
    ) {
        composable(
            "launch",
            enterTransition = { AnimationUtils.fadeInTransition() },
            exitTransition = { AnimationUtils.fadeOutTransition() }
        ) {
            LaunchScreen(
                onCreateRoom = { navController.navigate("permissions/create_room") },
                onJoinRoom = { navController.navigate("permissions/join_room") },
                onSettings = { navController.navigate("settings") }
            )
        }
        
        composable(
            "permissions/{destination}",
            enterTransition = { AnimationUtils.slideInFromRight() },
            exitTransition = { AnimationUtils.slideOutToLeft() },
            popEnterTransition = { AnimationUtils.slideInFromLeft() },
            popExitTransition = { AnimationUtils.slideOutToRight() }
        ) { backStackEntry ->
            val destination = backStackEntry.arguments?.getString("destination") ?: "create_room"
            PermissionScreen(
                onPermissionsGranted = {
                    when (destination) {
                        "create_room" -> navController.navigate("create_room") {
                            popUpTo("permissions/{destination}") { inclusive = true }
                        }
                        "join_room" -> navController.navigate("join_room") {
                            popUpTo("permissions/{destination}") { inclusive = true }
                        }
                    }
                },
                onPermissionsDenied = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            "create_room",
            enterTransition = { AnimationUtils.slideInFromRight() },
            exitTransition = { AnimationUtils.slideOutToLeft() },
            popEnterTransition = { AnimationUtils.slideInFromLeft() },
            popExitTransition = { AnimationUtils.slideOutToRight() }
        ) {
            RoomCreationScreen(
                onNavigateBack = { navController.popBackStack() },
                onStartCall = { roomCode ->
                    navController.navigate("call/$roomCode") {
                        popUpTo("launch") { inclusive = false }
                    }
                }
            )
        }
        
        composable(
            "join_room",
            enterTransition = { AnimationUtils.slideInFromRight() },
            exitTransition = { AnimationUtils.slideOutToLeft() },
            popEnterTransition = { AnimationUtils.slideInFromLeft() },
            popExitTransition = { AnimationUtils.slideOutToRight() }
        ) {
            JoinRoomScreen(
                onNavigateBack = { navController.popBackStack() },
                onJoinSuccess = { roomCode ->
                    navController.navigate("call/$roomCode") {
                        popUpTo("launch") { inclusive = false }
                    }
                }
            )
        }
        
        composable(
            "call/{roomCode}",
            enterTransition = { AnimationUtils.scaleInTransition() },
            exitTransition = { AnimationUtils.scaleOutTransition() }
        ) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
            CallScreen(
                roomCode = roomCode,
                onLeaveCall = { 
                    navController.navigate("launch") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            "settings",
            enterTransition = { AnimationUtils.slideInFromRight() },
            exitTransition = { AnimationUtils.slideOutToLeft() },
            popEnterTransition = { AnimationUtils.slideInFromLeft() },
            popExitTransition = { AnimationUtils.slideOutToRight() }
        ) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}