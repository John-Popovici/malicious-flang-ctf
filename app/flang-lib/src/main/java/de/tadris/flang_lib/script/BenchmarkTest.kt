package de.tadris.flang_lib.script

import de.tadris.flang_lib.Game
import de.tadris.flang_lib.bot.CFlangSocketEngine

fun main() {
    //val bot = FastFlangBot(1, 6, threads = 1, ttSizeMB = 1024, useOpeningDatabase = false)
    val bot = CFlangSocketEngine(port = 9999, startServer = false, maxDepth = 6)
    val start = System.currentTimeMillis()
    val tests = listOf(
        "",
        "PE2-E3 pD7-E6 PC1-B2",
        "PE2-E3 pD7-E6 PC1-B2 pB7-A6 PC2-B3 hD8-C6 RD1-A1",
        "PE2-E3 pD7-E6 PC1-B2 pB7-A6 PC2-B3 hD8-C6 RD1-A1 rE8-D8",
        "PE2-E3 pD7-E6 PC1-B2 pB7-A6 PC2-B3 hD8-C6 RD1-A1 rE8-D8 PD2-C3",
        "PE2-E3 pD7-E6 PC1-B2 pB7-A6 PC2-B3 hD8-C6 RD1-A1 rE8-D8 PD2-C3 pE6-E5",
        "PE2-E3 pD7-E6 PC1-B2 pB7-A6 PC2-B3 hD8-C6 RD1-A1 rE8-D8 PD2-C3 pE6-E5 FF1-F5",
        "PE2-E3 pD7-E6 PC1-B2 pB7-A6 PC2-B3 hD8-C6 RD1-A1 rE8-D8 PD2-C3 pE6-E5 FF1-F5 pE7-E6 UG1-F3 pA6-B5 FF5-H5",
        "PE2-E3 pD7-E6 PC1-B2 pB7-A6 PC2-B3 hD8-C6 RD1-A1 rE8-D8 PD2-C3 pE6-E5 FF1-F5 pE7-E6 UG1-F3 pA6-B5 FF5-H5 pA7-A6 HE1-D3 kA8-B7 PC3-C4 pB5-C4 PB3-C4 rD8-D3 PE3-E4 hC6-D4 UF3-D3 pF7-G6 FH5-G6 pE5-E4 UD3-E4 kB7-A7 PF2-E3 uB8-B2 UE4-D4 fC8-C4 FG6-E6 uB2-D4 PE3-D4 pC7-C6 PG2-G3 kA7-B6 KH1-G2 kB6-B5 RA1-E1 kB5-B4 FE6-C6 fC4-C6 RE1-E2 kB4-C4 PD4-D5 fC6-D5 RE2-F2 kC4-D3 KG2-F3 fD5-C6 PG3-F4 pF8-G7 KF3-G3 pA6-B5 PF4-E5 kD3-E4 PH2-H3 kE4-E5 RF2-H2 kE5-D4 PH3-H4 kD4-E3 KG3-H3 pB5-C4 PH4-G5 fC6-D7 KH3-G3 pC4-C3 KG3-G4 kE3-E4 KG4-G3 kE4-E3 KG3-G4 kE3-E4 KG4-G3 kE4-E3 KG3-G4 kE3-E4 KG4-G3 kE4-E3 KG3-G4 kE3-E4 KG4-G3 kE4-E3 KG3-G4 kE3-E4 KG4-G3 kE4-E3 KG3-G4 kE3-E4 KG4-G3 kE4-E3 KG3-G4",
        "Uf3 uc6 Hd3 a7b6 Kg1 ka7 Pb2 c2 Rc1 pa6 e2e3 a4 g2g3 d7e6 Kg2 d3 d3 b2 Rc2 g7 e3d4 ub5 Pc4 pc6 Kh3 uf5 a3 f7g6 f5 f5 b4 he6 e5 c6c5 a4 hg5 c5 c8 Kh4 c5 d7 h8 f4 a8 Kg3 he4 Kf3 rb8 Pf5 f2 Rf2 f5 e4 rb7 Kf5 a7 d8 kb6 b2 kb5 d5 a4 b7 a3 g3 c4 Re7 a2 Ke6",
        "Uf3 uc6 Hd3 a7b6 Kg1 ka7 Pb2 c2 Rc1 pa6 e2e3 a4 g2g3 d7e6 Kg2 d3 d3 b2 Rc2 g7 e3d4 ub5 Pc4 pc6 Kh3 uf5 a3 f7g6 f5 f5 b4 he6 e5 c6c5 a4 hg5 c5 c8 Kh4 c5 d7 h8 f4 a8 Kg3 he4 Kf3 rb8 Pf5 f2 Rf2 f5 e4 rb7 Kf5 a7 d8 kb6 b2 kb5 d5 a4 b7 a3",
        "Uf3 uc6 Hd3 a7b6 Kg1 ka7 Pb2 c2 Rc1 pa6 e2e3 a4 g2g3 d7e6 Kg2 d3 d3 b2 Rc2 g7 e3d4 ub5 Pc4 pc6 Kh3 uf5 a3 f7g6 f5 f5 b4 he6 e5 c6c5 a4 hg5 c5 c8 Kh4 c5 d7 h8 f4 a8 Kg3 he4 Kf3 rb8 Pf5 f2 Rf2 f5 e4 rb7 Kf5 a7 d8 kb6 b2",
        "Uf3 uc6 Hd3 a7b6 Kg1 ka7 Pb2 c2 Rc1 pa6 e2e3 a4 g2g3 d7e6 Kg2 d3 d3 b2 Rc2 g7 e3d4 ub5 Pc4 pc6 Kh3 uf5 a3 f7g6 f5 f5 b4 he6 e5 c6c5 a4 hg5 c5 c8 Kh4 c5 d7 h8 f4 a8 Kg3 he4 Kf3 rb8 Pf5 f2 Rf2 f5 e4 rb7",
        "Uf3 uc6 Hd3 a7b6 Kg1 ka7 Pb2 c2 Rc1 pa6 e2e3 a4 g2g3 d7e6 Kg2 d3 d3 b2 Rc2 g7 e3d4 ub5 Pc4 pc6 Kh3 uf5 a3 f7g6 f5 f5 b4 he6 e5 c6c5 a4 hg5 c5 c8 Kh4 c5 d7 h8 f4 a8 Kg3 he4 Kf3 rb8 Pf5",
        "Uf3 uc6 Hd3 a7b6 Kg1 ka7 Pb2 c2 Rc1 pa6 e2e3 a4 g2g3 d7e6 Kg2 d3 d3 b2 Rc2 g7 e3d4 ub5 Pc4 pc6 Kh3 uf5 a3 f7g6 f5 f5 b4 he6 e5 c6c5 a4 hg5 c5 c8 Kh4 c5 d7 h8 f4 a8",
        "Uf3 uc6 Hd3 a7b6 Kg1 ka7 Pb2 c2 Rc1 pa6 e2e3 a4 g2g3 d7e6 Kg2 d3 d3 b2 Rc2 g7 e3d4 ub5 Pc4 pc6 Kh3 uf5 a3 f7g6 f5 f5 b4 he6 e5 c6c5 a4 hg5 c5 c8 Kh4",
        "Uf3 uc6 Hd3 a7b6 Kg1 ka7 Pb2 c2 Rc1 pa6 e2e3 a4 g2g3 d7e6 Kg2 d3 d3 b2 Rc2 g7 e3d4 ub5 Pc4 pc6 Kh3 uf5 a3 f7g6 f5 f5 b4 he6 e5 c6c5",
        "Uf3 uc6 Hd3 a7b6 Kg1 ka7 Pb2 c2 Rc1 pa6 e2e3 a4 g2g3 d7e6 Kg2 d3 d3 b2 Rc2 g7 e3d4 ub5 Pc4 pc6 Kh3 uf5 a3 f7g6 f5",
        "Uf3 uc6 Hd3 a7b6 Kg1 ka7 Pb2 c2 Rc1 pa6 e2e3 a4 g2g3 d7e6 Kg2 d3 d3 b2 Rc2 g7 e3d4 ub5 Pc4 pc6",
        "Uf3 uc6 Hd3 a7b6 Kg1 ka7 Pb2 c2 Rc1 pa6 e2e3 a4 g2g3 d7e6 Kg2 d3 d3 b2 Rc2",
        "Uf3 uc6 Hd3 a7b6 Kg1 ka7 Pb2 c2 Rc1 pa6 e2e3 a4 g2g3 d7e6",
        "Uf3 uc6 Hd3 a7b6 Kg1 ka7 Pb2 c2 Rc1",
        "Uf3 uc6 Hd3 a7b6",
    )
    var total = 0L
    tests.forEach {
        val result = bot.findBestMoveIterative(Game.fromFMN(it).currentState)
        println(result.bestMove)
        total += result.count
    }
    val time = System.currentTimeMillis() - start
    println("Evaluated moves: " + (total / 1000) + "K")
    println("Total time ${time}ms")
}