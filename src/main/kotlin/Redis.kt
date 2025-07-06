package com.apptolast

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.ktor.server.application.*
import io.lettuce.core.ExperimentalLettuceCoroutinesApi

class RedisConfig {
    companion object {
        private lateinit var redisClient: RedisClient
        private lateinit var connection: StatefulRedisConnection<String, String>

        fun init() {
            // Si no requiere auth desde localhost con port-forward
            val uri = RedisURI.Builder
                .redis("138.199.157.58", 32079)
                .withPassword("AppToLast2023%".toCharArray())  // Comentar si no necesita
                .withDatabase(0)
                .build()

            redisClient = RedisClient.create(uri)
            connection = redisClient.connect()
        }

        @OptIn(ExperimentalLettuceCoroutinesApi::class)
        fun getCoroutineCommands() = connection.coroutines()

        fun shutdown() {
            connection.close()
            redisClient.shutdown()
        }
    }
}

fun Application.configureRedis() {
    RedisConfig.init()

    environment.monitor.subscribe(ApplicationStopped) {
        RedisConfig.shutdown()
    }
}