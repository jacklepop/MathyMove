package com.mathymove.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathymove.game.model.HighScoreEntry
import com.mathymove.game.ui.theme.GreyBackground
import com.mathymove.game.ui.theme.GreyBorder
import com.mathymove.game.ui.theme.GreySurface
import com.mathymove.game.ui.theme.NodeActiveBackground
import com.mathymove.game.ui.theme.TextPrimary
import com.mathymove.game.ui.theme.TextSecondary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState

@Composable
fun HighScoreDialog(
    highScores: List<HighScoreEntry>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GreySurface,
        title = {
            Text(
                text = "Top 10 High Scores",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            if (highScores.isEmpty()) {
                Text(
                    text = "No high scores recorded yet. Reach a target number to score points!",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
            } else {
                val listState = rememberLazyListState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 12.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        itemsIndexed(highScores) { index, entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${index + 1}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "${entry.score} pts",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = entry.formattedDateTime,
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                            if (index < highScores.size - 1) {
                                HorizontalDivider(color = GreyBorder.copy(alpha = 0.5f))
                            }
                        }
                    }

                    // Custom Scrollbar: Light grey 20px height x 4px width rounded scrollbar at right side
                    val layoutInfo = listState.layoutInfo
                    val visibleItemsInfo = layoutInfo.visibleItemsInfo
                    val totalItems = layoutInfo.totalItemsCount

                    if (totalItems > 0 && visibleItemsInfo.isNotEmpty()) {
                        val avgItemSize = visibleItemsInfo.sumOf { it.size }.toFloat() / visibleItemsInfo.size
                        val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
                        val totalScrollableContentHeight = (totalItems * avgItemSize - viewportHeight).coerceAtLeast(1f)

                        val currentScrollOffset = (listState.firstVisibleItemIndex * avgItemSize) + listState.firstVisibleItemScrollOffset
                        val scrollFraction = (currentScrollOffset / totalScrollableContentHeight).coerceIn(0f, 1f)
                        val canScroll = totalItems > visibleItemsInfo.size || listState.firstVisibleItemScrollOffset > 0

                        if (canScroll) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = (240 * scrollFraction).dp, end = 2.dp)
                                    .width(4.dp)
                                    .height(20.dp)
                                    .background(
                                        color = GreyBorder,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
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
                    text = "Close",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}
