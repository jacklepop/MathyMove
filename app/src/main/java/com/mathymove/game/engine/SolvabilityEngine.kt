package com.mathymove.game.engine

import com.mathymove.game.model.GameNode
import com.mathymove.game.model.NodeType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object SolvabilityEngine {

    private const val NODE_DISTANCE = 240f // Spacing between radiating nodes

    fun generateTargetNumber(totalMovesTaken: Int): Int {
        return if (totalMovesTaken >= 200) {
            Random.nextInt(100, 401)
        } else {
            Random.nextInt(1, 101)
        }
    }

    fun generateMovesBeforeCalculation(totalMovesTaken: Int): Int {
        return if (totalMovesTaken >= 200) {
            // Even number preferred so moves pair cleanly as (Operator, Number)
            Random.nextInt(5, 8) * 2 // 10 to 14 moves
        } else {
            Random.nextInt(3, 6) * 2 // 6 to 10 moves
        }
    }

    fun generateStartNumber(): Int {
        return Random.nextInt(0, 11)
    }

    /**
     * Solvability Solver: Generates a sequence of (Operator, Number) steps
     * that transforms `currentValue` into `targetNumber` in `pairsCount` steps.
     */
    fun solvePath(currentValue: Int, targetNumber: Int, pairsCount: Int): List<Pair<String, Int>> {
        val count = pairsCount.coerceAtLeast(1)
        val path = mutableListOf<Pair<String, Int>>()
        var curr = currentValue

        for (step in 0 until count - 1) {
            // Intermediate steps: choose safe random operation towards target
            val op = chooseIntermediateOp(curr, targetNumber)
            val num = chooseIntermediateNum(curr, op, targetNumber)
            curr = applyOp(curr, op, num)
            path.add(Pair(op, num))
        }

        // Final step: bridge `curr` directly to `targetNumber`
        val finalStep = bridgeToTarget(curr, targetNumber)
        path.add(finalStep)
        return path
    }

    private fun chooseIntermediateOp(curr: Int, target: Int): String {
        val ops = mutableListOf<String>()
        if (target > curr) {
            ops.add("+")
            ops.add("*")
        } else if (target < curr) {
            ops.add("-")
            ops.add("/")
        } else {
            ops.addAll(listOf("+", "-", "*", "/"))
        }
        return ops.randomOrNull() ?: "+"
    }

    private fun chooseIntermediateNum(curr: Int, op: String, target: Int): Int {
        return when (op) {
            "+" -> Random.nextInt(1, 11)
            "-" -> if (curr > 1) Random.nextInt(1, minOf(curr, 11)) else 1
            "*" -> if (curr in 1..20) Random.nextInt(2, 5) else 1
            "/" -> {
                val divisors = (1..10).filter { it != 0 && curr % it == 0 }
                if (divisors.isNotEmpty()) divisors.random() else 1
            }
            else -> 1
        }
    }

    private fun bridgeToTarget(curr: Int, target: Int): Pair<String, Int> {
        val diff = target - curr
        if (diff >= 0 && diff in 1..10) {
            return Pair("+", diff)
        }
        if (diff < 0 && -diff in 1..10) {
            return Pair("-", -diff)
        }
        if (curr != 0 && target % curr == 0) {
            val factor = target / curr
            if (factor in 1..10) {
                return Pair("*", factor)
            }
        }
        if (target != 0 && curr % target == 0) {
            val divisor = curr / target
            if (divisor in 1..10) {
                return Pair("/", divisor)
            }
        }
        // Fallback: construct exact addition / subtraction pair or single move adjustment
        val adjustedDiff = target - curr
        return if (adjustedDiff >= 0) {
            Pair("+", adjustedDiff.coerceIn(1, 10))
        } else {
            Pair("-", (-adjustedDiff).coerceIn(1, 10))
        }
    }

    fun applyOp(value: Int, op: String, number: Int): Int {
        return when (op) {
            "+" -> value + number
            "-" -> value - number
            "*" -> value * number
            "/" -> if (number != 0 && value % number == 0) value / number else value
            else -> value
        }
    }

    /**
     * Expands radiating nodes from `parentNode`.
     * Guarantees one golden branch follows `goldenPath` if available.
     * Ensures diverse non-repetitive operators across all child branches.
     */
    fun expandNodeChildren(
        parentNode: GameNode,
        existingNodes: Map<String, GameNode>,
        goldenStep: Pair<String, Int>? = null
    ): Pair<Map<String, GameNode>, List<String>> {
        val newNodes = existingNodes.toMutableMap()
        val createdChildIds = mutableListOf<String>()

        // 4 Radiating directions: 0°, 90°, 180°, 270° relative to parent's angle or absolute cardinal directions
        val baseAngles = if (parentNode.depth == 0) {
            listOf(0f, 90f, 180f, 270f)
        } else {
            val parentAngle = parentNode.directionAngle
            listOf(parentAngle - 40f, parentAngle, parentAngle + 40f)
        }

        val nextType = if (parentNode.type == NodeType.NUMBER) NodeType.OPERATOR else NodeType.NUMBER
        val goldenBranchIndex = Random.nextInt(0, baseAngles.size)

        // Prepare pool of distinct operators to prevent duplicate symbols across branches
        val availableOperators = mutableListOf("+", "-", "*", "/").shuffled().toMutableList()
        if (nextType == NodeType.OPERATOR && goldenStep != null) {
            val goldenOp = goldenStep.first
            // Ensure golden operator is prioritized at golden branch, then remove from pool
            availableOperators.remove(goldenOp)
        }

        baseAngles.forEachIndexed { index, angleDeg ->
            val rad = Math.toRadians(angleDeg.toDouble())
            val childX = parentNode.x + (NODE_DISTANCE * cos(rad)).toFloat()
            val childY = parentNode.y + (NODE_DISTANCE * sin(rad)).toFloat()

            val isGolden = (index == goldenBranchIndex) && (goldenStep != null)

            val valStr = if (nextType == NodeType.OPERATOR) {
                if (isGolden) {
                    goldenStep!!.first
                } else {
                    if (availableOperators.isNotEmpty()) availableOperators.removeAt(0) else listOf("+", "-", "*", "/").random()
                }
            } else {
                if (isGolden) goldenStep!!.second.toString() else Random.nextInt(1, 11).toString()
            }

            val childId = "node_${parentNode.depth + 1}_${System.nanoTime()}_$index"
            val childNode = GameNode(
                id = childId,
                x = childX,
                y = childY,
                value = valStr,
                type = nextType,
                depth = parentNode.depth + 1,
                visited = false,
                parentId = parentNode.id,
                childrenIds = emptyList(),
                directionAngle = angleDeg
            )

            newNodes[childId] = childNode
            createdChildIds.add(childId)
        }

        // Update parent with children IDs
        val updatedParent = parentNode.copy(childrenIds = parentNode.childrenIds + createdChildIds)
        newNodes[parentNode.id] = updatedParent

        return Pair(newNodes, createdChildIds)
    }
}
