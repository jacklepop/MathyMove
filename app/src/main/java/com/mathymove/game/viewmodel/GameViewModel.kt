package com.mathymove.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mathymove.game.data.GameRepository
import com.mathymove.game.engine.SolvabilityEngine
import com.mathymove.game.model.GameNode
import com.mathymove.game.model.GameState
import com.mathymove.game.model.NodeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(application)

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.savedGameState.collectLatest { saved ->
                _uiState.update { current ->
                    current.copy(hasSavedGame = saved != null)
                }
            }
        }
        viewModelScope.launch {
            repository.highScores.collectLatest { list ->
                _uiState.update { current ->
                    current.copy(highScores = list)
                }
            }
        }
    }

    fun startNewGame() {
        viewModelScope.launch {
            val startNum = SolvabilityEngine.generateStartNumber()
            val totalMoves = 0
            val targetNum = SolvabilityEngine.generateTargetNumber(totalMoves)
            val movesBudget = 14
            val now = System.currentTimeMillis()

            val rootId = "root_$now"
            val rootNode = GameNode(
                id = rootId,
                x = 0f,
                y = 0f,
                value = startNum.toString(),
                type = NodeType.NUMBER,
                depth = 0,
                visited = true,
                parentId = null,
                childrenIds = emptyList()
            )

            val initialNodes = mutableMapOf(rootId to rootNode)

            // Solve golden path for initial setup
            val goldenPath = SolvabilityEngine.solvePath(startNum, targetNum, movesBudget / 2)
            val firstStep = goldenPath.firstOrNull()

            val (expandedNodes, _) = SolvabilityEngine.expandNodeChildren(
                parentNode = rootNode,
                existingNodes = initialNodes,
                goldenStep = firstStep,
                currentValue = startNum
            )

            val (prunedNodes, _) = SolvabilityEngine.pruneAndCalculateDistances(expandedNodes, rootId)

            val newGameState = GameState(
                startNumber = startNum,
                currentValue = startNum,
                pendingOperator = null,
                targetNumber = targetNum,
                movesBeforeCalculation = movesBudget,
                movesTakenForTarget = 0,
                totalMovesTaken = 0,
                activeNodeId = rootId,
                nodes = prunedNodes,
                isGameOver = false,
                hasSavedGame = true,
                gameTimestamp = now
            )

            _uiState.update { current ->
                newGameState.copy(highScores = current.highScores)
            }
            repository.saveGame(newGameState)
        }
    }

    fun continueGame() {
        viewModelScope.launch {
            val saved = repository.savedGameState.firstOrNull()
            if (saved != null) {
                _uiState.update { current ->
                    saved.copy(
                        hasSavedGame = true,
                        highScores = current.highScores
                    )
                }
            } else {
                startNewGame()
            }
        }
    }

    fun onNodeTapped(nodeId: String) {
        val currentState = _uiState.value
        if (currentState.isGameOver) return

        val targetNode = currentState.nodes[nodeId] ?: return
        if (targetNode.visited) return // Greyed out circle, tapping does nothing

        val activeNode = currentState.nodes[currentState.activeNodeId] ?: return

        // Verify adjacency: node must be a child of active node or connected parent
        if (targetNode.parentId != activeNode.id && !activeNode.childrenIds.contains(nodeId)) {
            return
        }

        val newTotalMoves = currentState.totalMovesTaken + 1
        val newMovesTakenForTarget = currentState.movesTakenForTarget + 1

        var newCurrentValue = currentState.currentValue
        var newPendingOp = currentState.pendingOperator
        val updatedNodes = currentState.nodes.toMutableMap()
        var remainderObj: com.mathymove.game.model.DroppedRemainder? = null

        if (targetNode.type == NodeType.OPERATOR) {
            newPendingOp = targetNode.value
        } else if (targetNode.type == NodeType.NUMBER) {
            val numberVal = targetNode.value.toIntOrNull() ?: 0
            if (newPendingOp != null) {
                val (calcVal, remainderText) = SolvabilityEngine.applyOpWithRemainder(newCurrentValue, newPendingOp, numberVal)
                newCurrentValue = calcVal
                if (remainderText != null) {
                    remainderObj = com.mathymove.game.model.DroppedRemainder(
                        text = remainderText,
                        nodeId = nodeId,
                        timestamp = System.currentTimeMillis()
                    )
                }
                newPendingOp = null
            } else {
                newCurrentValue = numberVal
            }
        }

        // Mark tapped node as visited and update number circle value to calculated result
        val updatedTargetNode = if (targetNode.type == NodeType.NUMBER) {
            targetNode.copy(visited = true, value = newCurrentValue.toString())
        } else {
            targetNode.copy(visited = true)
        }
        updatedNodes[nodeId] = updatedTargetNode

        // Expand children if this node has no children generated yet
        if (updatedTargetNode.childrenIds.isEmpty()) {
            val movesRemaining = (currentState.movesBeforeCalculation - newMovesTakenForTarget).coerceAtLeast(0)
            val goldenPath = SolvabilityEngine.solvePath(newCurrentValue, currentState.targetNumber, movesRemaining / 2)
            val nextGoldenStep = goldenPath.firstOrNull()

            val (nodesWithChildren, _) = SolvabilityEngine.expandNodeChildren(
                parentNode = updatedTargetNode,
                existingNodes = updatedNodes,
                goldenStep = nextGoldenStep,
                currentValue = newCurrentValue
            )
            updatedNodes.clear()
            updatedNodes.putAll(nodesWithChildren)
        }

        // Check Target Win Condition & Score Update
        var newTargetNum = currentState.targetNumber
        var newMovesBudget = currentState.movesBeforeCalculation
        var resetMovesTaken = newMovesTakenForTarget
        var newScore = currentState.score
        val gameTimestamp = if (currentState.gameTimestamp == 0L) System.currentTimeMillis() else currentState.gameTimestamp

        var clutchTimestamp = currentState.clutchBonusTimestamp

        if (newCurrentValue == currentState.targetNumber) {
            // Target Achieved! Add target value to current score
            newScore += currentState.targetNumber

            viewModelScope.launch {
                repository.saveHighScore(gameTimestamp, newScore)
            }

            // Dynamically generate next goal number
            newTargetNum = SolvabilityEngine.generateTargetNumber(newTotalMoves)

            // Retain remaining moves before this step; add 13 moves if remaining moves <= 2, else 8
            val remainingMovesBefore = (currentState.movesBeforeCalculation - currentState.movesTakenForTarget).coerceAtLeast(0)
            val bonusMoves = if (remainingMovesBefore <= 2) {
                clutchTimestamp = System.currentTimeMillis()
                13
            } else {
                8
            }
            newMovesBudget = remainingMovesBefore + bonusMoves
            resetMovesTaken = 0
        }

        val movesRemainingAfter = (newMovesBudget - resetMovesTaken).coerceAtLeast(0)
        val isLost = movesRemainingAfter == 0 && newCurrentValue != newTargetNum

        // Apply tree pruning from current active nodeId
        val (finalPrunedNodes, _) = SolvabilityEngine.pruneAndCalculateDistances(updatedNodes, nodeId)

        val nextState = currentState.copy(
            currentValue = newCurrentValue,
            pendingOperator = newPendingOp,
            targetNumber = newTargetNum,
            movesBeforeCalculation = newMovesBudget,
            movesTakenForTarget = resetMovesTaken,
            totalMovesTaken = newTotalMoves,
            score = newScore,
            activeNodeId = nodeId,
            nodes = finalPrunedNodes,
            isGameOver = isLost,
            activeRemainder = remainderObj,
            gameTimestamp = gameTimestamp,
            clutchBonusTimestamp = clutchTimestamp
        )

        _uiState.update { current ->
            nextState.copy(highScores = current.highScores)
        }

        viewModelScope.launch {
            if (isLost) {
                repository.saveHighScore(gameTimestamp, newScore)
                repository.clearSavedGame()
            } else {
                repository.saveGame(nextState)
            }
        }
    }
}
