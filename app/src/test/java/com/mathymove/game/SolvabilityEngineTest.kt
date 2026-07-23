package com.mathymove.game

import com.mathymove.game.engine.SolvabilityEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolvabilityEngineTest {

    @Test
    fun testGenerateTargetNumber_under200Moves() {
        val target = SolvabilityEngine.generateTargetNumber(50)
        assertTrue("Target should be between 1 and 100", target in 1..100)
    }

    @Test
    fun testGenerateTargetNumber_over200Moves() {
        val target = SolvabilityEngine.generateTargetNumber(205)
        assertTrue("Target should be between 100 and 400", target in 100..400)
    }

    @Test
    fun testGenerateMovesBeforeCalculation_under200Moves() {
        val budget = SolvabilityEngine.generateMovesBeforeCalculation(50)
        assertTrue("Moves budget should be between 5 and 10", budget in 5..10)
    }

    @Test
    fun testGenerateMovesBeforeCalculation_over200Moves() {
        val budget = SolvabilityEngine.generateMovesBeforeCalculation(250)
        assertTrue("Moves budget should be between 10 and 15", budget in 10..15)
    }

    @Test
    fun testSolvePath_reachesTarget() {
        val startVal = 5
        val target = 42
        val pairsCount = 3

        val path = SolvabilityEngine.solvePath(startVal, target, pairsCount)

        var curr = startVal
        for ((op, num) in path) {
            curr = SolvabilityEngine.applyOp(curr, op, num)
        }

        assertEquals("Path evaluation must reach target number", target, curr)
    }

    @Test
    fun testApplyOp_multiplicationAndDivision() {
        assertEquals(15, SolvabilityEngine.applyOp(5, "x", 3))
        assertEquals(4, SolvabilityEngine.applyOp(12, "÷", 3))
    }

    @Test
    fun testExpandNodeChildren_radiatesTopDirectionAtFixedAngles() {
        val rootNode = com.mathymove.game.model.GameNode(
            id = "root",
            x = 0f,
            y = 0f,
            value = "5",
            type = com.mathymove.game.model.NodeType.NUMBER,
            depth = 0,
            visited = true
        )

        val (nodes, childIds) = SolvabilityEngine.expandNodeChildren(
            parentNode = rootNode,
            existingNodes = mapOf("root" to rootNode)
        )

        assertEquals("Should create 3 child nodes", 3, childIds.size)

        val expectedAngles = listOf(-150f, -90f, -30f)
        childIds.forEachIndexed { index, childId ->
            val child = nodes[childId]!!
            assertEquals("Child $index direction angle should match fixed angle", expectedAngles[index], child.directionAngle)
            assertTrue("Child $index Y position should be smaller (above parent)", child.y < rootNode.y)
        }
    }

    @Test
    fun testExpandNodeChildren_directionalFanOutAtDepthGreaterThanZero() {
        val farLeftChild = com.mathymove.game.model.GameNode(
            id = "far_left",
            x = -299.6f,
            y = -173f,
            value = "+",
            type = com.mathymove.game.model.NodeType.OPERATOR,
            depth = 1,
            visited = true,
            directionAngle = -150f
        )

        val (nodes, childIds) = SolvabilityEngine.expandNodeChildren(
            parentNode = farLeftChild,
            existingNodes = mapOf("far_left" to farLeftChild)
        )

        val expectedAngles = listOf(-210f, -150f, -90f)
        childIds.forEachIndexed { index, childId ->
            val child = nodes[childId]!!
            assertEquals("Child $index angle should maintain parent direction fan-out", expectedAngles[index], child.directionAngle)
            assertTrue("Child $index X position should not overlap center (0.0f)", kotlin.math.abs(child.x) > 10f)
        }
    }
}
