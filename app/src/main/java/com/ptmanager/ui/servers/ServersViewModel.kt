package com.ptmanager.ui.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptmanager.AppContainer
import com.ptmanager.data.model.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ServersUiState(
    val servers: List<ServerConfig> = emptyList(),
    val activeId: String = "",
)

class ServersViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(ServersUiState())
    val ui = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _ui.value = ServersUiState(
            servers = container.serverRepo.servers(),
            activeId = container.serverRepo.activeServerId(),
        )
    }

    fun save(config: ServerConfig, isNew: Boolean) {
        if (config.name.isBlank()) {
            container.serverRepo.addServer(config.copy(name = "未命名"))
        } else {
            if (isNew) container.serverRepo.addServer(config) else container.serverRepo.updateServer(config)
        }
        refresh()
    }

    fun remove(id: String) {
        container.serverRepo.removeServer(id)
        if (container.serverRepo.activeServerId() == id) container.serverRepo.setActiveServerId("")
        refresh()
    }

    fun open(id: String) {
        container.serverRepo.setActiveServerId(id)
        refresh()
    }

    suspend fun testConnection(config: ServerConfig): Result<String> =
        container.qbRepository(config).testConnection()

    companion object {
        fun newId() = UUID.randomUUID().toString()
    }
}