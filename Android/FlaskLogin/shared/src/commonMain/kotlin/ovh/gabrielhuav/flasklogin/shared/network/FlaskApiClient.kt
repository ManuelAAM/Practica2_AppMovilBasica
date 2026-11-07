package ovh.gabrielhuav.flasklogin.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ovh.gabrielhuav.flasklogin.shared.model.AdminAuditFavoritesResponse
import ovh.gabrielhuav.flasklogin.shared.model.AdminAuditHistoryResponse
import ovh.gabrielhuav.flasklogin.shared.model.AdminUsersResponse
import ovh.gabrielhuav.flasklogin.shared.model.AuthResponse
import ovh.gabrielhuav.flasklogin.shared.model.FavoritesResponse
import ovh.gabrielhuav.flasklogin.shared.model.GoogleAuthRequest
import ovh.gabrielhuav.flasklogin.shared.model.HistoryResponse
import ovh.gabrielhuav.flasklogin.shared.model.OfflineAction
import ovh.gabrielhuav.flasklogin.shared.model.ProfileResponse
import ovh.gabrielhuav.flasklogin.shared.model.SingleTaskResponse
import ovh.gabrielhuav.flasklogin.shared.model.SyncPayload
import ovh.gabrielhuav.flasklogin.shared.model.SyncResponse
import ovh.gabrielhuav.flasklogin.shared.model.SyncStatusResponse
import ovh.gabrielhuav.flasklogin.shared.model.TasksResponse

class FlaskApiClient(
    var baseUrl: String = "http://10.0.2.2:5000",
    private val client: HttpClient = createPlatformHttpClient()
) {

    private fun authHeader(token: String): Pair<String, String> {
        val cleanToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
        return "Authorization" to cleanToken
    }

    fun candidateHosts(): List<String> {
        return listOf(baseUrl, "http://127.0.0.1:5000", "http://10.0.2.2:5000", "http://192.168.1.75:5000").distinct()
    }

    private suspend fun <T> executeWithHostFailover(action: suspend (currentBaseUrl: String) -> Result<T>): Result<T> {
        val candidates = candidateHosts()
        var lastFailure: Result<T>? = null

        for (candidate in candidates) {
            val res = action(candidate)
            if (res.isSuccess) {
                if (baseUrl != candidate) {
                    baseUrl = candidate
                }
                return res
            }
            val ex = res.exceptionOrNull()
            if (ex != null && !isNetworkConnectionError(ex)) {
                return res
            }
            lastFailure = res
        }
        return lastFailure ?: Result.failure(Exception("No se pudo establecer conexión con el servidor Flask"))
    }

    private fun isNetworkConnectionError(e: Throwable): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("timeout") ||
               msg.contains("connect") ||
               msg.contains("failed to connect") ||
               msg.contains("unreachable") ||
               msg.contains("refused")
    }

    // ----------------------------------------------------------------------
    // Autenticación y Perfil
    // ----------------------------------------------------------------------
    suspend fun login(username: String, password: String): Result<AuthResponse> {
        return executeWithHostFailover { host ->
            try {
                val response = client.post("$host/login") {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("username", username)
                        put("password", password)
                    })
                }
                if (response.status == HttpStatusCode.OK) {
                    Result.success(response.body())
                } else {
                    val errorBody: AuthResponse = response.body()
                    Result.failure(Exception(errorBody.message ?: "Credenciales inválidas"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun register(username: String, password: String): Result<AuthResponse> {
        return executeWithHostFailover { host ->
            try {
                val response = client.post("$host/register") {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("username", username)
                        put("password", password)
                    })
                }
                if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                    Result.success(response.body())
                } else {
                    val errorBody: AuthResponse = response.body()
                    Result.failure(Exception(errorBody.message ?: "Error al registrar usuario"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun googleAuth(request: GoogleAuthRequest): Result<AuthResponse> {
        return executeWithHostFailover { host ->
            try {
                val response = client.post("$host/auth/google") {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("email", request.email)
                        put("name", request.name)
                        put("google_id", request.googleId)
                        request.idToken?.let { put("id_token", it) }
                    })
                }
                if (response.status == HttpStatusCode.OK) {
                    Result.success(response.body())
                } else {
                    val errorBody: AuthResponse = response.body()
                    Result.failure(Exception(errorBody.message ?: "Error al autenticar con Google"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getProfile(token: String): Result<ProfileResponse> {
        return try {
            val h = authHeader(token)
            val response = client.get("$baseUrl/profile") {
                header(h.first, h.second)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Error al consultar perfil: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------------------------------------------------------------
    // CRUD Tareas (Práctica 2 Conservado)
    // ----------------------------------------------------------------------
    suspend fun getTasks(token: String): Result<TasksResponse> {
        return try {
            val h = authHeader(token)
            val response = client.get("$baseUrl/tasks") {
                header(h.first, h.second)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Error HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTask(token: String, title: String, description: String): Result<SingleTaskResponse> {
        return try {
            val h = authHeader(token)
            val response = client.post("$baseUrl/tasks") {
                header(h.first, h.second)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("title", title)
                    put("description", description)
                    put("status", "pending")
                })
            }
            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Error al crear tarea: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTask(token: String, taskId: Int, status: String, title: String? = null, description: String? = null): Result<SingleTaskResponse> {
        return try {
            val h = authHeader(token)
            val response = client.put("$baseUrl/tasks/$taskId") {
                header(h.first, h.second)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("status", status)
                    title?.let { put("title", it) }
                    description?.let { put("description", it) }
                })
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Error al actualizar tarea"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(token: String, taskId: Int): Result<Boolean> {
        return try {
            val h = authHeader(token)
            val response = client.delete("$baseUrl/tasks/$taskId") {
                header(h.first, h.second)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(true)
            } else {
                Result.failure(Exception("Error al eliminar tarea"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------------------------------------------------------------
    // Historial de Búsquedas (Práctica 5 - Ejercicio 3.1)
    // ----------------------------------------------------------------------
    suspend fun getSearchHistory(token: String): Result<HistoryResponse> {
        return try {
            val h = authHeader(token)
            val response = client.get("$baseUrl/history") {
                header(h.first, h.second)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Error al consultar historial: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addSearchHistory(token: String, query: String, category: String = "tv_shows"): Result<Boolean> {
        return try {
            val h = authHeader(token)
            val response = client.post("$baseUrl/history") {
                header(h.first, h.second)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("query", query)
                    put("category", category)
                })
            }
            Result.success(response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearSearchHistory(token: String): Result<Boolean> {
        return try {
            val h = authHeader(token)
            val response = client.delete("$baseUrl/history") {
                header(h.first, h.second)
            }
            Result.success(response.status == HttpStatusCode.OK)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------------------------------------------------------------
    // Favoritos (Práctica 5 - Ejercicio 2 y 3)
    // ----------------------------------------------------------------------
    suspend fun getFavorites(token: String): Result<FavoritesResponse> {
        return try {
            val h = authHeader(token)
            val response = client.get("$baseUrl/favorites") {
                header(h.first, h.second)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Error al consultar favoritos: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addFavorite(
        token: String,
        itemId: String,
        title: String,
        genres: String,
        rating: String,
        imageUrl: String,
        summary: String
    ): Result<Boolean> {
        return try {
            val h = authHeader(token)
            val response = client.post("$baseUrl/favorites") {
                header(h.first, h.second)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("item_id", itemId)
                    put("title", title)
                    put("genres", genres)
                    put("rating", rating)
                    put("image_url", imageUrl)
                    put("summary", summary)
                })
            }
            Result.success(response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFavorite(token: String, itemId: String): Result<Boolean> {
        return try {
            val h = authHeader(token)
            val response = client.delete("$baseUrl/favorites/$itemId") {
                header(h.first, h.second)
            }
            Result.success(response.status == HttpStatusCode.OK)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------------------------------------------------------------
    // Sincronización en Lote para Cola Offline (Ejercicio 1.2)
    // ----------------------------------------------------------------------
    suspend fun syncOfflineFavorites(token: String, actions: List<OfflineAction>): Result<SyncResponse> {
        return try {
            val h = authHeader(token)
            val response = client.post("$baseUrl/sync/favorites") {
                header(h.first, h.second)
                contentType(ContentType.Application.Json)
                setBody(SyncPayload(actions))
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Error de sincronización: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSyncStatus(token: String): Result<SyncStatusResponse> {
        return try {
            val h = authHeader(token)
            val response = client.get("$baseUrl/sync/status") {
                header(h.first, h.second)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Error al consultar estado de sinc: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------------------------------------------------------------
    // Auditoría Administrativa (Solo Administrador)
    // ----------------------------------------------------------------------
    suspend fun adminGetUsers(token: String): Result<AdminUsersResponse> {
        return try {
            val h = authHeader(token)
            val response = client.get("$baseUrl/users") {
                header(h.first, h.second)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Error al consultar usuarios (Código: ${response.status.value})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminGetUserHistory(token: String, userId: Int): Result<AdminAuditHistoryResponse> {
        return try {
            val h = authHeader(token)
            val response = client.get("$baseUrl/admin/users/$userId/history") {
                header(h.first, h.second)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else if (response.status == HttpStatusCode.Forbidden) {
                Result.failure(Exception("403 Forbidden: Se requieren permisos de Administrador"))
            } else {
                Result.failure(Exception("Error al consultar historial del usuario: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminGetUserFavorites(token: String, userId: Int): Result<AdminAuditFavoritesResponse> {
        return try {
            val h = authHeader(token)
            val response = client.get("$baseUrl/admin/users/$userId/favorites") {
                header(h.first, h.second)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else if (response.status == HttpStatusCode.Forbidden) {
                Result.failure(Exception("403 Forbidden: Se requieren permisos de Administrador"))
            } else {
                Result.failure(Exception("Error al consultar favoritos del usuario: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
