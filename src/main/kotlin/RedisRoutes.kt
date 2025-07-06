package com.apptolast

import com.apptolast.models.Book
import com.apptolast.models.Chapter
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

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

@Serializable
data class PopulateResponse(
    val success: Boolean,
    val message: String,
    val booksCreated: Int,
    val chaptersCreated: Int
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

        // GET - Poblar con datos ficticios
        get("/populate") {
            try {
                val redis = RedisConfig.getCoroutineCommands()
                val json = Json { prettyPrint = true }

                // Crear usuarios ficticios
                val userId1 = UUID.randomUUID().toString()
                val userId2 = UUID.randomUUID().toString()

                // Crear libros ficticios
                val books = listOf(
                    Book(
                        id = UUID.randomUUID().toString(),
                        userId = userId1,
                        title = "El Arte de la Programación en Kotlin",
                        description = "Una guía completa para dominar Kotlin y sus características más avanzadas",
                        coverImage = "https://example.com/kotlin-book-cover.jpg",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        isActive = true
                    ),
                    Book(
                        id = UUID.randomUUID().toString(),
                        userId = userId1,
                        title = "Microservicios con Ktor",
                        description = "Aprende a construir microservicios escalables y eficientes con Ktor",
                        coverImage = null,
                        createdAt = System.currentTimeMillis() - 86400000, // 1 día antes
                        updatedAt = System.currentTimeMillis() - 43200000, // 12 horas antes
                        isActive = true
                    ),
                    Book(
                        id = UUID.randomUUID().toString(),
                        userId = userId2,
                        title = "Redis: La Base de Datos en Memoria",
                        description = "Todo lo que necesitas saber sobre Redis y su implementación en aplicaciones modernas",
                        coverImage = "https://example.com/redis-book-cover.jpg",
                        createdAt = System.currentTimeMillis() - 172800000, // 2 días antes
                        updatedAt = System.currentTimeMillis() - 3600000, // 1 hora antes
                        isActive = false
                    )
                )

                // Guardar libros en Redis
                var booksCreated = 0
                for (book in books) {
                    val key = "book:${book.id}"
                    val value = json.encodeToString(book)
                    redis.set(key, value)

                    // También guardar en índice por usuario
                    redis.sadd("user:${book.userId}:books", book.id)

                    booksCreated++
                }

                // Crear capítulos ficticios
                val chapters = mutableListOf<Chapter>()

                // Capítulos para el primer libro
                chapters.add(Chapter(
                    id = UUID.randomUUID().toString(),
                    bookId = books[0].id,
                    userId = books[0].userId,
                    title = "Introducción a Kotlin",
                    content = "Kotlin es un lenguaje de programación moderno que combina lo mejor de la programación orientada a objetos y funcional...",
                    editedDate = "2024-01-15T10:30:00Z",
                    orderIndex = 1,
                    createdAt = System.currentTimeMillis() - 86400000,
                    updatedAt = System.currentTimeMillis() - 86400000,
                    isDeleted = false
                ))

                chapters.add(Chapter(
                    id = UUID.randomUUID().toString(),
                    bookId = books[0].id,
                    userId = books[0].userId,
                    title = "Variables y Tipos de Datos",
                    content = "En Kotlin, las variables pueden ser mutables (var) o inmutables (val). La inferencia de tipos es una característica poderosa...",
                    editedDate = "2024-01-16T14:20:00Z",
                    orderIndex = 2,
                    createdAt = System.currentTimeMillis() - 72000000,
                    updatedAt = System.currentTimeMillis() - 36000000,
                    isDeleted = false
                ))

                chapters.add(Chapter(
                    id = UUID.randomUUID().toString(),
                    bookId = books[0].id,
                    userId = books[0].userId,
                    title = "Funciones y Lambdas",
                    content = "Las funciones en Kotlin son ciudadanos de primera clase. Pueden ser almacenadas en variables, pasadas como parámetros...",
                    editedDate = "2024-01-17T09:15:00Z",
                    orderIndex = 3,
                    createdAt = System.currentTimeMillis() - 60000000,
                    updatedAt = System.currentTimeMillis() - 18000000,
                    isDeleted = false
                ))

                // Capítulos para el segundo libro
                chapters.add(Chapter(
                    id = UUID.randomUUID().toString(),
                    bookId = books[1].id,
                    userId = books[1].userId,
                    title = "¿Qué es Ktor?",
                    content = "Ktor es un framework asíncrono para crear aplicaciones web y APIs REST en Kotlin. Diseñado desde cero para aprovechar las corrutinas...",
                    editedDate = "2024-01-18T11:00:00Z",
                    orderIndex = 1,
                    createdAt = System.currentTimeMillis() - 48000000,
                    updatedAt = System.currentTimeMillis() - 24000000,
                    isDeleted = false
                ))

                chapters.add(Chapter(
                    id = UUID.randomUUID().toString(),
                    bookId = books[1].id,
                    userId = books[1].userId,
                    title = "Configuración del Proyecto",
                    content = "Para comenzar con Ktor, necesitamos configurar nuestro build.gradle.kts con las dependencias necesarias...",
                    editedDate = "2024-01-18T15:30:00Z",
                    orderIndex = 2,
                    createdAt = System.currentTimeMillis() - 36000000,
                    updatedAt = System.currentTimeMillis() - 12000000,
                    isDeleted = false
                ))

                // Capítulo eliminado (para probar el flag isDeleted)
                chapters.add(
                    Chapter(
                        id = UUID.randomUUID().toString(),
                        bookId = books[1].id,
                        userId = books[1].userId,
                        title = "Capítulo Borrador Eliminado",
                        content = "Este contenido fue eliminado...",
                        editedDate = "2024-01-17T08:00:00Z",
                        orderIndex = 3,
                        createdAt = System.currentTimeMillis() - 96000000,
                        updatedAt = System.currentTimeMillis() - 6000000,
                        isDeleted = true
                    )
                )

                // Capítulos para el tercer libro
                chapters.add(Chapter(
                    id = UUID.randomUUID().toString(),
                    bookId = books[2].id,
                    userId = books[2].userId,
                    title = "Introducción a Redis",
                    content = "Redis es una base de datos en memoria de código abierto que se utiliza como base de datos, caché y broker de mensajes...",
                    editedDate = "2024-01-10T16:45:00Z",
                    orderIndex = 1,
                    createdAt = System.currentTimeMillis() - 144000000,
                    updatedAt = System.currentTimeMillis() - 72000000,
                    isDeleted = false
                ))

                // Guardar capítulos en Redis
                var chaptersCreated = 0
                for (chapter in chapters) {
                    val key = "chapter:${chapter.id}"
                    val value = json.encodeToString(chapter)
                    redis.set(key, value)

                    // También guardar en índice por libro
                    redis.sadd("book:${chapter.bookId}:chapters", chapter.id)

                    // Y en índice por usuario
                    redis.sadd("user:${chapter.userId}:chapters", chapter.id)

                    chaptersCreated++
                }

                // Crear algunos índices adicionales útiles
                // Índice de todos los libros
                for (book in books) {
                    redis.sadd("books:all", book.id)
                    if (book.isActive) {
                        redis.sadd("books:active", book.id)
                    }
                }

                // Índice de todos los capítulos
                for (chapter in chapters) {
                    redis.sadd("chapters:all", chapter.id)
                    if (!chapter.isDeleted) {
                        redis.sadd("chapters:active", chapter.id)
                    }
                }

                call.respond(
                    HttpStatusCode.OK,
                    PopulateResponse(
                        success = true,
                        message = "Base de datos poblada exitosamente con datos ficticios",
                        booksCreated = booksCreated,
                        chaptersCreated = chaptersCreated
                    )
                )

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    PopulateResponse(
                        success = false,
                        message = "Error al poblar la base de datos: ${e.message}",
                        booksCreated = 0,
                        chaptersCreated = 0
                    )
                )
            }
        }
    }
}