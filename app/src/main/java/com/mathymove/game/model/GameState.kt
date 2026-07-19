package com.mathymove.game.model

import kotlinx.serialization.Serializable

@Serializable
data class DroppedRemainder(
    val text: String,
    val nodeId: String,
    val timestamp: Long
)

@Serializable
data class GameState(
    val startNumber: Int = 0,
    val currentValue: Int = 0,
    val pendingOperator: String? = null,
    val targetNumber: Int = 0,
    val movesBeforeCalculation: Int = 5,
    val movesTakenForTarget: Int = 0,
    val totalMovesTaken: Int = 0,
    val score: Int = 0,
    val activeNodeId: String = "root",
    val nodes: Map<String, GameNode> = emptyMap(),
    val isGameOver: Boolean = false,
    val hasSavedGame: Boolean = false,
    val highScores: List<HighScoreEntry> = emptyList(),
    val activeRemainder: DroppedRemainder? = null,
    val gameTimestamp: Long = 0L
) {
    val movesRemainingForTarget: Int
        get() = (movesBeforeCalculation - movesTakenForTarget).coerceAtLeast(0)

    val activeNode: GameNode?
        get() = nodes[activeNodeId]
}
