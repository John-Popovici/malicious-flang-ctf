package de.tadris.flang.network_api.exception

class TooManyRequestsException(body: String?) : ServerException(
    "Too Many Requests - Daily analysis limit exceeded", 429, body
)