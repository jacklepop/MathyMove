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
            ops.add("x")
        } else if (target < curr) {
            ops.add("-")
            ops.add("÷")
        } else {
            ops.addAll(listOf("+", "-", "x", "÷"))
        }
        return ops.randomOrNull() ?: "+"
    }

    private fun chooseIntermediateNum(curr: Int, op: String, target: Int): Int {
        return when (op) {
            "+" -> Random.nextInt(1, 11)
            "-" -> if (curr > 1) Random.nextInt(1, minOf(curr, 11)) else 1
            "x", "*" -> if (curr in 1..20) Random.nextInt(2, 5) else 1
            "÷", "/" -> {
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
                return Pair("x", factor)
            }
        }
        if (target != 0 && curr % target == 0) {
            val divisor = curr / target
            if (divisor in 1..10) {
                return Pair("÷", divisor)
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
            "x", "*", "×" -> value * number
            "÷", "/" -> if (number != 0 && value % number == 0) value / number else value
            else -> value
        }
    }

    /**
     * Expands radiating nodes from `parentNode`.
     * Radiates 4 lines at every depth layer.
     * Guarantees one golden branch follows `goldenPath` if available.
     * Ensures diverse non-repetitive operators across all 4 child branches.
     * Enforces strict whole-number division with no remainder/decimals.
     */
    fun expandNodeChildren(
        parentNode: GameNode,
        existingNodes: Map<String, GameNode>,
        goldenStep: Pair<String, Int>? = null,
        currentValue: Int = 0
    ): Pair<Map<String, GameNode>, List<String>> {
        val newNodes = existingNodes.toMutableMap()
        val createdChildIds = mutableListOf<String>()

        // 3 Radiating directions at EVERY depth layer
        val baseAngles = if (parentNode.depth == 0) {
            listOf(0f, 120f, 240f)
        } else {
            val parentAngle = parentNode.directionAngle
            listOf(parentAngle - 40f, parentAngle, parentAngle + 40f)
        }

        val nextType = if (parentNode.type == NodeType.NUMBER) NodeType.OPERATOR else NodeType.NUMBER
        val goldenBranchIndex = Random.nextInt(0, baseAngles.size)

        // Check if currentValue can be cleanly divided by any integer in 2..10
        val canDivideCleanly = (currentValue == 0) || (2..10).any { it != 0 && currentValue % it == 0 }

        // Prepare pool of distinct operators (+, -, x, ÷) to ensure no duplicate operators across the 3 branches
        val availableOperators = mutableListOf("+", "-", "x", "÷")
        if (!canDivideCleanly) {
            availableOperators.remove("÷") // Do not present division if clean division is impossible
        }
        availableOperators.shuffle()

        if (nextType == NodeType.OPERATOR && goldenStep != null) {
            val goldenOp = goldenStep.first
            availableOperators.remove(goldenOp)
        }

        // Prepare pool of distinct numbers to ensure NO duplicate numbers across the 3 child branches
        val availableNumbers = if (parentNode.type == NodeType.OPERATOR && (parentNode.value == "÷" || parentNode.value == "/")) {
            val cleanDivs = (1..10).filter { it != 0 && currentValue % it == 0 }.toMutableList()
            if (cleanDivs.size < 3) {
                val extraDivs = (11..30).filter { it != 0 && currentValue % it == 0 }
                cleanDivs.addAll(extraDivs)
            }
            cleanDivs.distinct().shuffled().toMutableList()
        } else {
            (1..10).shuffled().toMutableList()
        }

        if (nextType == NodeType.NUMBER && goldenStep != null) {
            val goldenNum = goldenStep.second
            availableNumbers.remove(goldenNum)
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
                    if (availableOperators.isNotEmpty()) availableOperators.removeAt(0) else listOf("+", "-", "x").random()
                }
            } else {
                if (isGolden) {
                    goldenStep!!.second.toString()
                } else {
                    if (availableNumbers.isNotEmpty()) availableNumbers.removeAt(0).toString() else Random.nextInt(1, 11).toString()
                }
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

    /**
     * Tree Pruning and Distance Calculator:
     * Calculates graph distances from `activeNodeId` via BFS.
     * Retains nodes up to 3 levels away so nodes moving to level 3 can smoothly fade out over 1.5 seconds.
     * Prunes (deletes) nodes 4 or more levels away from memory and state.
     */
    fun pruneAndCalculateDistances(
        nodes: Map<String, GameNode>,
        activeNodeId: String
    ): Pair<Map<String, GameNode>, Map<String, Int>> {
        val distances = mutableMapOf<String, Int>()
        val queue = ArrayDeque<String>()

        if (nodes.containsKey(activeNodeId)) {
            distances[activeNodeId] = 0
            queue.add(activeNodeId)
        }

        while (queue.isNotEmpty()) {
            val currId = queue.removeFirst()
            val currDist = distances[currId] ?: 0

            if (currDist < 4) {
                val node = nodes[currId] ?: continue
                val neighbors = mutableListOf<String>()
                node.parentId?.let { neighbors.add(it) }
                neighbors.addAll(node.childrenIds)

                for (nbrId in neighbors) {
                    if (nodes.containsKey(nbrId) && !distances.containsKey(nbrId)) {
                        distances[nbrId] = currDist + 1
                        queue.add(nbrId)
                    }
                }
            }
        }

        // Keep nodes up to distance 3 (0=active, 1=100%/70%, 2=40%, 3=fading to 0% in 1.5s)
        val prunedNodes = nodes.filterKeys { id ->
            val dist = distances[id]
            dist != null && dist <= 3
        }.toMutableMap()

        // Clean up references to pruned nodes in parentId & childrenIds
        val cleanedNodes = prunedNodes.mapValues { (_, node) ->
            node.copy(
                childrenIds = node.childrenIds.filter { prunedNodes.containsKey(it) },
                parentId = if (node.parentId != null && prunedNodes.containsKey(node.parentId)) node.parentId else null
            )
        }

        return Pair(cleanedNodes, distances)
    }
}
