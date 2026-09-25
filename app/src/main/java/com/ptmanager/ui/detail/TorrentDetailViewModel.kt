package com.ptmanager.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptmanager.AppContainer
import com.ptmanager.data.model.TorrentInfo
import com.ptmanager.data.model.TorrentProperties
import com.ptmanager.data.model.Tracker
import com.ptmanager.data.repo.QbRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TorrentDetailUiState(
    val info: TorrentInfo? = null,
    val properties: TorrentProperties? = null,
    val trackers: List<Tracker> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val busy: Boolean = false,
)

class TorrentDetailViewModel(
    private val container: AppContainer,
    val serverId: String,
    val torrentHash: String,
) : ViewModel() {

    private val _ui = MutableStateFlow(TorrentDetailUiState())
    val ui = _ui.asStateFlow()

    private var cachedRepo: QbRepository? = null
    private var pollJob: Job? = null

    init {
        startPolling()
    }

    private fun repo(): QbRepository {
        cachedRepo?.let { return it }
        val server = container.serverRepo.getServer(serverId)
            ?: throw IllegalStateException("服务器不存在或已删除")
        return container.qbRepository(server).also { cachedRepo = it }
    }

    fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                refresh(silent = true)
                delay(3000)
            }
        }
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _ui.value = _ui.value.copy(loading = true, error = null)
            try {
                val r = repo()
                val info = r.listTorrents("all", "name", false).find { it.hash == torrentHash }
                val props = r.properties(torrentHash)
                val trackers = r.trackers(torrentHash)
                _ui.value = _ui.value.copy(
                    info = info,
                    properties = props,
                    trackers = trackers,
                    loading = false,
                    error = null,
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "加载失败")
            }
        }
    }

    fun runAction(
        action: suspend (QbRepository) -> Unit,
        onDone: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
    ) {
        if (_ui.value.busy) return
        _ui.value = _ui.value.copy(busy = true)
        viewModelScope.launch {
            try {
                action(repo())
                _ui.value = _ui.value.copy(busy = false)
                onDone?.invoke()
                refresh(silent = true)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(busy = false)
                onError?.invoke(e.message ?: "操作失败")
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}