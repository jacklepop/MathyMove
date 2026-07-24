package com.mathymove.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.launch
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

    val density = LocalDensity.current
    val viewConfig = LocalViewConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    val leftPaddingPx = with(density) { 20.dp.toPx() }
    val defaultRadius = 84f
    val lineStrokePx = 4.8f
    val paddingPx = with(density) { 10.dp.toPx() }

    // Dynamic sizing of the active node circle with ~10 padding around text
    val activeNodeText = activeNode?.value ?: ""
    val activeTextLayoutResult = textMeasurer.measure(
        text = activeNodeText,
        style = TextStyle(
            fontSize = 28.8.sp,
            fontWeight = FontWeight.Bold
        )
    )
    val activeTextWidth = activeTextLayoutResult.size.width.toFloat()
    val activeTextHeight = activeTextLayoutResult.size.height.toFloat()
    val reqActiveRadius = hypot(activeTextWidth / 2f, activeTextHeight / 2f) + paddingPx
    val activeNodeRadius = maxOf(defaultRadius, reqActiveRadius)
    val deltaRadius = activeNodeRadius - defaultRadius

    // Smooth panning target offset: focus on previous node if present
    val previousNode = activeNode?.parentId?.let { nodes[it] }
    val targetOffsetX = if (previousNode != null) {
        previousNode.x
    } else {
        activeNode?.x ?: 0f
    }
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

    // User drag/pan offsets for Layer 2 viewing
    val userPanX = remember { Animatable(0f) }
    val userPanY = remember { Animatable(0f) }

    // Calculate graph distances from activeNodeId via BFS for fade effects
    val distanceMap = remember(nodes, activeNodeId) {
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
            animationSpec = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            label = "alpha_$id"
        )
        animAlpha
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GreyBackground)
            .pointerInput(nodes, activeNodeId, animOffsetX, animOffsetY, deltaRadius) {
                val touchSlop = viewConfig.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var curPanX = userPanX.value
                    var curPanY = userPanY.value
                    var totalDragDist = 0f
                    val pointerId = down.id
                    val downPos = down.position

                    var pointerUp = false
                    while (!pointerUp) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                        if (change.pressed) {
                            val dragAmount = change.positionChange()
                            if (dragAmount != Offset.Zero) {
                                change.consume()
                                totalDragDist += dragAmount.getDistance()
                                curPanX += dragAmount.x
                                curPanY += dragAmount.y
                                coroutineScope.launch {
                                    userPanX.snapTo(curPanX)
                                    userPanY.snapTo(curPanY)
                                }
                            }
                        } else {
                            pointerUp = true
                        }
                    }

                    // Calculate node display positions for hit testing & rendering
                    val mainNodeScreenX = leftPaddingPx + defaultRadius
                    val centerCanvasY = size.height / 2f
                    val active = nodes[activeNodeId]

                    val dispMap = mutableMapOf<String, Offset>()
                    if (active != null) {
                        val activeBaseX = mainNodeScreenX + (active.x - animOffsetX)
                        val activeBaseY = centerCanvasY + (active.y - animOffsetY)
                        dispMap[activeNodeId] = Offset(activeBaseX, activeBaseY)

                        val queue = ArrayDeque<String>()
                        queue.add(activeNodeId)
                        val visitedBFS = mutableSetOf(activeNodeId)

                        while (queue.isNotEmpty()) {
                            val currId = queue.removeFirst()
                            val currNode = nodes[currId] ?: continue
                            val currPos = dispMap[currId] ?: continue

                            val neighbors = mutableListOf<String>()
                            currNode.parentId?.let { neighbors.add(it) }
                            neighbors.addAll(currNode.childrenIds)

                            for (nbrId in neighbors) {
                                if (nodes.containsKey(nbrId) && !visitedBFS.contains(nbrId)) {
                                    visitedBFS.add(nbrId)
                                    val nbrNode = nodes[nbrId]!!
                                    val baseVecX = nbrNode.x - currNode.x
                                    val baseVecY = nbrNode.y - currNode.y
                                    val dist = hypot(baseVecX, baseVecY)

                                    if (currId == activeNodeId && dist > 0f) {
                                        val ux = baseVecX / dist
                                        val uy = baseVecY / dist
                                        val extendedDist = dist + deltaRadius
                                        dispMap[nbrId] = Offset(activeBaseX + ux * extendedDist, activeBaseY + uy * extendedDist)
                                    } else {
                                        dispMap[nbrId] = Offset(currPos.x + baseVecX, currPos.y + baseVecY)
                                    }
                                    queue.add(nbrId)
                                }
                            }
                        }
                    }

                    // If gesture was a TAP (not a drag), process node tap event
                    if (totalDragDist < touchSlop) {
                        val panOffset = Offset(userPanX.value, userPanY.value)
                        val tappableChildNodes = activeNode?.childrenIds?.mapNotNull { nodes[it] } ?: emptyList()
                        val tappedNode = tappableChildNodes.firstOrNull { node ->
                            if (node.visited) return@firstOrNull false
                            val nodePos = dispMap[node.id] ?: return@firstOrNull false
                            val screenNodeX = nodePos.x + panOffset.x
                            val screenNodeY = nodePos.y + panOffset.y
                            hypot(downPos.x - screenNodeX, downPos.y - screenNodeY) <= defaultRadius + 18f
                        }
                        if (tappedNode != null) {
                            onNodeTapped(tappedNode.id)
                        }
                    }

                    // Smooth snap-back to default starting position of active node upon release
                    coroutineScope.launch {
                        userPanX.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    coroutineScope.launch {
                        userPanY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val mainNodeScreenX = leftPaddingPx + defaultRadius
            val centerCanvasY = size.height / 2f
            val panOffset = Offset(userPanX.value, userPanY.value)

            // Step 0: Compute display positions for all nodes with radial expansion away from active node
            val dispMap = mutableMapOf<String, Offset>()
            val active = nodes[activeNodeId]
            if (active != null) {
                val activeBaseX = mainNodeScreenX + (active.x - animOffsetX)
                val activeBaseY = centerCanvasY + (active.y - animOffsetY)
                dispMap[activeNodeId] = Offset(activeBaseX, activeBaseY)

                val queue = ArrayDeque<String>()
                queue.add(activeNodeId)
                val visitedBFS = mutableSetOf(activeNodeId)

                while (queue.isNotEmpty()) {
                    val currId = queue.removeFirst()
                    val currNode = nodes[currId] ?: continue
                    val currPos = dispMap[currId] ?: continue

                    val neighbors = mutableListOf<String>()
                    currNode.parentId?.let { neighbors.add(it) }
                    neighbors.addAll(currNode.childrenIds)

                    for (nbrId in neighbors) {
                        if (nodes.containsKey(nbrId) && !visitedBFS.contains(nbrId)) {
                            visitedBFS.add(nbrId)
                            val nbrNode = nodes[nbrId]!!
                            val baseVecX = nbrNode.x - currNode.x
                            val baseVecY = nbrNode.y - currNode.y
                            val dist = hypot(baseVecX, baseVecY)

                            if (currId == activeNodeId && dist > 0f) {
                                val ux = baseVecX / dist
                                val uy = baseVecY / dist
                                val extendedDist = dist + deltaRadius
                                dispMap[nbrId] = Offset(activeBaseX + ux * extendedDist, activeBaseY + uy * extendedDist)
                            } else {
                                dispMap[nbrId] = Offset(currPos.x + baseVecX, currPos.y + baseVecY)
                            }
                            queue.add(nbrId)
                        }
                    }
                }
            }

            // Step 1: Draw radiating connecting lines between parent and child nodes with 1.5s smooth fade
            nodes.values.forEach { node ->
                val parent = node.parentId?.let { nodes[it] }
                if (parent != null) {
                    val pPos = (dispMap[parent.id] ?: Offset.Zero) + panOffset
                    val cPos = (dispMap[node.id] ?: Offset.Zero) + panOffset

                    val pAlpha = nodeAlphaMap[parent.id] ?: 0f
                    val cAlpha = nodeAlphaMap[node.id] ?: 0f
                    val isConnectedToActive = (node.id == activeNodeId || parent.id == activeNodeId)

                    val lineAlpha = if (isConnectedToActive) 1.0f else minOf(pAlpha, cAlpha)
                    if (lineAlpha > 0.01f) {
                        val baseLineColor = if (isConnectedToActive) LineActiveColor else LineColor

                        val dx = cPos.x - pPos.x
                        val dy = cPos.y - pPos.y
                        val dist = hypot(dx, dy)

                        if (dist > 0f) {
                            val parentRadius = if (parent.id == activeNodeId) activeNodeRadius + 7.2f else defaultRadius
                            val childRadius = if (node.id == activeNodeId) activeNodeRadius + 7.2f else defaultRadius

                            if (dist > parentRadius + childRadius) {
                                val ux = dx / dist
                                val uy = dy / dist

                                val startX = pPos.x + ux * parentRadius
                                val startY = pPos.y + uy * parentRadius
                                val endX = cPos.x - ux * childRadius
                                val endY = cPos.y - uy * childRadius

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

            // Step 2: Draw nodes & text (draw older nodes first, active node next, and direct child nodes LAST on top)
            val sortedNodes = nodes.values.sortedWith(compareBy { node ->
                when {
                    activeNode?.childrenIds?.contains(node.id) == true -> 2
                    node.id == activeNodeId -> 1
                    else -> 0
                }
            })

            sortedNodes.forEach { node ->
                val nodePos = (dispMap[node.id] ?: Offset.Zero) + panOffset
                val screenX = nodePos.x
                val screenY = nodePos.y

                val isActive = node.id == activeNodeId
                val isDirectChild = activeNode?.childrenIds?.contains(node.id) == true
                val isVisited = node.visited && !isActive
                val nodeAlpha = if (isActive) 1.0f else (nodeAlphaMap[node.id] ?: 0f)
                val currentRadius = if (isActive) activeNodeRadius else defaultRadius

                if (nodeAlpha > 0.01f || isActive || isDirectChild) {
                    val bgColor: Color
                    val textColor: Color

                    if (isActive || isDirectChild) {
                        bgColor = NodeActiveBackground
                        textColor = NodeActiveText
                    } else if (isVisited) {
                        bgColor = NodeVisitedBackground
                        textColor = NodeVisitedText
                    } else {
                        bgColor = NodeNormalBackground
                        textColor = NodeNormalText
                    }

                    // Draw solid node circle
                    drawCircle(
                        color = if (isActive || isDirectChild) bgColor else bgColor.copy(alpha = nodeAlpha),
                        radius = currentRadius,
                        center = Offset(screenX, screenY)
                    )

                    // Outer border ring for active node
                    if (isActive) {
                        drawCircle(
                            color = LineActiveColor,
                            radius = currentRadius + 7.2f,
                            center = Offset(screenX, screenY),
                            style = Stroke(width = 4.8f)
                        )
                    }

                    // Draw Text symbol or number in circle
                    val textLayoutResult = if (isActive) {
                        activeTextLayoutResult
                    } else {
                        textMeasurer.measure(
                            text = node.value,
                            style = TextStyle(
                                color = if (isDirectChild) textColor else textColor.copy(alpha = nodeAlpha),
                                fontSize = 28.8.sp,
                                fontWeight = if (isDirectChild) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }

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

