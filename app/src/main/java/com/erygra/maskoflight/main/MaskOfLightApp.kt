package com.erygra.maskoflight.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.erygra.maskoflight.core.GameState
import com.erygra.maskoflight.ui.*
import com.erygra.maskoflight.ui.theme.MaskOfLightTheme

/**
 * MaskOfLightApp - الدالة الرئيسية للتطبيق
 * 
 * تقوم بـ:
 * - إعداد Theme والألوان
 * - إعداد Navigation
 * - اختيار الشاشة الصحيحة بناءً على حالة اللعبة
 * - إدارة الانتقالات بين الشاشات
 * 
 * Parameters:
 * @param gameViewModel الـ ViewModel الرئيسي للعبة
 * @param onPermissionsGranted callback عند الموافقة على الأذن
 * @param onPermissionsDenied callback عند رفض الأذن
 */
@Composable
fun MaskOfLightApp(
    gameViewModel: GameViewModel,
    onPermissionsGranted: () -> Unit = {},
    onPermissionsDenied: () -> Unit = {}
) {
    // ====================================================================
    // 🎨 Theme والإعدادات
    // ====================================================================

    MaskOfLightTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // حالة التطبيق من ViewModel
            val appState by gameViewModel.appState.collectAsState()
            val isInitialized by gameViewModel.isGameInitialized.collectAsState()
            val currentScreen by gameViewModel.currentScreen.collectAsState()

            // Navigation Controller
            val navController = rememberNavController()

            // ================================================================
            // 🎯 اختيار الشاشة الصحيحة
            // ================================================================

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                when {
                    // 1. شاشة البداية / التحميل
                    !isInitialized -> {
                        SplashScreenContent(
                            onInitialized = onPermissionsGranted
                        )
                    }

                    // 2. شاشات اللعبة الرئيسية
                    isInitialized -> {
                        GameNavigation(
                            navController = navController,
                            gameViewModel = gameViewModel,
                            appState = appState,
                            currentScreen = currentScreen
                        )
                    }
                }
            }
        }
    }
}

/**
 * GameNavigation - نظام التنقل للعبة
 * 
 * تنقل بين:
 * - شاشة القائمة الرئيسية (Main Menu)
 * - شاشة اللعبة (Game Screen)
 * - شاشات الإعدادات (Settings)
 * - شاشات الدعم (Help)
 * 
 * @param navController controller التنقل
 * @param gameViewModel ViewModel اللعبة
 * @param appState حالة التطبيق
 * @param currentScreen الشاشة الحالية
 */
@Composable
fun GameNavigation(
    navController: NavHostController,
    gameViewModel: GameViewModel,
    appState: String,
    currentScreen: String
) {
    NavHost(
        navController = navController,
        startDestination = "main_menu"
    ) {
        // ================================================================
        // شاشة القائمة الرئيسية
        // ================================================================
        composable("main_menu") {
            MainMenuScreen(
                gameViewModel = gameViewModel,
                onNewGame = {
                    gameViewModel.startNewGame()
                    navController.navigate("game_screen") {
                        popUpTo("main_menu") { saveState = true }
                        launchSingleTop = true
                    }
                },
                onContinueGame = {
                    gameViewModel.resumeGame()
                    navController.navigate("game_screen") {
                        popUpTo("main_menu") { saveState = true }
                        launchSingleTop = true
                    }
                },
                onSettings = {
                    navController.navigate("settings_screen")
                },
                onCredits = {
                    navController.navigate("credits_screen")
                },
                onExit = {
                    gameViewModel.exitGame()
                }
            )
        }

        // ================================================================
        // شاشة اللعبة الرئيسية
        // ================================================================
        composable("game_screen") {
            GameScreenContent(
                gameViewModel = gameViewModel,
                onPause = {
                    gameViewModel.pauseGame()
                    navController.navigate("pause_menu")
                },
                onGameOver = {
                    gameViewModel.handleGameOver()
                    navController.navigate("game_over_screen") {
                        popUpTo("game_screen") { inclusive = true }
                    }
                },
                onLevelComplete = {
                    gameViewModel.handleLevelComplete()
                    navController.navigate("level_complete_screen")
                }
            )
        }

        // ================================================================
        // قائمة الإيقاف المؤقت
        // ================================================================
        composable("pause_menu") {
            PauseMenuScreen(
                gameViewModel = gameViewModel,
                onResume = {
                    gameViewModel.resumeGame()
                    navController.popBackStack()
                },
                onSettings = {
                    navController.navigate("settings_screen")
                },
                onQuitToMenu = {
                    gameViewModel.quitToMainMenu()
                    navController.navigate("main_menu") {
                        popUpTo("pause_menu") { inclusive = true }
                    }
                }
            )
        }

        // ================================================================
        // شاشة نهاية اللعبة
        // ================================================================
        composable("game_over_screen") {
            GameOverScreen(
                gameViewModel = gameViewModel,
                onRetry = {
                    gameViewModel.restartLevel()
                    navController.navigate("game_screen") {
                        popUpTo("game_over_screen") { inclusive = true }
                    }
                },
                onQuitToMenu = {
                    navController.navigate("main_menu") {
                        popUpTo("game_over_screen") { inclusive = true }
                    }
                }
            )
        }

        // ================================================================
        // شاشة اكتمال المرحلة
        // ================================================================
        composable("level_complete_screen") {
            LevelCompleteScreen(
                gameViewModel = gameViewModel,
                onContinue = {
                    gameViewModel.loadNextLevel()
                    navController.navigate("game_screen") {
                        popUpTo("level_complete_screen") { inclusive = true }
                    }
                },
                onReturnToMenu = {
                    navController.navigate("main_menu") {
                        popUpTo("level_complete_screen") { inclusive = true }
                    }
                }
            )
        }

        // ================================================================
        // شاشة الإعدادات
        // ================================================================
        composable("settings_screen") {
            SettingsScreen(
                gameViewModel = gameViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ================================================================
        // شاشة الاعتمادات / الفضل
        // ================================================================
        composable("credits_screen") {
            CreditsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * MainMenuScreen - شاشة القائمة الرئيسية
 * 
 * تعرض:
 * - شعار اللعبة
 * - أزرار العمل الرئيسية
 * - خيارات الإعدادات
 * - معلومات الإصدار
 */
@Composable
fun MainMenuScreen(
    gameViewModel: GameViewModel,
    onNewGame: () -> Unit = {},
    onContinueGame: () -> Unit = {},
    onSettings: () -> Unit = {},
    onCredits: () -> Unit = {},
    onExit: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        MenuSystem(
            onNewGameClick = onNewGame,
            onContinueClick = onContinueGame,
            onSettingsClick = onSettings,
            onCreditsClick = onCredits,
            onExitClick = onExit
        )
    }
}

/**
 * GameScreenContent - محتوى شاشة اللعبة
 * 
 * يعرض:
 * - منطقة لعب اللعبة
 * - HUD والواجهات
 * - أزرار التحكم
 */
@Composable
fun GameScreenContent(
    gameViewModel: GameViewModel,
    onPause: () -> Unit = {},
    onGameOver: () -> Unit = {},
    onLevelComplete: () -> Unit = {}
) {
    // الحصول على حالة اللعبة
    val gameState by gameViewModel.gameState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // عرض لعبة اللعبة الفعلية
        GameplayArea(
            gameViewModel = gameViewModel
        )

        // عرض HUD
        HUD(
            gameViewModel = gameViewModel,
            onPauseClick = onPause
        )

        // مراقبة حالة اللعبة للأحداث
        LaunchedEffect(gameState) {
            when (gameState) {
                GameState.GAME_OVER -> onGameOver()
                GameState.LEVEL_COMPLETE -> onLevelComplete()
                else -> {} // حالات أخرى لا تتطلب إجراء
            }
        }
    }
}

/**
 * PauseMenuScreen - قائمة الإيقاف المؤقت
 */
@Composable
fun PauseMenuScreen(
    gameViewModel: GameViewModel,
    onResume: () -> Unit = {},
    onSettings: () -> Unit = {},
    onQuitToMenu: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
    ) {
        PauseMenu(
            onResumeClick = onResume,
            onSettingsClick = onSettings,
            onQuitClick = onQuitToMenu
        )
    }
}

/**
 * GameOverScreen - شاشة نهاية اللعبة
 */
@Composable
fun GameOverScreen(
    gameViewModel: GameViewModel,
    onRetry: () -> Unit = {},
    onQuitToMenu: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GameOverDialog(
            onRetryClick = onRetry,
            onMenuClick = onQuitToMenu
        )
    }
}

/**
 * LevelCompleteScreen - شاشة اكتمال المرحلة
 */
@Composable
fun LevelCompleteScreen(
    gameViewModel: GameViewModel,
    onContinue: () -> Unit = {},
    onReturnToMenu: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LevelCompleteDialog(
            gameViewModel = gameViewModel,
            onContinueClick = onContinue,
            onMenuClick = onReturnToMenu
        )
    }
}

/**
 * SettingsScreen - شاشة الإعدادات
 */
@Composable
fun SettingsScreen(
    gameViewModel: GameViewModel,
    onBack: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        SettingsMenu(
            gameViewModel = gameViewModel,
            onBackClick = onBack
        )
    }
}

/**
 * CreditsScreen - شاشة الاعتمادات
 */
@Composable
fun CreditsScreen(
    onBack: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        CreditsMenu(
            onBackClick = onBack
        )
    }
}