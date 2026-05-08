package de.tadris.flang.network_api.exception

class UnknownServerExpeption(code: Int, body: String?) :
    ServerException("Unknown Exception", code, body)