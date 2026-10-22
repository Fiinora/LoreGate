package com.loregate.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.loregate.app.feature.auth.LoginRoute
import com.loregate.app.feature.home.HomeRoute
import com.loregate.app.feature.map.MapRoute
import com.loregate.app.feature.passport.PassportRoute
import com.loregate.app.feature.profile.ProfileRoute
import com.loregate.app.feature.story.ChapterDetailRoute
import com.loregate.app.feature.story.GameChapterRoute
import com.loregate.app.feature.story.StoryListRoute
import com.loregate.app.feature.story.StoryProgressRoute

@Composable
fun LoreGateNavHost(
    isUserAuthenticated: Boolean,
    navController: NavHostController = rememberNavController()
) {
    val startDestination: Any = if (isUserAuthenticated) HomeRoute else LoginRoute

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<LoginRoute> {
            LoginRoute(
                onNavigateToHome = {
                    navController.navigate(HomeRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<HomeRoute> {
            HomeRoute(
                onNavigateToStoryList = { navController.navigate(StoryListRoute) },
                onNavigateToMap = { navController.navigate(MapRoute()) },
                onNavigateToProfile = { navController.navigate(ProfileRoute) },
                onNavigateToPassport = { navController.navigate(PassportRoute) },
                onNavigateToMapWithTrail = { trailId ->
                    navController.navigate(MapRoute(initialTrailId = trailId))
                }
            )
        }

        composable<PassportRoute> {
            PassportRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMapWithSite = { siteId ->
                    navController.navigate(MapRoute(targetSiteId = siteId))
                }
            )
        }

        composable<ProfileRoute> {
            ProfileRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPassport = { navController.navigate(PassportRoute) },
                onNavigateToStoryProgress = { navController.navigate(StoryProgressRoute) },
                onNavigateToStoryList = { navController.navigate(StoryListRoute) },
                onNavigateToLogin = {
                    navController.navigate(LoginRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<StoryListRoute> {
            StoryListRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGameChapter = { storyGroupId ->
                    navController.navigate(GameChapterRoute(storyGroupId = storyGroupId))
                }
            )
        }

        composable<StoryProgressRoute> {
            StoryProgressRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGameChapter = { storyGroupId ->
                    navController.navigate(GameChapterRoute(storyGroupId = storyGroupId))
                }
            )
        }

        composable<MapRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<MapRoute>()
            MapRoute(
                targetSiteId = route.targetSiteId,
                initialTrailId = route.initialTrailId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(ProfileRoute) },
                onNavigateToChapterDetail = { siteId ->
                    navController.navigate(ChapterDetailRoute(siteId = siteId))
                }
            )
        }

        composable<GameChapterRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<GameChapterRoute>()
            GameChapterRoute(
                storyGroupId = route.storyGroupId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMapWithTarget = { siteId ->
                    navController.navigate(MapRoute(targetSiteId = siteId))
                },
                onNavigateToChapterDetail = { siteId ->
                    navController.navigate(ChapterDetailRoute(siteId = siteId))
                }
            )
        }

        composable<ChapterDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ChapterDetailRoute>()
            ChapterDetailRoute(
                siteId = route.siteId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNextSiteOnMap = { nextSiteId ->
                    navController.navigate(MapRoute(targetSiteId = nextSiteId)) {
                        popUpTo<ChapterDetailRoute> { inclusive = true }
                    }
                }
            )
        }
    }
}
