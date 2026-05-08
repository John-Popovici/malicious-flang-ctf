package de.tadris.flang.network

import android.content.Context
import de.tadris.flang.network_api.model.Session
import androidx.core.content.edit

class CredentialsStorage(context: Context) {

    companion object {
        const val ROLE_USER = "USER"
        const val ROLE_ADMIN = "ADMIN"
    }

    private val prefs = context.getSharedPreferences("credentials", Context.MODE_PRIVATE)

    fun getUsername() = prefs.getString("username", "")!!

    fun getSessionKey() = prefs.getString("key", "")!!

    fun getRole() = prefs.getString("role", ROLE_USER)!!

    fun saveSession(username: String, session: Session){
        prefs.edit { putString("username", username).putString("key", session.key) }
    }

    fun saveRole(role: String){
        prefs.edit { putString("role", role) }
    }

    fun clear(){
        prefs.edit { clear() }
    }

}