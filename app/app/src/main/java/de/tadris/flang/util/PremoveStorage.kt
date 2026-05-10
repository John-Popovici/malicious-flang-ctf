package de.tadris.flang.util

import de.tadris.flang.network_api.model.Premove

class PremoveStorage {

    private val currentPremoves = mutableListOf<Premove>()

    fun onPremovesLoaded(premoves: List<Premove>){
        currentPremoves.clear()
        currentPremoves.addAll(premoves)
    }

    fun addPremove(premove: Premove){
        currentPremoves += premove
    }

    fun hasPremoves(): Boolean {
        return currentPremoves.isNotEmpty()
    }

    fun clear() {
        currentPremoves.clear()
    }

    fun getVisiblePremove(): Premove? {
        return currentPremoves.firstOrNull()
    }

}