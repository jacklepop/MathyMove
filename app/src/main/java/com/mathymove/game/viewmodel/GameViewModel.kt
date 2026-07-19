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
            val movesBudget = SolvabilityEngine.generateMovesBeforeCalculation(totalMoves)

            val rootId = "root_${System.currentTimeMillis()}"
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
                hasSavedGame = true
            )

            _uiState.value = newGameState
            repository.saveGame(newGameState)
        }
    }

    fun continueGame() {
        viewModelScope.launch {
            repository.savedGameState.collectLatest { saved ->
                if (saved != null) {
                    _uiState.value = saved.copy(hasSavedGame = true)
                } else {
                    startNewGame()
                }
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

        if (targetNode.type == NodeType.OPERATOR) {
            newPendingOp = targetNode.value
        } else if (targetNode.type == NodeType.NUMBER) {
            val numberVal = targetNode.value.toIntOrNull() ?: 0
            if (newPendingOp != null) {
                newCurrentValue = SolvabilityEngine.applyOp(newCurrentValue, newPendingOp, numberVal)
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

        if (newCurrentValue == currentState.targetNumber) {
            // Target Achieved! Add target value to current score
            newScore += currentState.targetNumber

            viewModelScope.launch {
                repository.addHighScore(newScore)
            }

            // Dynamically generate next goal & move budget
            newTargetNum = SolvabilityEngine.generateTargetNumber(newTotalMoves)
            newMovesBudget = SolvabilityEngine.generateMovesBeforeCalculation(newTotalMoves)
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
            isGameOver = isLost
        )

        _uiState.value = nextState

        viewModelScope.launch {
            if (isLost) {
                repository.clearSavedGame()
            } else {
                repository.saveGame(nextState)
            }
        }
    }
}
