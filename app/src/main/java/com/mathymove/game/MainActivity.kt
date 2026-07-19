package com.mathymove.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mathymove.game.ui.screens.GameScreen
import com.mathymove.game.ui.screens.StartScreen
import com.mathymove.game.ui.theme.GreyBackground
import com.mathymove.game.ui.theme.MathyMoveTheme
import com.mathymove.game.viewmodel.GameViewModel
import kotlinx.serialization.Serializable

@Serializable
object StartRoute

@Serializable
object GameRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MathyMoveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GreyBackground
                ) {
                    MathyMoveApp()
                }
            }
        }
    }
}

@Composable
fun MathyMoveApp(
    gameViewModel: GameViewModel = viewModel()
) {
    val navController = rememberNavController()
    val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = StartRoute
    ) {
        composable<StartRoute> {
            StartScreen(
                hasSavedGame = uiState.hasSavedGame,
                onNewGame = {
                    gameViewModel.startNewGame()
                    navController.navigate(GameRoute)
                },
                onContinueGame = {
                    gameViewModel.continueGame()
                    navController.navigate(GameRoute)
                }
            )
        }

        composable<GameRoute> {
            GameScreen(
                state = uiState,
                onNodeTapped = { nodeId ->
                    gameViewModel.onNodeTapped(nodeId)
                },
                onTryAgain = {
                    gameViewModel.startNewGame()
                },
                onBackToMenu = {
                    navController.popBackStack()
                }
            )
        }
    }
}
