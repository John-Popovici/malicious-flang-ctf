package de.tadris.flang.network_api.util

import java.security.MessageDigest

object Sha256 {
    fun getSha256(string: String): String {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
        digest.update(string.toByteArray())
        return bytesToHexString(digest.digest())
    }

    private fun bytesToHexString(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (aByte in bytes) {
            val hex = Integer.toHexString(0xFF and aByte.toInt())
            if (hex.length == 1) {
                sb.append('0')
            }
            sb.append(hex)
        }
        return sb.toString()
    }
}