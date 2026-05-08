package de.tadris.flang

import de.tadris.flang_lib.COLOR_BLACK
import de.tadris.flang_lib.COLOR_WHITE
import de.tadris.flang_lib.TYPE_FLANGER
import de.tadris.flang_lib.TYPE_HORSE
import de.tadris.flang_lib.TYPE_KING
import de.tadris.flang_lib.TYPE_NONE
import de.tadris.flang_lib.TYPE_PAWN
import de.tadris.flang_lib.TYPE_ROOK
import de.tadris.flang_lib.TYPE_UNI
import de.tadris.flang_lib.FROZEN_FROZEN
import de.tadris.flang_lib.FROZEN_NORMAL
import de.tadris.flang_lib.FastType
import de.tadris.flang_lib.PieceState
import de.tadris.flang_lib.getColor
import de.tadris.flang_lib.getFrozen
import de.tadris.flang_lib.getType
import de.tadris.flang_lib.packPieceState
import org.junit.Assert.*
import org.junit.Test

class TestFastPieceState {

    @Test
    fun testPackPieceState() {
        val pieceState = packPieceState(TYPE_KING, COLOR_WHITE, FROZEN_NORMAL)
        
        assertEquals(TYPE_KING, pieceState.getType())
        assertEquals(COLOR_WHITE, pieceState.getColor())
        assertEquals(FROZEN_NORMAL, pieceState.getFrozen())
    }

    @Test
    fun testAllPieceTypes() {
        val types = arrayOf(
            TYPE_NONE,
            TYPE_KING,
            TYPE_PAWN,
            TYPE_HORSE,
            TYPE_ROOK,
            TYPE_UNI,
            TYPE_FLANGER
        )
        
        for (type in types) {
            val pieceState = packPieceState(type, COLOR_WHITE, FROZEN_NORMAL)
            assertEquals(type, pieceState.getType())
            assertEquals(COLOR_WHITE, pieceState.getColor())
            assertEquals(FROZEN_NORMAL, pieceState.getFrozen())
        }
    }

    @Test
    fun testBothColors() {
        val whiteKing = packPieceState(TYPE_KING, COLOR_WHITE, FROZEN_NORMAL)
        val blackKing = packPieceState(TYPE_KING, COLOR_BLACK, FROZEN_NORMAL)
        
        assertEquals(COLOR_WHITE, whiteKing.getColor())
        assertEquals(COLOR_BLACK, blackKing.getColor())
        assertEquals(TYPE_KING, whiteKing.getType())
        assertEquals(TYPE_KING, blackKing.getType())
    }

    @Test
    fun testFrozenStates() {
        val normalPiece = packPieceState(TYPE_KING, COLOR_WHITE, FROZEN_NORMAL)
        val frozenPiece = packPieceState(TYPE_KING, COLOR_WHITE, FROZEN_FROZEN)
        
        assertEquals(FROZEN_NORMAL, normalPiece.getFrozen())
        assertEquals(FROZEN_FROZEN, frozenPiece.getFrozen())
        assertEquals(TYPE_KING, normalPiece.getType())
        assertEquals(TYPE_KING, frozenPiece.getType())
        assertEquals(COLOR_WHITE, normalPiece.getColor())
        assertEquals(COLOR_WHITE, frozenPiece.getColor())
    }

    @Test
    fun testAllCombinations() {
        val types = arrayOf(
            TYPE_NONE,
            TYPE_KING,
            TYPE_PAWN,
            TYPE_HORSE,
            TYPE_ROOK,
            TYPE_UNI,
            TYPE_FLANGER
        )
        val colors = arrayOf(COLOR_WHITE, COLOR_BLACK)
        val frozenStates = arrayOf(FROZEN_NORMAL, FROZEN_FROZEN)
        
        for (type in types) {
            for (color in colors) {
                for (frozen in frozenStates) {
                    val pieceState = packPieceState(type, color, frozen)
                    assertEquals("Type mismatch for $type, $color, $frozen", type, pieceState.getType())
                    assertEquals("Color mismatch for $type, $color, $frozen", color, pieceState.getColor())
                    assertEquals("Frozen mismatch for $type, $color, $frozen", frozen, pieceState.getFrozen())
                }
            }
        }
    }

    @Test
    fun testZeroPieceState() {
        val emptyState: PieceState = 0
        
        assertEquals(TYPE_NONE, emptyState.getType())
        assertEquals(COLOR_BLACK, emptyState.getColor()) // false = BLACK
        assertEquals(FROZEN_NORMAL, emptyState.getFrozen()) // false = NORMAL
    }

    @Test
    fun testMaximumValues() {
        // Test with maximum type value (7)
        val maxType: FastType = 7
        val pieceState = packPieceState(maxType, COLOR_WHITE, FROZEN_FROZEN)
        
        assertEquals(maxType, pieceState.getType())
        assertEquals(COLOR_WHITE, pieceState.getColor())
        assertEquals(FROZEN_FROZEN, pieceState.getFrozen())
    }

    @Test
    fun testInvalidTypeThrowsException() {
        try {
            packPieceState(8, COLOR_WHITE, FROZEN_NORMAL) // 8 is outside valid range (0-7)
            fail("Should throw IllegalArgumentException for type > 7")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Type must be between 0 and 7"))
        }
        
        try {
            packPieceState(-1, COLOR_WHITE, FROZEN_NORMAL) // -1 is outside valid range (0-7)
            fail("Should throw IllegalArgumentException for type < 0")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Type must be between 0 and 7"))
        }
    }

    @Test
    fun testBitMasking() {
        // Test that bits don't interfere with each other
        val whiteFrozenKing = packPieceState(TYPE_KING, COLOR_WHITE, FROZEN_FROZEN)
        val blackNormalKing = packPieceState(TYPE_KING, COLOR_BLACK, FROZEN_NORMAL)
        
        // Both should have the same type but different color and frozen state
        assertEquals(TYPE_KING, whiteFrozenKing.getType())
        assertEquals(TYPE_KING, blackNormalKing.getType())
        assertEquals(COLOR_WHITE, whiteFrozenKing.getColor())
        assertEquals(COLOR_BLACK, blackNormalKing.getColor())
        assertEquals(FROZEN_FROZEN, whiteFrozenKing.getFrozen())
        assertEquals(FROZEN_NORMAL, blackNormalKing.getFrozen())
    }

    @Test
    fun testPieceStateIndependence() {
        // Test that different piece states don't interfere
        val pawn = packPieceState(TYPE_PAWN, COLOR_WHITE, FROZEN_NORMAL)
        val king = packPieceState(TYPE_KING, COLOR_BLACK, FROZEN_FROZEN)
        
        // Each should maintain its own values
        assertEquals(TYPE_PAWN, pawn.getType())
        assertEquals(COLOR_WHITE, pawn.getColor())
        assertEquals(FROZEN_NORMAL, pawn.getFrozen())
        
        assertEquals(TYPE_KING, king.getType())
        assertEquals(COLOR_BLACK, king.getColor())
        assertEquals(FROZEN_FROZEN, king.getFrozen())
    }

    @Test
    fun testByteValueRange() {
        // Test that byte values are in expected range
        val maxPiece = packPieceState(TYPE_FLANGER, COLOR_WHITE, FROZEN_FROZEN)
        
        // Should be a valid byte value
        assertTrue(maxPiece >= Byte.MIN_VALUE)
        assertTrue(maxPiece <= Byte.MAX_VALUE)
        
        // Should preserve all components
        assertEquals(TYPE_FLANGER, maxPiece.getType())
        assertEquals(COLOR_WHITE, maxPiece.getColor())
        assertEquals(FROZEN_FROZEN, maxPiece.getFrozen())
    }

    @Test
    fun testCommonGamePieces() {
        // Test creating common game pieces
        val whitePawn = packPieceState(TYPE_PAWN, COLOR_WHITE, FROZEN_NORMAL)
        val blackPawn = packPieceState(TYPE_PAWN, COLOR_BLACK, FROZEN_NORMAL)
        val whiteKing = packPieceState(TYPE_KING, COLOR_WHITE, FROZEN_NORMAL)
        val blackKing = packPieceState(TYPE_KING, COLOR_BLACK, FROZEN_NORMAL)
        val frozenRook = packPieceState(TYPE_ROOK, COLOR_WHITE, FROZEN_FROZEN)
        
        // Verify all pieces are created correctly
        assertEquals(TYPE_PAWN, whitePawn.getType())
        assertEquals(COLOR_WHITE, whitePawn.getColor())
        assertEquals(FROZEN_NORMAL, whitePawn.getFrozen())
        
        assertEquals(TYPE_PAWN, blackPawn.getType())
        assertEquals(COLOR_BLACK, blackPawn.getColor())
        assertEquals(FROZEN_NORMAL, blackPawn.getFrozen())
        
        assertEquals(TYPE_KING, whiteKing.getType())
        assertEquals(COLOR_WHITE, whiteKing.getColor())
        assertEquals(FROZEN_NORMAL, whiteKing.getFrozen())
        
        assertEquals(TYPE_KING, blackKing.getType())
        assertEquals(COLOR_BLACK, blackKing.getColor())
        assertEquals(FROZEN_NORMAL, blackKing.getFrozen())
        
        assertEquals(TYPE_ROOK, frozenRook.getType())
        assertEquals(COLOR_WHITE, frozenRook.getColor())
        assertEquals(FROZEN_FROZEN, frozenRook.getFrozen())
    }
}