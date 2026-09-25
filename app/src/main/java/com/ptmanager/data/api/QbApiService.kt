package com.ptmanager.data.api

import com.ptmanager.data.model.TorrentInfo
import com.ptmanager.data.model.TorrentProperties
import com.ptmanager.data.model.Tracker
import com.ptmanager.data.model.TransferInfo
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface QbApiService {

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
    ): Response<okhttp3.ResponseBody>

    @GET("app/version")
    suspend fun version(): Response<okhttp3.ResponseBody>

    @GET("transfer/info")
    suspend fun transferInfo(): Response<TransferInfo>

    @GET("torrents/info")
    suspend fun torrentsInfo(
        @Query("filter") filter: String?,
        @Query("sort") sort: String?,
        @Query("reverse") reverse: Boolean?,
    ): Response<List<TorrentInfo>>

    @GET("torrents/properties")
    suspend fun torrentProperties(@Query("hash") hash: String): Response<TorrentProperties>

    @GET("torrents/trackers")
    suspend fun torrentTrackers(@Query("hash") hash: String): Response<List<Tracker>>

    @FormUrlEncoded
    @POST("torrents/resume")
    suspend fun start(@Field("hashes") hashes: String): Response<okhttp3.ResponseBody>

    @FormUrlEncoded
    @POST("torrents/pause")
    suspend fun stop(@Field("hashes") hashes: String): Response<okhttp3.ResponseBody>

    @FormUrlEncoded
    @POST("torrents/recheck")
    suspend fun recheck(@Field("hashes") hashes: String): Response<okhttp3.ResponseBody>

    @FormUrlEncoded
    @POST("torrents/reannounce")
    suspend fun reannounce(@Field("hashes") hashes: String): Response<okhttp3.ResponseBody>

    @FormUrlEncoded
    @POST("torrents/delete")
    suspend fun delete(
        @Field("hashes") hashes: String,
        @Field("deleteFiles") deleteFiles: Boolean,
    ): Response<okhttp3.ResponseBody>

    @Multipart
    @POST("torrents/add")
    suspend fun addByUrls(
        @Part("urls") urls: RequestBody,
        @Part("savepath") savepath: RequestBody?,
        @Part("category") category: RequestBody?,
        @Part("paused") paused: RequestBody?,
    ): Response<okhttp3.ResponseBody>

    @Multipart
    @POST("torrents/add")
    suspend fun addByFile(
        @Part("savepath") savepath: RequestBody?,
        @Part("category") category: RequestBody?,
        @Part("paused") paused: RequestBody?,
        @Part files: List<MultipartBody.Part>,
    ): Response<okhttp3.ResponseBody>
}