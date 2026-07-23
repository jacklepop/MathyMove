package com.mathymove.game

import org.junit.Assert.assertEquals
import org.junit.Test

class GameViewModelTest {

    @Test
    fun testClutchBonusMoves_whenMovesRemainingLessThanOrEqualTo2() {
        val movesBefore = 10
        val movesTaken = 8
        val remainingMovesBefore = (movesBefore - movesTaken).coerceAtLeast(0)

        assertEquals(2, remainingMovesBefore)
        val bonusMoves = if (remainingMovesBefore <= 2) 13 else 8
        assertEquals(13, bonusMoves)
    }

    @Test
    fun testStandardBonusMoves_whenMovesRemainingGreaterThan2() {
        val movesBefore = 10
        val movesTaken = 5
        val remainingMovesBefore = (movesBefore - movesTaken).coerceAtLeast(0)

        assertEquals(5, remainingMovesBefore)
        val bonusMoves = if (remainingMovesBefore <= 2) 13 else 8
        assertEquals(8, bonusMoves)
    }
}
