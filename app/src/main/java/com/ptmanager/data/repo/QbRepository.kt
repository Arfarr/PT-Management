package com.ptmanager.data.repo

import com.ptmanager.data.api.QbApiService
import com.ptmanager.data.model.ServerConfig
import com.ptmanager.data.model.TorrentInfo
import com.ptmanager.data.model.TorrentProperties
import com.ptmanager.data.model.Tracker
import com.ptmanager.data.model.TransferInfo
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException

class QbApiException(val code: Int, message: String) : IOException(message)

class QbRepository(private val api: QbApiService, private val server: ServerConfig) {

    suspend fun testConnection(): Result<String> {
        return try {
            val r = api.version()
            if (r.isSuccessful) {
                val v = r.body()?.string()?.trim().orEmpty()
                Result.success(if (v.isEmpty()) server.name else "qBittorrent ${v.removePrefix("v")}")
            } else {
                Result.failure(QbApiException(r.code(), errorBody(r)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listTorrents(filter: String, sort: String, reverse: Boolean): List<TorrentInfo> =
        call { api.torrentsInfo(filter, sort, reverse) }

    suspend fun transferInfo(): TransferInfo = call { api.transferInfo() }

    suspend fun properties(hash: String): TorrentProperties = call { api.torrentProperties(hash) }

    suspend fun trackers(hash: String): List<Tracker> = call { api.torrentTrackers(hash) }

    suspend fun start(hashes: List<String>) = callVoid { api.start(hashes.joinToString("|")) }

    suspend fun stop(hashes: List<String>) = callVoid { api.stop(hashes.joinToString("|")) }

    suspend fun recheck(hashes: List<String>) = callVoid { api.recheck(hashes.joinToString("|")) }

    suspend fun reannounce(hashes: List<String>) = callVoid { api.reannounce(hashes.joinToString("|")) }

    suspend fun delete(hashes: List<String>, deleteFiles: Boolean) =
        callVoid { api.delete(hashes.joinToString("|"), deleteFiles) }

    suspend fun addUrls(urls: List<String>, category: String?, paused: Boolean) {
        val urlsBody = urls.joinToString("\n").toRequestBody("text/plain".toMediaType())
        val categoryBody = category?.toRequestBody("text/plain".toMediaType())
        val pausedBody = paused.toString().toRequestBody("text/plain".toMediaType())
        callVoid { api.addByUrls(urlsBody, null, categoryBody, pausedBody) }
    }

    suspend fun addFiles(files: List<ByteArray>, names: List<String>, category: String?, paused: Boolean) {
        val parts = files.zip(names).map { (data, name) ->
            MultipartBody.Part.createFormData(
                "torrents", name,
                data.toRequestBody("application/x-bittorrent".toMediaType())
            )
        }
        if (parts.isEmpty()) throw IOException("未选择 torrent 文件")
        val categoryBody = category?.toRequestBody("text/plain".toMediaType())
        val pausedBody = paused.toString().toRequestBody("text/plain".toMediaType())
        callVoid { api.addByFile(null, categoryBody, pausedBody, parts) }
    }

    private suspend fun <T> call(block: suspend () -> Response<T>): T {
        val r = block()
        if (!r.isSuccessful) throw QbApiException(r.code(), errorBody(r))
        return requireNotNull(r.body()) { "空响应体" }
    }

    private suspend fun callVoid(block: suspend () -> Response<okhttp3.ResponseBody>) {
        val r = block()
        if (!r.isSuccessful) throw QbApiException(r.code(), errorBody(r))
    }

    private fun errorBody(r: Response<*>): String =
        r.errorBody()?.string().orEmpty().ifBlank { "HTTP ${r.code()}" }.take(300)
}