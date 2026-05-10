package de.tadris.flang.util

import android.content.Context
import de.tadris.flang.network.DataRepository
import de.tadris.flang.network_api.model.Premove
import de.tadris.flang_lib.getNotationV1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConditionalPremoves(
    private val context: Context,
    private val gameId: Long,
    private val gameFMN: String,
    private val gameMoveCount: Int,
    private val onUpdate: (List<Premove>) -> Unit
) {

    private val api get() = DataRepository.getInstance().accessRestrictedAPI(context)

    private var premovesLoaded = false
    private val premoves = mutableListOf<Premove>()

    suspend fun fetchPremoves() {
        if(!premovesLoaded){
            val premoves = withContext(Dispatchers.IO){ api.getPremoves(gameId) }
            val removablePremoves = premoves.filter { it.moveCount < gameMoveCount || (it.fmnCondition != null && !it.fmnCondition!!.contains(gameFMN)) }
            val displayedPremoves = premoves.filter { it !in removablePremoves }
            this.premoves.addAll(displayedPremoves)
            onUpdate()

            removablePremoves.forEach {
                withContext(Dispatchers.IO){ api.removePremove(it.id) }
            }
        }
        onUpdate()
    }

    suspend fun addPremove(premove: Premove){
        premoves.add(premove)
        val index = premoves.lastIndex
        onUpdate()
        try {
            val result = withContext(Dispatchers.IO){ api.addPremove(gameId, premove.moveCount, premove.move.getNotationV1(), premove.fmnCondition) }
            premoves[index] = premove.copy(id = result.id)
            onUpdate()
        }catch (e: Exception){
            e.printStackTrace()
            premoves.remove(premove)
            onUpdate()
        }
    }

    suspend fun removePremove(premove: Premove){
        premoves.remove(premove)
        onUpdate()
        try {
            withContext(Dispatchers.IO){ api.removePremove(premove.id) }
        }catch (e: Exception){
            e.printStackTrace()
            premoves.remove(premove)
            onUpdate()
        }
    }

    private fun onUpdate(){
        onUpdate(premoves)
    }

}