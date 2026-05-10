package de.tadris.flang.ui.activity

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import android.util.Log

data class User(
    val username: String,
    val salt: String,
    val passwordHash: String
)


fun readUsers(file: File): MutableList<User> {
    if (!file.exists()) {
        Log.d("Flang", "User file not found!")
        return mutableListOf()
    }

    val jsonArray = JSONArray(file.readText())

    val userList = mutableListOf<User>()

    for (i in 0 until jsonArray.length()) {
        val user = jsonArray.getJSONObject(i)
        userList.add(
            User(
                user.getString("username"),
                user.getString("salt"),
                user.getString("passwordHash")
            )
        )
    }

    return userList
}

fun writeUsers(file: File, userList: MutableList<User>) {
    val jsonArray = JSONArray()
    for (user in userList) {
        val obj = JSONObject()
        obj.put("username", user.username)
        obj.put("salt", user.salt)
        obj.put("passwordHash", user.passwordHash)
        jsonArray.put(obj)
    }
    if (!file.exists()) {
        file.createNewFile()
    }
    file.writeText(jsonArray.toString())
}
