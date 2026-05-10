package de.tadris.flang

import de.tadris.flang_lib.Game
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.getNotationV1
import de.tadris.flang_lib.getNotationV2
import de.tadris.flang_lib.parseMove
import org.junit.Test
import org.junit.Assert.*

class TestFlangNotations {

    @Test
    fun testFBN2(){
        val board = Board.fromFBN("+R-4K5P1PHPUP1P3P13pp2pup-4p2frhp5k3")
        println(board)
        assertEquals(
            "R- + + + +K+ + + + + +P+ +P+H+P+U+P+ +P+ + + +P+ + + + + + + + + + + + + +p+p+ + +p+u+p- + + + +p+ + +f+r+h+p+ + + + + +k+ + + +",
            board.getFBN()
        )
    }

    @Test
    fun testCreateFBN2(){
        val board = Board.fromFBN("R- + + + +K+ + + + + +P+ +P+H+P+U+P+ +P+ + + +P+ + + + + + + + + + + + + +p+p+ + +p+u+p- + + + +p+ + +f+r+h+p+ + + + + +k+ + + +")
        println(board)
        assertEquals(
            "+R-4K5P1PHPUP1P3P13pp2pup-4p2frhp5k3",
            board.getFBN2()
        )
    }

    @Test
    fun testParsingMoveNotations(){
        val board = Board.getDefault()
        assertEquals("PC1-B2", parseMove(board, "PC1-B2").getNotationV1())
        assertEquals("PC1-B2", parseMove(board, "Pc1-b2").getNotationV1())
        assertEquals("PC1-B2", parseMove(board, "pc1b2").getNotationV1())
        assertEquals("PC1-B2", parseMove(board, "c1b2").getNotationV1())
        assertEquals("PC1-B2", parseMove(board, "b2").getNotationV1())
        assertEquals("PG2-H3", parseMove(board, "g2h3").getNotationV1())
        assertEquals("UG1-F3", parseMove(board, "Uf3").getNotationV1())
    }

    @Test
    fun testParsingFMN2(){
        val game = Game.fromFMN("g2g3 he6 Kg2 g7 Kh3 h8 Ph4 d7d6 d2c3 ua6 f2f3 a7b6 c2d3 a4 Pe3 a7 f4 c3 e3e4 ka6 Pf5 kb5 Kg4 ka4 b2 kb3 e6 kb2 Kf5 kc1")
        assertEquals("+2kRHFU8P2uP8P2P5K3p1pP4pp1ppp3f4r", game.currentState.getFBN2())
    }

    @Test
    fun testCreatingMoveNotations(){
        val board = Board.getDefault()
        assertEquals("b2", parseMove(board, "PC1-B2").getNotationV2(board))
        assertEquals("g2h3", parseMove(board, "PG2-H3").getNotationV2(board))
        assertEquals("Uf3", parseMove(board, "UG1-F3").getNotationV2(board))
    }

    @Test
    fun testCreateFMN2(){
        val board = Game.fromFMN("PG2-H3 pB7-B6 FF1-F7 hD8-F7 HE1-G2 pE7-F6 PC1-B2 pF8-G7 RD1-A1 pF6-G5 UG1-C1 pD7-E6 PB2-B3 uB8-C6 RA1-B1 pE6-F5 UC1-A3 kA8-B8 RB1-G1 fC8-D7 PC2-D3 kB8-C8 RG1-A1 rE8-E2 KH1-G1 kC8-D8 RA1-D1 kD8-E8 KG1-F1 rE2-E7 RD1-A1 pC7-D6")
        println(board)
        assertEquals(
            "g2h3 b7b6 f7 hf7 Hg2 f6 b2 g7 a1 pg5 c1 pe6 b2b3 uc6 Rb1 f5 a3 kb8 Rg1 fd7 c2d3 kc8 Ra1 e2 g1 kd8 d1 ke8 f1 re7 Ra1 pd6",
            board.getFMNv2()
        )
    }


}