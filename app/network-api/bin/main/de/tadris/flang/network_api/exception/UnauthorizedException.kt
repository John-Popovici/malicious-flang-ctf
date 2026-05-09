package de.tadris.flang.network_api.exception

class UnauthorizedException(body: String?) : ServerException("Unauthorized", 401, body)