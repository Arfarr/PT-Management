package com.ptmanager.ui.torrents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptmanager.AppContainer
import com.ptmanager.data.model.TorrentInfo
import com.ptmanager.data.model.TransferInfo
import com.ptmanager.data.repo.QbRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TorrentFilter(val apiValue: String, val label: String) {
    ALL("all", "全部"),
    DOWNLOADING("downloading", "下载中"),
    SEEDING("seeding", "做种中"),
    COMPLETED("completed", "已完成"),
    PAUSED("paused", "已暂停"),
    ERRORED("errored", "错误"),
}

enum class TorrentSort(val apiValue: String, val label: String, val defaultDesc: Boolean) {
    ADDED_ON("added_on", "添加时间", true),
    NAME("name", "名称", false),
    SIZE("size", "大小", true),
    PROGRESS("progress", "进度", true),
    RATIO("ratio", "分享率", true),
    DL_SPEED("dlspeed", "下载速度", true),
    UP_SPEED("upspeed", "上传速度", true),
    ETA("eta", "剩余时间", true),
    NUM_SEEDS("num_seeds", "做种数", true),
    NUM_LEECHS("num_leechs", "下载数", true),
}

data class TorrentListUiState(
    val serverName: String = "",
    val torrents: List<TorrentInfo> = emptyList(),
    val transfer: TransferInfo? = null,
    val filter: TorrentFilter = TorrentFilter.ALL,
    val sort: TorrentSort = TorrentSort.ADDED_ON,
    val sortDesc: Boolean = true,
    val query: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val connected: Boolean = true,
)

class TorrentListViewModel(
    private val container: AppContainer,
    val serverId: String,
) : ViewModel() {

    private val _ui = MutableStateFlow(TorrentListUiState())
    val ui = _ui.asStateFlow()

    private var cachedRepo: QbRepository? = null
    private var pollJob: Job? = null

    init {
        _ui.value = _ui.value.copy(
            serverName = container.serverRepo.getServer(serverId)?.name ?: "未知服务器",
        )
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
                val torrents = r.listTorrents(
                    _ui.value.filter.apiValue,
                    _ui.value.sort.apiValue,
                    _ui.value.sortDesc,
                )
                val transfer = r.transferInfo()
                _ui.value = _ui.value.copy(
                    torrents = torrents,
                    transfer = transfer,
                    loading = false,
                    error = null,
                    connected = true,
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    error = e.message ?: "加载失败",
                    connected = false,
                )
            }
        }
    }

    fun setFilter(filter: TorrentFilter) {
        _ui.value = _ui.value.copy(filter = filter)
        refresh()
    }

    fun setSort(sort: TorrentSort) {
        val nextDesc = if (sort == _ui.value.sort) !_ui.value.sortDesc else sort.defaultDesc
        _ui.value = _ui.value.copy(sort = sort, sortDesc = nextDesc)
        refresh()
    }

    fun setQuery(query: String) {
        _ui.value = _ui.value.copy(query = query)
    }

    fun filteredTorrents(state: TorrentListUiState): List<TorrentInfo> {
        val q = state.query.trim()
        return if (q.isEmpty()) state.torrents
        else state.torrents.filter { it.name.contains(q, ignoreCase = true) }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}