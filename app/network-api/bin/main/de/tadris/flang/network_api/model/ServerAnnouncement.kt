package de.tadris.flang.network_api.model

data class ServerAnnouncement(val title: String, val text: String, val url: String, val priority: Int = 0)
