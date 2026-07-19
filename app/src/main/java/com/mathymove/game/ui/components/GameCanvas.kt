package com.mathymove.game.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathymove.game.model.GameNode
import com.mathymove.game.ui.theme.GreyBackground
import com.mathymove.game.ui.theme.LineActiveColor
import com.mathymove.game.ui.theme.LineColor
import com.mathymove.game.ui.theme.NodeActiveBackground
import com.mathymove.game.ui.theme.NodeActiveText
import com.mathymove.game.ui.theme.NodeNormalBackground
import com.mathymove.game.ui.theme.NodeNormalText
import com.mathymove.game.ui.theme.NodeVisitedBackground
import com.mathymove.game.ui.theme.NodeVisitedText
import kotlin.math.hypot

@Composable
fun GameCanvas(
    nodes: Map<String, GameNode>,
    activeNodeId: String,
    onNodeTapped: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val activeNode = nodes[activeNodeId]

    // Smooth panning target offset to center view on active node
    val targetOffsetX = activeNode?.x ?: 0f
    val targetOffsetY = activeNode?.y ?: 0f

    val animOffsetX by animateFloatAsState(
        targetValue = targetOffsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "animOffsetX"
    )

    val animOffsetY by animateFloatAsState(
        targetValue = targetOffsetY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "animOffsetY"
    )

    // Calculate graph distances from activeNodeId via BFS for fade effects
    val distanceMap = androidx.compose.runtime.remember(nodes, activeNodeId) {
        val dists = mutableMapOf<String, Int>()
        val queue = ArrayDeque<String>()

        if (nodes.containsKey(activeNodeId)) {
            dists[activeNodeId] = 0
            queue.add(activeNodeId)
        }

        while (queue.isNotEmpty()) {
            val currId = queue.removeFirst()
            val currDist = dists[currId] ?: 0

            val node = nodes[currId] ?: continue
            val neighbors = mutableListOf<String>()
            node.parentId?.let { neighbors.add(it) }
            neighbors.addAll(node.childrenIds)

            for (nbrId in neighbors) {
                if (nodes.containsKey(nbrId) && !dists.containsKey(nbrId)) {
                    dists[nbrId] = currDist + 1
                    queue.add(nbrId)
                }
            }
        }
        dists
    }

    fun getAlphaForDistance(dist: Int): Float {
        return when (dist) {
            0 -> 1.0f  // Active node & direct radiating lines (100% visible)
            1 -> 0.7f  // 1 level away (30% transparency)
            2 -> 0.4f  // 2 levels away (60% transparency)
            else -> 0.0f
        }
    }

    val circleRadiusPx = 70f
    val lineStrokePx = 4f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GreyBackground)
            .pointerInput(nodes, activeNodeId, animOffsetX, animOffsetY) {
                detectTapGestures { tapOffset ->
                    val centerCanvasX = size.width / 2f
                    val centerCanvasY = size.height / 2f

                    // Find tapped node in world coordinates
                    for (node in nodes.values) {
                        val worldNodeX = node.x
                        val worldNodeY = node.y

                        // Screen coordinates of node
                        val screenNodeX = centerCanvasX + (worldNodeX - animOffsetX)
                        val screenNodeY = centerCanvasY + (worldNodeY - animOffsetY)

                        val dist = hypot(tapOffset.x - screenNodeX, tapOffset.y - screenNodeY)
                        if (dist <= circleRadiusPx + 15f) {
                            if (!node.visited) {
                                onNodeTapped(node.id)
                            }
                            break
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerCanvasX = size.width / 2f
            val centerCanvasY = size.height / 2f

            // Step 1: Draw radiating connecting lines between parent and child nodes
            nodes.values.forEach { node ->
                val parent = node.parentId?.let { nodes[it] }
                if (parent != null) {
                    val pScreenX = centerCanvasX + (parent.x - animOffsetX)
                    val pScreenY = centerCanvasY + (parent.y - animOffsetY)
                    val cScreenX = centerCanvasX + (node.x - animOffsetX)
                    val cScreenY = centerCanvasY + (node.y - animOffsetY)

                    val pDist = distanceMap[parent.id] ?: 2
                    val cDist = distanceMap[node.id] ?: 2
                    val isConnectedToActive = (node.id == activeNodeId || parent.id == activeNodeId)

                    val lineAlpha = if (isConnectedToActive) 1.0f else minOf(getAlphaForDistance(pDist), getAlphaForDistance(cDist))
                    val baseLineColor = if (isConnectedToActive) LineActiveColor else LineColor

                    drawLine(
                        color = baseLineColor.copy(alpha = lineAlpha),
                        start = Offset(pScreenX, pScreenY),
                        end = Offset(cScreenX, cScreenY),
                        strokeWidth = if (isConnectedToActive) lineStrokePx * 1.5f else lineStrokePx
                    )
                }
            }

            // Step 2: Draw circle nodes & text with distance-based transparency
            nodes.values.forEach { node ->
                val screenX = centerCanvasX + (node.x - animOffsetX)
                val screenY = centerCanvasY + (node.y - animOffsetY)

                val isActive = node.id == activeNodeId
                val isVisited = node.visited && !isActive

                val nodeDist = distanceMap[node.id] ?: 0
                val nodeAlpha = getAlphaForDistance(nodeDist)

                val bgColor: Color
                val textColor: Color

                if (isActive) {
                    bgColor = NodeActiveBackground
                    textColor = NodeActiveText
                } else if (isVisited) {
                    bgColor = NodeVisitedBackground
                    textColor = NodeVisitedText
                } else {
                    bgColor = NodeNormalBackground
                    textColor = NodeNormalText
                }

                // Draw solid node circle with distance-based opacity
                drawCircle(
                    color = bgColor.copy(alpha = nodeAlpha),
                    radius = circleRadiusPx,
                    center = Offset(screenX, screenY)
                )

                // Outer border for active/selectable nodes
                if (isActive) {
                    drawCircle(
                        color = LineActiveColor.copy(alpha = nodeAlpha),
                        radius = circleRadiusPx + 6f,
                        center = Offset(screenX, screenY),
                        style = Stroke(width = 4f)
                    )
                }

                // Draw Text symbol or number in circle
                val textLayoutResult = textMeasurer.measure(
                    text = node.value,
                    style = TextStyle(
                        color = textColor.copy(alpha = nodeAlpha),
                        fontSize = 24.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                    )
                )

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = screenX - (textLayoutResult.size.width / 2f),
                        y = screenY - (textLayoutResult.size.height / 2f)
                    )
                )
            }
        }
    }
}
