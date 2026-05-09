package de.tadris.flang.network_api.exception

class ForbiddenException(message: String, body: String?) : ServerException(
    "Forbidden - $message", 403, body
)