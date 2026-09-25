package com.ptmanager.ui.add

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptmanager.AppContainer
import com.ptmanager.data.repo.QbRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

data class SelectedFile(
    val name: String,
    val size: Long,
)

data class AddTorrentUiState(
    val urlsText: String = "",
    val category: String = "",
    val paused: Boolean = false,
    val files: List<SelectedFile> = emptyList(),
    val adding: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

class AddTorrentViewModel(
    private val container: AppContainer,
    private val appContext: Context,
    val serverId: String,
) : ViewModel() {

    private val _ui = MutableStateFlow(AddTorrentUiState())
    val ui = _ui.asStateFlow()

    private val pendingUris = mutableListOf<Uri>()

    init {
        readClipboard()
    }

    fun readClipboard() {
        val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(appContext)?.toString()
        if (text.isNullOrBlank()) return
        val links = text.lines()
            .map { it.trim() }
            .filter { it.startsWith("magnet:?") || it.contains("btih:") }
            .toList()
        if (links.isNotEmpty()) {
            _ui.value = _ui.value.copy(urlsText = links.joinToString("\n"))
        }
    }

    fun setUrls(text: String) {
        _ui.value = _ui.value.copy(urlsText = text)
    }

    fun setCategory(category: String) {
        _ui.value = _ui.value.copy(category = category)
    }

    fun setPaused(paused: Boolean) {
        _ui.value = _ui.value.copy(paused = paused)
    }

    fun addFiles(uris: List<Uri>) {
        val added = uris.map { uri ->
            val name = container.app.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: "file_${System.currentTimeMillis()}.torrent"
            val size = container.app.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.SIZE),
                null, null, null,
            )?.use { c -> if (c.moveToFirst()) c.getLong(0) else -1L } ?: -1L
            SelectedFile(name, size)
        }
        pendingUris.addAll(uris)
        _ui.value = _ui.value.copy(files = _ui.value.files + added, error = null)
    }

    fun removeFile(index: Int) {
        if (index in pendingUris.indices) pendingUris.removeAt(index)
        _ui.value = _ui.value.copy(files = _ui.value.files.toMutableList().also { it.removeAt(index) })
    }

    fun add(onDone: () -> Unit, onError: (String) -> Unit) {
        if (_ui.value.adding) return
        val urls = _ui.value.urlsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (urls.isEmpty() && pendingUris.isEmpty()) {
            onError("请输入 URL/磁力链接，或选择 torrent 文件")
            return
        }
        _ui.value = _ui.value.copy(adding = true, error = null)
        viewModelScope.launch {
            val repo = getRepo()
            try {
                val category = _ui.value.category.trim().ifBlank { null }
                val paused = _ui.value.paused
                if (urls.isNotEmpty()) repo.addUrls(urls, category, paused)
                if (pendingUris.isNotEmpty()) {
                    val data = withContext(Dispatchers.IO) {
                        pendingUris.map { uri ->
                            val name = container.app.contentResolver.query(
                                uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                                null, null, null,
                            )?.use { c -> if (c.moveToFirst()) c.getString(0) else "torrent.torrent" }
                                ?: "torrent.torrent"
                            val bytes = container.app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                ?: throw java.io.IOException("无法读取文件: $name")
                            bytes to name
                        }
                    }
                    repo.addFiles(data.map { it.first }, data.map { it.second }, category, paused)
                }
                _ui.value = _ui.value.copy(adding = false, done = true)
                onDone()
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(adding = false, error = e.message ?: "添加失败")
                onError(e.message ?: "添加失败")
            }
        }
    }

    private fun getRepo(): QbRepository {
        val cfg = container.serverRepo.getServer(serverId) ?: throw IllegalStateException("服务器不存在")
        return container.qbRepository(cfg)
    }
}