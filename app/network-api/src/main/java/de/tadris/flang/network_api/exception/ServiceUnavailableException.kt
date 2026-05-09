package de.tadris.flang.network_api.exception

class ServiceUnavailableException(body: String?) :
    ServerException("Service not available. Server probably in maintenance mode", 503, body)