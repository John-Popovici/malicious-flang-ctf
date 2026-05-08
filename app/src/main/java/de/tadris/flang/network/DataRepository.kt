package de.tadris.flang.network

import android.content.Context
import de.tadris.flang.network_api.FlangAPI
import de.tadris.flang.network_api.exception.ForbiddenException
import de.tadris.flang.network_api.model.DailyGameRequest
import de.tadris.flang.network_api.model.GameConfiguration
import de.tadris.flang.network_api.model.GameRequest
import de.tadris.flang.network_api.model.GameRequestResult

const val HOST = "www.tadris.de"
const val PORT = 443
const val ROOT = "api/flang"
const val SSL_ENABLED = true
const val DEBUG = false
const val LOGIN_INTERVAL = 15*60*1000

class DataRepository {

    companion object {

        private var instance: DataRepository? = null

        fun getInstance(): DataRepository {
            if(instance == null){
                instance = DataRepository()
            }
            return instance!!
        }
    }

    private var lastLogin = 0L
    private val api = FlangAPI(HOST, PORT, ROOT, SSL_ENABLED, DEBUG)

    fun credentialsAvailable(context: Context) = CredentialsStorage(context).getUsername().isNotEmpty()

    fun accessOpenAPI() = api

    fun accessRestrictedAPI(context: Context): FlangAPI {
        login(context)
        return api
    }

    fun login(context: Context, throwErrors: Boolean = false){
        synchronized(this){
            if(System.currentTimeMillis() - lastLogin > LOGIN_INTERVAL && credentialsAvailable(context)){
                try{
                    val credentialsStorage = CredentialsStorage(context)
                    api.login(credentialsStorage.getUsername(), credentialsStorage.getSessionKey())
                    lastLogin = System.currentTimeMillis()
                }catch (e: ForbiddenException){
                    e.printStackTrace()
                    if(throwErrors) throw e
                }
            }
        }
    }

    fun requestGame(context: Context, configuration: GameConfiguration): GameRequestResult {
        login(context)
        return api.requestGame(gameConfiguration = configuration)
    }

    fun acceptGame(context: Context, gameRequest: GameRequest): GameRequestResult {
        login(context)
        return api.acceptRequest(gameRequest)
    }

    fun resetLogin(){
        lastLogin = 0L
    }

    fun createDailyGameRequest(context: Context, isRated: Boolean, time: Long, range: Int): GameRequestResult {
        login(context)
        return api.createDailyGameRequest(isRated, time, range)
    }

    fun acceptDailyGameRequest(context: Context, dailyGameRequest: DailyGameRequest): GameRequestResult {
        login(context)
        return api.acceptDailyGameRequest(dailyGameRequest.id)
    }

    fun cancelDailyGameRequest(context: Context, dailyGameRequest: DailyGameRequest) {
        login(context)
        api.cancelDailyGameRequest(dailyGameRequest.id)
    }

}