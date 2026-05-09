package de.tadris.flang.network_api.exception

class BadRequestException(message: String, body: String?) : ServerException(
    "Bad Request - $message", 400, body
)