package com.ptmanager.data.api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json

class InMemoryCookieJar : CookieJar {
    private val cookies = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookieList: List<Cookie>) {
        cookies.removeAll { it.matches(url) }
        cookies.addAll(cookieList)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        cookies.filter { it.expiresAt > System.currentTimeMillis() && it.matches(url) }
}

class AuthInterceptor(
    private val loginUrl: String,
    private val username: String,
    private val password: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header(HEADER_SKIP_AUTH) == "1") return chain.proceed(request)
        var response = chain.proceed(request)
        if (response.code == 403) {
            response.close()
            val loginRequest = Request.Builder()
                .url(loginUrl)
                .header(HEADER_SKIP_AUTH, "1")
                .post(
                    FormBody.Builder()
                        .add("username", username)
                        .add("password", password)
                        .build()
                )
                .build()
            chain.proceed(loginRequest).close()
            response = chain.proceed(request)
        }
        return response
    }

    companion object {
        const val HEADER_SKIP_AUTH = "X-Skip-Auth"
    }
}

object QbClientFactory {
    private val cache = ConcurrentHashMap<String, QbApiService>()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun create(baseUrl: String, username: String, password: String): QbApiService {
        val key = "$baseUrl|$username|$password"
        cache[key]?.let { return it }
        val client = OkHttpClient.Builder()
            .cookieJar(InMemoryCookieJar())
            .addInterceptor(logging)
            .addInterceptor(AuthInterceptor("$baseUrl/api/v2/auth/login", username, password))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("$baseUrl/api/v2/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json; charset=utf-8".toMediaType()))
            .build()
        val service = retrofit.create(QbApiService::class.java)
        cache[key] = service
        return service
    }

    fun evict(baseUrl: String) {
        cache.keys.removeAll { it.startsWith(baseUrl) }
    }

    private val logging = HttpLoggingInterceptor { msg ->
        android.util.Log.i("PTAUTH", msg)
    }.apply { level = HttpLoggingInterceptor.Level.BODY }
}
