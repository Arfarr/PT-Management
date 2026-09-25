@file:OptIn(ExperimentalSerializationApi::class)

package com.ptmanager.data.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TorrentInfo(
    @SerialName("hash") val hash: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("size") val size: Long = 0,
    @SerialName("progress") val progress: Double = 0.0,
    @SerialName("dlspeed") val dlspeed: Long = 0,
    @SerialName("upspeed") val upspeed: Long = 0,
    @SerialName("eta") val eta: Long = 8640000,
    @SerialName("ratio") val ratio: Double = 0.0,
    @SerialName("category") val category: String = "",
    @SerialName("tags") val tags: String = "",
    @SerialName("save_path") val savePath: String = "",
    @SerialName("num_leechs") val numLeechs: Int = 0,
    @SerialName("num_seeds") val numSeeds: Int = 0,
    @SerialName("state") val state: String = "",
    @SerialName("added_on") val addedOn: Long = 0,
    @SerialName("completion_on") val completionOn: Long = 0,
    @SerialName("amount_left") val amountLeft: Long = 0,
    @SerialName("uploaded") val uploaded: Long = 0,
    @SerialName("downloaded") val downloaded: Long = 0,
    @SerialName("availability") val availability: Double = -1.0,
    @SerialName("tracker") val tracker: String = "",
    @SerialName("trackers_count") val trackersCount: Int = 0,
    @SerialName("content_path") val contentPath: String = "",
) {
    val isCompleted: Boolean get() = progress >= 1.0

    fun displayState(): String = when (state) {
        "downloading" -> "下载中"
        "stalledDL", "stalled" -> "下载停滞"
        "uploading", "forcedUP" -> "做种中"
        "stalledUP" -> "做种停滞"
        "pausedDL" -> "已暂停(下载)"
        "pausedUP" -> "已暂停(做种)"
        "queuedDL" -> "排队(下载)"
        "queuedUP" -> "排队(做种)"
        "checkingDL", "checkingUP", "checkingResumeData", "moving" -> "校验中"
        "allocating" -> "分配空间"
        "error", "missingFiles" -> "错误"
        "metaDL" -> "获取元数据"
        else -> state
    }
}

@Serializable
data class TorrentProperties(
    @SerialName("save_path") val savePath: String = "",
    @SerialName("creation_date") val creationDate: Long = 0,
    @SerialName("piece_size") val pieceSize: Long = 0,
    @SerialName("comment") val comment: String = "",
    @SerialName("total_wasted") val totalWasted: Long = 0,
    @SerialName("total_uploaded") val totalUploaded: Long = 0,
    @SerialName("total_downloaded") val totalDownloaded: Long = 0,
    @SerialName("up_limit") val upLimit: Long = -1,
    @SerialName("dl_limit") val dlLimit: Long = -1,
    @SerialName("time_elapsed") val timeElapsed: Long = 0,
    @SerialName("seeding_time") val seedingTime: Long = 0,
    @SerialName("nb_connections") val nbConnections: Int = 0,
    @SerialName("nb_connections_limit") val nbConnectionsLimit: Int = -1,
    @SerialName("share_ratio") val shareRatio: Double = 0.0,
    @SerialName("addition_date") val additionDate: Long = 0,
    @SerialName("completion_date") val completionDate: Long = 0,
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("dl_speed") val dlSpeed: Long = 0,
    @SerialName("dl_speed_avg") val dlSpeedAvg: Long = 0,
    @SerialName("up_speed") val upSpeed: Long = 0,
    @SerialName("up_speed_avg") val upSpeedAvg: Long = 0,
    @SerialName("eta") val eta: Long = 8640000,
    @SerialName("last_seen") val lastSeen: Long = 0,
    @SerialName("peers") val peers: Int = 0,
    @SerialName("peers_total") val peersTotal: Int = 0,
    @SerialName("pieces_have") val piecesHave: Long = 0,
    @SerialName("pieces_num") val piecesNum: Long = 0,
    @SerialName("reannounce") val reannounce: Long = 0,
    @SerialName("seeding_time_limit") val seedingTimeLimit: Long = 0,
    @SerialName("seen_complete") val seenComplete: Long = 0,
    @SerialName("seq_dl") val seqDl: Boolean = false,
    @SerialName("total_size") val totalSize: Long = 0,
    @SerialName("is_private") val isPrivate: Boolean = false,
    @SerialName("total_safe_size") val totalSafeSize: Long = 0,
    @SerialName("content_path") val contentPath: String = "",
)

@Serializable
data class Tracker(
    @SerialName("url") val url: String = "",
    @SerialName("status") val status: Int = 0,
    @SerialName("num_peers") val numPeers: Int = 0,
    @SerialName("num_seeds") val numSeeds: Int = 0,
    @SerialName("num_leeches") val numLeeches: Int = 0,
    @SerialName("num_downloaded") val numDownloaded: Int = 0,
    @SerialName("msg") val msg: String = "",
    @SerialName("tier") val tier: Int = 0,
) {
    val statusText: String
        get() = when (status) {
            0 -> "已禁用"
            1 -> "未联系"
            2 -> "可连接"
            3 -> "已更新"
            4 -> "未认证"
            5 -> "不可用"
            else -> "未知($status)"
        }
}

@Serializable
data class TransferInfo(
    @SerialName("dl_info_speed") val dlInfoSpeed: Long = 0,
    @SerialName("up_info_speed") val upInfoSpeed: Long = 0,
    @SerialName("dl_rate_limit") val dlRateLimit: Long = 0,
    @SerialName("up_rate_limit") val upRateLimit: Long = 0,
    @SerialName("dl_info_data") val dlInfoData: Long = 0,
    @SerialName("up_info_data") val upInfoData: Long = 0,
    @SerialName("connection_status") val connectionStatus: String = "connected",
    @SerialName("dht_nodes") val dhtNodes: Long = 0,
)