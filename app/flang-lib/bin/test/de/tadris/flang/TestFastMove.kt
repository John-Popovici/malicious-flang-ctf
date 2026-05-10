package de.tadris.flang

import de.tadris.flang_lib.COLOR_BLACK
import de.tadris.flang_lib.COLOR_WHITE
import de.tadris.flang_lib.TYPE_FLANGER
import de.tadris.flang_lib.TYPE_HORSE
import de.tadris.flang_lib.TYPE_KING
import de.tadris.flang_lib.TYPE_PAWN
import de.tadris.flang_lib.TYPE_ROOK
import de.tadris.flang_lib.TYPE_UNI
import de.tadris.flang_lib.FROZEN_FROZEN
import de.tadris.flang_lib.FROZEN_NORMAL
import de.tadris.flang_lib.RESIGN_BLACK
import de.tadris.flang_lib.RESIGN_WHITE
import de.tadris.flang_lib.getFromIndex
import de.tadris.flang_lib.getFromPieceState
import de.tadris.flang_lib.getPreviouslyFrozenPieceIndex
import de.tadris.flang_lib.getResignColor
import de.tadris.flang_lib.getToIndex
import de.tadris.flang_lib.getToPieceState
import de.tadris.flang_lib.indexOf
import de.tadris.flang_lib.isResign
import de.tadris.flang_lib.packMove
import de.tadris.flang_lib.packPieceState
import de.tadris.flang_lib.packResignMove
import de.tadris.flang_lib.toDebugString
import org.junit.Assert.*
import org.junit.Test

class TestFastMove {

    @Test
    fun testPackMove() {
        val fromIndex = 10
        val toIndex = 20
        val fromPieceState = packPieceState(TYPE_KING, COLOR_WHITE, FROZEN_NORMAL)
        val toPieceState = packPieceState(TYPE_PAWN, COLOR_BLACK, FROZEN_FROZEN)
        val frozenIndex = 30
        
        val move = packMove(fromIndex, toIndex, fromPieceState, toPieceState, frozenIndex)
        
        assertEquals(fromIndex, move.getFromIndex())
        assertEquals(toIndex, move.getToIndex())
        assertEquals(fromPieceState, move.getFromPieceState())
        assertEquals(toPieceState, move.getToPieceState())
        assertEquals(frozenIndex, move.getPreviouslyFrozenPieceIndex())
    }

    @Test
    fun testMoveWithMaxValues() {
        val fromIndex = 255
        val toIndex = 254
        val fromPieceState = packPieceState(TYPE_FLANGER, COLOR_BLACK, FROZEN_FROZEN)
        val toPieceState = packPieceState(TYPE_UNI, COLOR_WHITE, FROZEN_NORMAL)
        val frozenIndex = 253
        
        val move = packMove(fromIndex, toIndex, fromPieceState, toPieceState, frozenIndex)
        
        assertEquals(fromIndex, move.getFromIndex())
        assertEquals(toIndex, move.getToIndex())
        assertEquals(fromPieceState, move.getFromPieceState())
        assertEquals(toPieceState, move.getToPieceState())
        assertEquals(frozenIndex, move.getPreviouslyFrozenPieceIndex())
    }

    @Test
    fun testMoveWithZeroValues() {
        val move = packMove(0, 0, 0, 0, 0)
        
        assertEquals(0, move.getFromIndex())
        assertEquals(0, move.getToIndex())
        assertEquals(0.toByte(), move.getFromPieceState())
        assertEquals(0.toByte(), move.getToPieceState())
        assertEquals(0, move.getPreviouslyFrozenPieceIndex())
    }

    @Test
    fun testMoveWithNegativeIndices() {
        val move = packMove(-1, -1, 0, 0, -1)
        
        // -1 should be stored as 255 (0xFF) in unsigned byte, but returned as -1 for frozen index
        assertEquals(255, move.getFromIndex())
        assertEquals(255, move.getToIndex())
        assertEquals(-1, move.getPreviouslyFrozenPieceIndex())
    }

    @Test
    fun testMoveIndependentFields() {
        // Test that changing one field doesn't affect others
        val baseMove = packMove(10, 20, 0, 0, 30)
        
        // Create moves with different values in each field
        val move1 = packMove(99, 20, 0, 0, 30)
        val move2 = packMove(10, 99, 0, 0, 30)
        val move3 = packMove(10, 20, 0xFF.toByte(), 0, 30)
        val move4 = packMove(10, 20, 0, 0xFF.toByte(), 30)
        val move5 = packMove(10, 20, 0, 0, 99)
        
        // Verify that only the intended field changed
        assertEquals(99, move1.getFromIndex())
        assertEquals(20, move1.getToIndex())
        
        assertEquals(10, move2.getFromIndex())
        assertEquals(99, move2.getToIndex())
        
        assertEquals(0xFF.toByte(), move3.getFromPieceState())
        assertEquals(0.toByte(), move3.getToPieceState())
        
        assertEquals(0.toByte(), move4.getFromPieceState())
        assertEquals(0xFF.toByte(), move4.getToPieceState())
        
        assertEquals(30, baseMove.getPreviouslyFrozenPieceIndex())
        assertEquals(99, move5.getPreviouslyFrozenPieceIndex())
    }

    @Test
    fun testMoveDebugString() {
        val move = packMove(
            10, 20,
            packPieceState(TYPE_KING, COLOR_WHITE, FROZEN_NORMAL),
            packPieceState(TYPE_PAWN, COLOR_BLACK, FROZEN_FROZEN),
            30
        )
        
        val debugString = move.toDebugString()
        
        assertTrue(debugString.contains("fromIndex=10"))
        assertTrue(debugString.contains("toIndex=20"))
        assertTrue(debugString.contains("previouslyFrozenPieceIndex=30"))
        assertTrue(debugString.contains("Move("))
    }

    @Test
    fun testMoveWithRealGameScenario() {
        // Simulate a real game move: white pawn from e2 to e4
        val e2Index = indexOf(4, 6) // e2 in chess notation
        val e4Index = indexOf(4, 4) // e4 in chess notation
        val whitePawn = packPieceState(TYPE_PAWN, COLOR_WHITE, FROZEN_NORMAL)
        val emptySquare = 0.toByte()
        val noFrozenPiece = -1
        
        val move = packMove(e2Index, e4Index, whitePawn, emptySquare, noFrozenPiece)
        
        assertEquals(e2Index, move.getFromIndex())
        assertEquals(e4Index, move.getToIndex())
        assertEquals(whitePawn, move.getFromPieceState())
        assertEquals(emptySquare, move.getToPieceState())
        assertEquals(-1, move.getPreviouslyFrozenPieceIndex()) // -1 should be returned as -1
    }

    @Test
    fun testMoveCapture() {
        // Test capturing move
        val attackerIndex = indexOf(3, 3)
        val victimIndex = indexOf(4, 4)
        val attacker = packPieceState(TYPE_ROOK, COLOR_WHITE, FROZEN_NORMAL)
        val victim = packPieceState(TYPE_PAWN, COLOR_BLACK, FROZEN_NORMAL)
        val noFrozenPiece = -1
        
        val move = packMove(attackerIndex, victimIndex, attacker, victim, noFrozenPiece)
        
        assertEquals(attackerIndex, move.getFromIndex())
        assertEquals(victimIndex, move.getToIndex())
        assertEquals(attacker, move.getFromPieceState())
        assertEquals(victim, move.getToPieceState())
    }

    @Test
    fun testMoveWithFrozenPiece() {
        // Test move that involves unfreezing a piece
        val moveIndex = indexOf(2, 2)
        val targetIndex = indexOf(3, 3)
        val movingPiece = packPieceState(TYPE_HORSE, COLOR_BLACK, FROZEN_NORMAL)
        val emptySquare = 0.toByte()
        val frozenPieceIndex = indexOf(5, 5)
        
        val move = packMove(moveIndex, targetIndex, movingPiece, emptySquare, frozenPieceIndex)
        
        assertEquals(moveIndex, move.getFromIndex())
        assertEquals(targetIndex, move.getToIndex())
        assertEquals(movingPiece, move.getFromPieceState())
        assertEquals(emptySquare, move.getToPieceState())
        assertEquals(frozenPieceIndex, move.getPreviouslyFrozenPieceIndex())
    }

    @Test
    fun testLongValuePreservation() {
        // Test that the Long value is preserved correctly
        val move = packMove(
            100, 150,
            packPieceState(TYPE_KING, COLOR_WHITE, FROZEN_FROZEN),
            packPieceState(TYPE_PAWN, COLOR_BLACK, FROZEN_NORMAL),
            200
        )

        // Store and retrieve the Long value
        val longValue: Long = move

        assertEquals(100, longValue.getFromIndex())
        assertEquals(150, longValue.getToIndex())
        assertEquals(200, longValue.getPreviouslyFrozenPieceIndex())
    }

    @Test
    fun testResignWhite() {
        val resignMove = RESIGN_WHITE

        assertTrue(resignMove.isResign())
        assertEquals(COLOR_WHITE, resignMove.getResignColor())
    }

    @Test
    fun testResignBlack() {
        val resignMove = RESIGN_BLACK

        assertTrue(resignMove.isResign())
        assertEquals(COLOR_BLACK, resignMove.getResignColor())
    }

    @Test
    fun testPackResignMove() {
        val whiteResign = packResignMove(COLOR_WHITE)
        val blackResign = packResignMove(COLOR_BLACK)

        assertTrue(whiteResign.isResign())
        assertEquals(COLOR_WHITE, whiteResign.getResignColor())

        assertTrue(blackResign.isResign())
        assertEquals(COLOR_BLACK, blackResign.getResignColor())
    }

    @Test
    fun testNormalMoveIsNotResign() {
        val normalMove = packMove(10, 20, 0, 0, -1)

        assertFalse(normalMove.isResign())
    }

    @Test
    fun testResignDebugString() {
        val whiteResign = RESIGN_WHITE
        val blackResign = RESIGN_BLACK

        val whiteDebugString = whiteResign.toDebugString()
        val blackDebugString = blackResign.toDebugString()

        assertTrue(whiteDebugString.contains("RESIGN"))
        assertTrue(whiteDebugString.contains("WHITE"))

        assertTrue(blackDebugString.contains("RESIGN"))
        assertTrue(blackDebugString.contains("BLACK"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetResignColorOnNormalMoveThrows() {
        val normalMove = packMove(10, 20, 0, 0, -1)
        normalMove.getResignColor() // Should throw
    }
}