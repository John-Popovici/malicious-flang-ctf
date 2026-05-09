package de.tadris.flang.network_api.model

data class ServerInfo(val playerCount: Int, val gameCount: Int, val announcements: List<ServerAnnouncement>)
