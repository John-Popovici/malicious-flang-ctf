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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception

abstract class AuthActivity : AppCompatActivity() {

    protected suspend fun authenticate(username: String, password: String, register: Boolean){
        val passwordHash = Sha256.getSha256(password)
        val dialog = LoadingDialogViewController(this).show()
        try{
            if(register){
                processRegister(username, passwordHash)
            }
            val session = processLogin(username, passwordHash)
            CredentialsStorage(this).saveSession(username, session)
            Toast.makeText(this, R.string.loggedIn, Toast.LENGTH_LONG).show()
            dialog.hide()
            finish()
        }catch (e: Exception){
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            dialog.hide()
        }
    }

    @WorkerThread
    private suspend fun processRegister(username: String, passwordHash: String) = withContext(Dispatchers.IO) {
        try {
            DataRepository.getInstance().accessOpenAPI().register(username, passwordHash)
        }catch (_: ForbiddenException){
            throw Exception(getString(R.string.errorUsernameTaken))
        }catch (_: BadRequestException){
            throw Exception(getString(R.string.errorUsernameNotAllowed))
        }
    }

    @WorkerThread
    private suspend fun processLogin(username: String, passwordHash: String) = withContext(Dispatchers.IO) {
        try {
            DataRepository.getInstance().accessOpenAPI().newSession(username, passwordHash)
        }catch (_: ForbiddenException){
            throw Exception(getString(R.string.errorCredentialsNotCorrect))
        }
    }

}