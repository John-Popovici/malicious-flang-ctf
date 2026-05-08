package de.tadris.flang.network_api.exception

class ServerUnreachableException(message: String?) :
    FlangAPIException("Can not connect to server - $message")