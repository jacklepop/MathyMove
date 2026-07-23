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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.mathymove.game.ui.theme.TextSecondary
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    state: GameState,
    onNodeTapped: (String) -> Unit,
    onTryAgain: () -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showHighScoreDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    // Target Number Animation (Fade out in 0.5s -> Fade in 1.0s & scale 200% down to 100%)
    var displayedTarget by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(state.targetNumber) }
    val targetAlpha = androidx.compose.runtime.remember { Animatable(1f) }
    val targetScale = androidx.compose.runtime.remember { Animatable(1f) }

    androidx.compose.runtime.LaunchedEffect(state.targetNumber) {
        if (state.targetNumber != displayedTarget) {
            // Phase 1: Fade out old target to 0 visibility in 0.5 seconds (500ms)
            targetAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 500,
                    easing = LinearEasing
                )
            )

            // Update displayed target value
            displayedTarget = state.targetNumber

            // Reset scale to 200% (2.0f) and alpha to 0 for Phase 2
            targetScale.snapTo(2f)

            // Phase 2: Fade in to 100% visibility & scale down to 100% size over 1.0 second (1000ms)
            coroutineScope {
                launch {
                    targetAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 1000,
                            easing = LinearEasing
                        )
                    )
                }
                launch {
                    targetScale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 1000,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            }
        }
    }

    // Remaining Moves Animation (Target style when <= 5: fade out 0.5s -> fade in 1.0s & scale 200% down to 100%)
    val currentMovesRemaining = state.movesRemainingForTarget
    var displayedMoves by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(currentMovesRemaining) }
    val movesAlpha = androidx.compose.runtime.remember { Animatable(1f) }
    val movesScale = androidx.compose.runtime.remember { Animatable(1f) }

    // Clutch Target Win Animation (200% -> 100% scale & Bright Green -> Default white over 2 seconds)
    val clutchProgress = androidx.compose.runtime.remember { Animatable(0f) }

    androidx.compose.runtime.LaunchedEffect(state.clutchBonusTimestamp) {
        if (state.clutchBonusTimestamp > 0L) {
            displayedMoves = currentMovesRemaining
            movesAlpha.snapTo(1f)
            movesScale.snapTo(1f)
            clutchProgress.snapTo(1f)
            clutchProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 2000,
                    easing = LinearEasing
                )
            )
        }
    }

    androidx.compose.runtime.LaunchedEffect(currentMovesRemaining) {
        if (currentMovesRemaining <= 5 && currentMovesRemaining != displayedMoves && state.clutchBonusTimestamp == 0L) {
            // Phase 1: Fade out old moves count to 0 visibility in 0.5 seconds (500ms)
            movesAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 500,
                    easing = LinearEasing
                )
            )

            // Update displayed moves count
            displayedMoves = currentMovesRemaining

            // Reset scale to 200% (2.0f) and alpha to 0 for Phase 2
            movesScale.snapTo(2f)

            // Phase 2: Fade in to 100% visibility & scale down to 100% size over 1.0 second (1000ms)
            coroutineScope {
                launch {
                    movesAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 1000,
                            easing = LinearEasing
                        )
                    )
                }
                launch {
                    movesScale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 1000,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            }
        } else {
            // Default display animation when > 5
            displayedMoves = currentMovesRemaining
            if (clutchProgress.value <= 0.01f) {
                movesAlpha.snapTo(1f)
                movesScale.snapTo(1f)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GreyBackground)
    ) {
        // Main Infinite Canvas Viewport
        GameCanvas(
            nodes = state.nodes,
            activeNodeId = state.activeNodeId,
            activeRemainder = state.activeRemainder,
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
            // Hamburger menu button (3 line symbol) positioned to the right of Target
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp)
            ) {
                androidx.compose.material3.IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(43.dp)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
                        val strokeWidth = 3.dp.toPx()
                        val color = GreySurface
                        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.2f), end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.2f), strokeWidth = strokeWidth)
                        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.5f), end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.5f), strokeWidth = strokeWidth)
                        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.8f), end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.8f), strokeWidth = strokeWidth)
                    }
                }

                androidx.compose.material3.DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(GreySurface)
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Continue Game", color = TextPrimary, fontSize = 16.8.sp, fontWeight = FontWeight.Medium) },
                        onClick = {
                            showMenu = false
                        }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("New Game", color = TextPrimary, fontSize = 16.8.sp, fontWeight = FontWeight.Medium) },
                        onClick = {
                            showMenu = false
                            onTryAgain()
                        }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("High scores", color = TextPrimary, fontSize = 16.8.sp, fontWeight = FontWeight.Medium) },
                        onClick = {
                            showMenu = false
                            showHighScoreDialog = true
                        }
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "TARGET",
                    fontSize = 13.2.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "$displayedTarget",
                    fontSize = 52.8.sp,
                    fontWeight = FontWeight.Light,
                    color = GreySurface,
                    modifier = Modifier.graphicsLayer {
                        this.alpha = targetAlpha.value
                        this.scaleX = targetScale.value
                        this.scaleY = targetScale.value
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3 Equal-width columns: Remaining Moves, Moves Taken, Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val clutchActive = clutchProgress.value > 0.001f
                    val effectiveScale = if (clutchActive) (1f + clutchProgress.value) else movesScale.value
                    val effectiveColor = if (clutchActive) {
                        androidx.compose.ui.graphics.lerp(GreySurface, com.mathymove.game.ui.theme.BrightGreenBonus, clutchProgress.value)
                    } else {
                        GreySurface
                    }
                    HudStatItem(
                        label = "Remaining Moves",
                        value = "$displayedMoves",
                        alpha = movesAlpha.value,
                        scale = effectiveScale,
                        textColor = effectiveColor,
                        modifier = Modifier.weight(1f)
                    )
                    HudStatItem(
                        label = "Moves Taken",
                        value = "${state.totalMovesTaken}",
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
                        fontSize = 28.8.sp,
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
                            fontSize = 18.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Target was: ${state.targetNumber}",
                            fontSize = 19.2.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Your current value: ${state.currentValue}",
                            fontSize = 19.2.sp,
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
                            fontSize = 19.2.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            )
        }

        if (showHighScoreDialog) {
            com.mathymove.game.ui.components.HighScoreDialog(
                highScores = state.highScores,
                onDismiss = { showHighScoreDialog = false }
            )
        }
    }
}

@Composable
private fun HudStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    scale: Float = 1f,
    textColor: androidx.compose.ui.graphics.Color = GreySurface
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 11.4.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            fontSize = 20.4.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
            }
        )
    }
}

