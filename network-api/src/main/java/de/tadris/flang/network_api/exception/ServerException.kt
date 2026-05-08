package de.tadris.flang.network_api.exception

open class ServerException(message: String, code: Int, body: String?) : FlangAPIException(
    "$message - $body"
)