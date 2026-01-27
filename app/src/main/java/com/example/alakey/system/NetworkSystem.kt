package com.example.alakey.system

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkSystem @Inject constructor() : Component {

    private var _client: OkHttpClient? = null
    val client: OkHttpClient get() = _client ?: throw IllegalStateException("NetworkSystem not started")

    override fun start() {
        if (_client != null) return
        _client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override fun stop() {
        // OkHttp client doesn't have a strict 'stop' but we can null it out.
        _client = null
    }
}
