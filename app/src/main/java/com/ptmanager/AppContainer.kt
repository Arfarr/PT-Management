package com.ptmanager

import android.app.Application
import com.ptmanager.data.api.QbClientFactory
import com.ptmanager.data.model.ServerConfig
import com.ptmanager.data.repo.QbRepository
import com.ptmanager.data.repo.ServerRepository

class AppContainer(val app: Application) {
    val serverRepo: ServerRepository = ServerRepository(app)

    fun qbRepository(server: ServerConfig): QbRepository {
        val normalized = server.copy(baseUrl = server.normalizedUrl)
        val api = QbClientFactory.create(normalized.normalizedUrl, normalized.username, normalized.password)
        return QbRepository(api, normalized)
    }
}

class PTManagerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val android.app.Application.container: AppContainer
    get() = (this as PTManagerApp).container