package com.mathymove.game.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathymove.game.ui.theme.GreyBackground
import com.mathymove.game.ui.theme.GreyBorder
import com.mathymove.game.ui.theme.NodeActiveBackground
import com.mathymove.game.ui.theme.TextPrimary
import com.mathymove.game.ui.theme.TextSecondary

@Composable
fun StartScreen(
    hasSavedGame: Boolean,
    highScores: List<com.mathymove.game.model.HighScoreEntry>,
    onNewGame: () -> Unit,
    onContinueGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHighScoresDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GreyBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            // Minimal Title Header
            Text(
                text = "MathyMove",
                fontSize = 50.4.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimary,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Keep your mind active through endless numerical pathways",
                fontSize = 16.8.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Continue Game Option (if saved game exists)
            AnimatedVisibility(
                visible = hasSavedGame,
                enter = fadeIn() + slideInVertically { it / 2 }
            ) {
                Column {
                    Button(
                        onClick = onContinueGame,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NodeActiveBackground,
                            contentColor = GreyBackground
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Continue Game",
                            fontSize = 21.6.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // New Game Button
            if (hasSavedGame) {
                OutlinedButton(
                    onClick = onNewGame,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "New Game",
                        fontSize = 21.6.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            } else {
                Button(
                    onClick = onNewGame,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NodeActiveBackground,
                        contentColor = GreyBackground
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "New Game",
                        fontSize = 21.6.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // High Scores Button
            OutlinedButton(
                onClick = { showHighScoresDialog = true },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "High Scores",
                    fontSize = 19.2.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        if (showHighScoresDialog) {
            com.mathymove.game.ui.components.HighScoreDialog(
                highScores = highScores,
                onDismiss = { showHighScoresDialog = false }
            )
        }
    }
}
