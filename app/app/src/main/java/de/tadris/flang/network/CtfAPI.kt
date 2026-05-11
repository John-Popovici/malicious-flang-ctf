package de.tadris.flang.network

import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// localhost on PC -> this IP on emulator
const val url = "10.0.2.2"
const val port = 3000
const val user_route = "user"
const val update_route = "updates"

fun apiRegister(username: String, passwordHash: String, salt: String): String {
    val endpoint = "http://$url:$port/$user_route/register"
    val json = """
        {
            "username": "$username",
            "password": "$passwordHash",
            "salt": "$salt"
        }
    """.trimIndent()

    return request(endpoint, json, "POST")
}

fun apiLogin(username: String, passwordHash: String): String {
    val endpoint = "http://$url:$port/$user_route/login"
    val json = """
        {
            "username": "$username",
            "password": "$passwordHash"
        }
    """.trimIndent()

    return request(endpoint, json, "POST")
}

fun apiGetSalt(username: String): String {
        val endpoint = "http://$url:$port/$user_route/get_salt?username=$username"
        return request(endpoint, "", "GET")
}


fun apiGetUpdate(): String {
    val endpoint =  "http://$url:$port/$update_route/get_update"
    return request(endpoint, "", "GET")
}

fun apiConfirmUpdate(confirmMessage: String): String {
    val endpoint =  "http://$url:$port/$update_route/confirm_update"
    val json = """
        {
            "confirmed": "$confirmMessage"
        }
    """.trimIndent()
    return request(endpoint, json, "POST")
}

fun apiShareC(url_string: String, json: String): String {
    val endpoint =  "http://$url:$port/shareC"
    return request(endpoint, json, "POST")
}


// TODO: throw exception if error happened
private fun request(endpoint: String, json: String, method: String): String {
    val url = URL(endpoint)
    val conn = url.openConnection() as HttpURLConnection

    conn.requestMethod = method
    conn.setRequestProperty("Content-Type", "application/json")

    if (method == "POST") {
        conn.doOutput = true
        OutputStreamWriter(conn.outputStream).use {
            it.write(json)
            it.flush()
        }
    }

    val response = conn.inputStream.bufferedReader().use {
        it.readText()
    }

    conn.disconnect()

    // Check for failure
    val responseObject = JSONObject(response)
    val success = responseObject.optBoolean("success", false)

    if (!success){
        throw Exception(responseObject.optString("error", "Unknown error"))
    }
    return responseObject.optString("message", "Success")
}