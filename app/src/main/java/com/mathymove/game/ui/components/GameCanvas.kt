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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.withTransform
import com.mathymove.game.model.DroppedRemainder
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
    activeRemainder: DroppedRemainder? = null,
    onNodeTapped: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val activeNode = nodes[activeNodeId]

    // Remainder break-off animation controller (3 seconds / 3000ms)
    val remainderProgress = remember { Animatable(0f) }

    LaunchedEffect(activeRemainder?.timestamp) {
        if (activeRemainder != null) {
            remainderProgress.snapTo(0f)
            remainderProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 3000,
                    easing = LinearEasing
                )
            )
        }
    }

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
            else -> 0.0f // Fading out over 1.5s to 0% opacity
        }
    }

    // Smooth 1.5s animated opacity map for all nodes
    val nodeAlphaMap = nodes.mapValues { (id, _) ->
        val nodeDist = distanceMap[id] ?: 3
        val targetAlpha = getAlphaForDistance(nodeDist)
        val animAlpha by animateFloatAsState(
            targetValue = targetAlpha,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 1500,
                easing = androidx.compose.animation.core.LinearEasing
            ),
            label = "alpha_$id"
        )
        animAlpha
    }

    val circleRadiusPx = 84f
    val lineStrokePx = 4.8f

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
                        if (dist <= circleRadiusPx + 18f) {
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

            // Step 1: Draw radiating connecting lines between parent and child nodes with 1.5s smooth fade
            nodes.values.forEach { node ->
                val parent = node.parentId?.let { nodes[it] }
                if (parent != null) {
                    val pScreenX = centerCanvasX + (parent.x - animOffsetX)
                    val pScreenY = centerCanvasY + (parent.y - animOffsetY)
                    val cScreenX = centerCanvasX + (node.x - animOffsetX)
                    val cScreenY = centerCanvasY + (node.y - animOffsetY)

                    val pAlpha = nodeAlphaMap[parent.id] ?: 0f
                    val cAlpha = nodeAlphaMap[node.id] ?: 0f
                    val isConnectedToActive = (node.id == activeNodeId || parent.id == activeNodeId)

                    val lineAlpha = if (isConnectedToActive) 1.0f else minOf(pAlpha, cAlpha)
                    if (lineAlpha > 0.01f) {
                        val baseLineColor = if (isConnectedToActive) LineActiveColor else LineColor

                        val dx = cScreenX - pScreenX
                        val dy = cScreenY - pScreenY
                        val dist = hypot(dx, dy)

                        if (dist > 0f) {
                            val parentRadius = if (parent.id == activeNodeId) circleRadiusPx + 7.2f else circleRadiusPx
                            val childRadius = if (node.id == activeNodeId) circleRadiusPx + 7.2f else circleRadiusPx

                            if (dist > parentRadius + childRadius) {
                                val ux = dx / dist
                                val uy = dy / dist

                                val startX = pScreenX + ux * parentRadius
                                val startY = pScreenY + uy * parentRadius
                                val endX = cScreenX - ux * childRadius
                                val endY = cScreenY - uy * childRadius

                                drawLine(
                                    color = baseLineColor.copy(alpha = lineAlpha),
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = if (isConnectedToActive) lineStrokePx * 1.5f else lineStrokePx
                                )
                            }
                        }
                    }
                }
            }

            // Step 2: Draw circle nodes & text (Active circle unaffected by fade/visibility changes)
            nodes.values.forEach { node ->
                val screenX = centerCanvasX + (node.x - animOffsetX)
                val screenY = centerCanvasY + (node.y - animOffsetY)

                val isActive = node.id == activeNodeId
                val isVisited = node.visited && !isActive
                val nodeAlpha = if (isActive) 1.0f else (nodeAlphaMap[node.id] ?: 0f)

                if (nodeAlpha > 0.01f || isActive) {
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

                    // Draw solid node circle (100% solid for active node)
                    drawCircle(
                        color = if (isActive) bgColor else bgColor.copy(alpha = nodeAlpha),
                        radius = circleRadiusPx,
                        center = Offset(screenX, screenY)
                    )

                    // Outer border for active/selectable nodes (100% solid for active node)
                    if (isActive) {
                        drawCircle(
                            color = LineActiveColor,
                            radius = circleRadiusPx + 7.2f,
                            center = Offset(screenX, screenY),
                            style = Stroke(width = 4.8f)
                        )
                    }

                    // Draw Text symbol or number in circle (100% solid for active node)
                    val textLayoutResult = textMeasurer.measure(
                        text = node.value,
                        style = TextStyle(
                            color = if (isActive) textColor else textColor.copy(alpha = nodeAlpha),
                            fontSize = 28.8.sp,
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

                    // Draw Breaking-Off Remainder Animation (shrinks size to 0, rotates 90° CW, fades 100% -> 0% over 3s)
                    if (activeRemainder != null && activeRemainder.nodeId == node.id && remainderProgress.value < 1.0f) {
                        val prog = remainderProgress.value
                        val remScale = (1.0f - prog).coerceAtLeast(0.001f)
                        val remRotation = prog * 90f // Rotates 90 degrees clockwise over 3 seconds
                        val remAlpha = (1.0f - prog) * nodeAlpha
                        val remOffsetX = prog * 40f  // Break-off drift right
                        val remOffsetY = prog * 20f  // Break-off drift down

                        val mainWidth = textLayoutResult.size.width
                        val startX = screenX + (mainWidth / 2f) + 4f + remOffsetX
                        val startY = screenY + remOffsetY

                        val remTextLayout = textMeasurer.measure(
                            text = activeRemainder.text,
                            style = TextStyle(
                                color = textColor.copy(alpha = remAlpha),
                                fontSize = 26.4.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        withTransform({
                            translate(left = startX, top = startY)
                            rotate(degrees = remRotation, pivot = Offset.Zero)
                            scale(scaleX = remScale, scaleY = remScale, pivot = Offset.Zero)
                        }) {
                            drawText(
                                textLayoutResult = remTextLayout,
                                topLeft = Offset(0f, -remTextLayout.size.height / 2f)
                            )
                        }
                    }
                }
            }
        }
    }
}
