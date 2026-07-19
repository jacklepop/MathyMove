package com.mathymove.game.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathymove.game.model.GameState
import com.mathymove.game.ui.components.GameCanvas
import com.mathymove.game.ui.theme.GreyBackground
import com.mathymove.game.ui.theme.GreySurface
import com.mathymove.game.ui.theme.NodeActiveBackground
import com.mathymove.game.ui.theme.TextPrimary
import com.mathymove.game.ui.theme.TextSecondary

@Composable
fun GameScreen(
    state: GameState,
    onNodeTapped: (String) -> Unit,
    onTryAgain: () -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GreyBackground)
    ) {
        // Main Infinite Canvas Viewport
        GameCanvas(
            nodes = state.nodes,
            activeNodeId = state.activeNodeId,
            onNodeTapped = onNodeTapped,
            modifier = Modifier.fillMaxSize()
        )

        // Edge-to-Edge Top Black Box HUD (No surrounding gaps)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    color = NodeActiveBackground,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .statusBarsPadding()
                .padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "TARGET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "${state.targetNumber}",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
                    color = GreySurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3 Equal-width columns: Remaining Moves, Moves Taken, Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudStatItem(
                        label = "Remaining Moves",
                        value = "${state.movesRemainingForTarget}",
                        modifier = Modifier.weight(1f)
                    )
                    HudStatItem(
                        label = "Moves Taken",
                        value = "${state.movesTakenForTarget}",
                        modifier = Modifier.weight(1f)
                    )
                    HudStatItem(
                        label = "Score",
                        value = "${state.score}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Try Again / Game Over Dialog
        if (state.isGameOver) {
            AlertDialog(
                onDismissRequest = { },
                containerColor = GreySurface,
                title = {
                    Text(
                        text = "Try again",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Moves ran out before reaching the target.",
                            fontSize = 15.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Target was: ${state.targetNumber}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Your current value: ${state.currentValue}",
                            fontSize = 16.sp,
                            color = TextSecondary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onTryAgain,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NodeActiveBackground,
                            contentColor = GreyBackground
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "New Game",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun HudStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = GreySurface,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

