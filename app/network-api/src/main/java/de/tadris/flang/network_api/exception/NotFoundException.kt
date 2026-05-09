package de.tadris.flang.network_api.exception

class NotFoundException(message: String, body: String?) : ServerException(
    "Not found - $message", 404, body
)