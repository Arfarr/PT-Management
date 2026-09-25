package com.ptmanager.data.repo

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ptmanager.data.model.ServerConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

class ServerRepository(context: Context) {

    private val prefs: SharedPreferences = createEncryptedPrefs(context.applicationContext)
    private val json = Json { ignoreUnknownKeys = true }

    private val serializer = ListSerializer(ServerConfig.serializer())

    fun servers(): List<ServerConfig> {
        val raw = prefs.getString(KEY_SERVERS, null) ?: return emptyList()
        return try {
            json.decodeFromString(serializer, raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveServers(list: List<ServerConfig>) {
        prefs.edit().putString(KEY_SERVERS, json.encodeToString(serializer, list)).apply()
    }

    fun addServer(config: ServerConfig) {
        val list = servers().toMutableList().apply { add(config) }
        saveServers(list)
    }

    fun updateServer(config: ServerConfig) {
        val list = servers().map { if (it.id == config.id) config else it }
        saveServers(list)
    }

    fun removeServer(id: String) {
        saveServers(servers().filter { it.id != id })
    }

    fun getServer(id: String): ServerConfig? = servers().firstOrNull { it.id == id }

    fun activeServerId(): String = prefs.getString(KEY_ACTIVE, "").orEmpty()

    fun setActiveServerId(id: String) {
        prefs.edit().putString(KEY_ACTIVE, id).apply()
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "ptmanager_secure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.getSharedPreferences("ptmanager_secure", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val KEY_SERVERS = "servers"
        private const val KEY_ACTIVE = "active_server_id"
    }
}