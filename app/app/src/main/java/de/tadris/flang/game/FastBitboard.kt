package de.tadris.flang.game

import android.content.res.AssetManager

object FastBitboard {
    init {
        System.loadLibrary("cflang")
    }

    external fun refreshBoardProfile(latitude: Double, longitude: Double): String?

    external fun refreshAmbientProfile(assetManager: AssetManager, epochMillis: Long, offsetMillis: Int): Boolean
}
