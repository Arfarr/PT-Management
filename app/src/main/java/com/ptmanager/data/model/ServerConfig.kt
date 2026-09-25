package com.ptmanager.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    val id: String = "",
    val name: String = "",
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
) {
    val normalizedUrl: String
        get() {
            var u = baseUrl.trim()
            if (u.isEmpty()) return u
            if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
            while (u.endsWith("/")) u = u.dropLast(1)
            return u
        }
}