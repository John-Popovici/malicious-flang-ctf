package de.tadris.flang.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.annotation.WorkerThread
import de.tadris.flang.R
import de.tadris.flang.network.CredentialsStorage
import de.tadris.flang.network.DataRepository
import de.tadris.flang.network_api.exception.BadRequestException
import de.tadris.flang.network_api.exception.ForbiddenException
import de.tadris.flang.ui.dialog.LoadingDialogViewController
import de.tadris.flang.network_api.util.Sha256
import de.tadris.flang.ui.activity.readUsers
import de.tadris.flang.ui.activity.writeUsers
import de.tadris.flang.ui.activity.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64
import android.util.Log
import java.io.File
import de.tadris.flang.network.apiRegister
import de.tadris.flang.network.apiLogin
import de.tadris.flang.network.apiGetSalt

abstract class AuthActivity : AppCompatActivity() {

    protected suspend fun authenticate(username: String, password: String, register: Boolean){
        //val passwordHash = Sha256.getSha256(password)
        val dialog = LoadingDialogViewController(this).show()
        try{
            if(register){
                val salt = generateSalt()
                val passwordHash = hashPassword(password, salt)
                val message = processRegister(username, passwordHash, salt)
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
            val salt = processGetSalt(username)
            val passwordHash = hashPassword(password, salt)
            val message = processLogin(username, passwordHash)
            // CredentialsStorage(this).saveSession(username, session)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            dialog.hide()
            finish()
        }catch (e: Exception){
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            dialog.hide()
        }
    }

    @WorkerThread
    private suspend fun processRegister(username: String, passwordHash: String, salt: String): String = withContext(Dispatchers.IO) {
        try {
            apiRegister(username, passwordHash, salt)
        } catch (_: ForbiddenException){
            throw Exception(getString(R.string.errorCredentialsNotCorrect))
        }
    }

    @WorkerThread
    private suspend fun processLogin(username: String, passwordHash: String): String = withContext(Dispatchers.IO) {
        try {
            apiLogin(username, passwordHash)
        }catch (_: ForbiddenException){
            throw Exception(getString(R.string.errorCredentialsNotCorrect))
        }
    }

    @WorkerThread
    private suspend fun processGetSalt(username: String): String = withContext(Dispatchers.IO) {
        try {
            apiGetSalt(username)
        }catch (_: ForbiddenException){
            throw Exception("Salt not retrieved")
        }
    }

    private fun generateSalt(): String {
        /*
        Generates 16 char random salt
         */
        val bytes = ByteArray(16)
        // Gets a secure random number generator
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hashPassword(password: String, salt: String): String {
        // Sha256 is very fast, so easy to brute force, we can
        // switch this to a slower hash function if we want to make
        // it a bit harder
        return Sha256.getSha256(password+salt)
    }

}