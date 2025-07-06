package com.apptolast

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import kotlinx.serialization.Serializable

@Serializable
data class RedisEntry(
    val key: String,
    val value: String
)

@Serializable
data class RedisResponse(
    val success: Boolean,
    val message: String,
    val data: String? = null
)

@OptIn(ExperimentalLettuceCoroutinesApi::class)
fun Route.redisRoutes() {
    route("/redis") {

        // GET - Health check
        get("/health") {
            try {
                val pong = RedisConfig.getCoroutineCommands().ping()
                if (pong == "PONG") {
                    call.respond(HttpStatusCode.OK, RedisResponse(true, "Redis is healthy", pong))
                } else {
                    call.respond(HttpStatusCode.ServiceUnavailable, RedisResponse(false, "Redis health check failed"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.ServiceUnavailable, RedisResponse(false, "Redis is not available: ${e.message}"))
            }
        }

        // POST - Guardar clave-valor
        post("/set") {
            val entry = call.receive<RedisEntry>()
            try {
                val result = RedisConfig.getCoroutineCommands().set(entry.key, entry.value)
                call.respond(HttpStatusCode.Created, RedisResponse(true, "Value saved successfully", result))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, RedisResponse(false, "Error: ${e.message}"))
            }
        }

        // GET - Obtener valor por clave
        get("/{key}") {
            val key = call.parameters["key"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                RedisResponse(false, "Key parameter is required")
            )

            try {
                val value = RedisConfig.getCoroutineCommands().get(key)
                if (value != null) {
                    call.respond(HttpStatusCode.OK, RedisResponse(true, "Value retrieved successfully", value))
                } else {
                    call.respond(HttpStatusCode.NotFound, RedisResponse(false, "Key not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, RedisResponse(false, "Error: ${e.message}"))
            }
        }
    }
}