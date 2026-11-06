package ovh.gabrielhuav.flasklogin.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ----------------------------------------------------------------------
// Modelos de Autenticación y Usuario
// ----------------------------------------------------------------------
@Serializable
data class UserData(
    val id: Int,
    val username: String,
    val role: String,
    @SerialName("auth_provider") val authProvider: String? = "native",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AuthResponse(
    val status: String,
    val message: String? = null,
    val token: String? = null,
    @SerialName("user_id") val userId: Int? = null,
    val username: String? = null,
    val role: String? = null,
    @SerialName("auth_provider") val authProvider: String? = null
)

@Serializable
data class ProfileResponse(
    val status: String,
    val message: String? = null,
    val token: String? = null,
    val user: UserData? = null
)

@Serializable
data class AdminUsersResponse(
    val status: String,
    val users: List<UserData> = emptyList()
)

@Serializable
data class GoogleAuthRequest(
    val email: String,
    val name: String,
    @SerialName("google_id") val googleId: String,
    @SerialName("id_token") val idToken: String? = null
)

// ----------------------------------------------------------------------
// Modelos de Tareas (CRUD Práctica 2 Conservado)
// ----------------------------------------------------------------------
@Serializable
data class TaskItem(
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val status: String = "pending", // "pending" | "completed"
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user_id") val userId: Int = 0
)

@Serializable
data class TasksResponse(
    val status: String,
    val tasks: List<TaskItem> = emptyList()
)

@Serializable
data class SingleTaskResponse(
    val status: String,
    val message: String? = null,
    val task: TaskItem? = null
)

// ----------------------------------------------------------------------
// Modelos de API Pública (TVMaze API)
// ----------------------------------------------------------------------
@Serializable
data class TvImage(
    val medium: String? = null,
    val original: String? = null
)

@Serializable
data class TvRating(
    val average: Double? = null
)

@Serializable
data class TvSchedule(
    val time: String? = "",
    val days: List<String> = emptyList()
)

@Serializable
data class TvShow(
    val id: Long,
    val name: String,
    val type: String? = null,
    val language: String? = null,
    val genres: List<String> = emptyList(),
    val status: String? = null,
    val runtime: Int? = null,
    val premiered: String? = null,
    val rating: TvRating? = null,
    val image: TvImage? = null,
    val summary: String? = null,
    val schedule: TvSchedule? = null
) {
    val cleanSummary: String
        get() = summary?.replace(Regex("<.*?>"), "")?.trim() ?: "Sin descripción disponible."

    val genresDisplay: String
        get() = if (genres.isEmpty()) "General" else genres.joinToString(", ")
}

@Serializable
data class TvSearchResult(
    val score: Double? = 0.0,
    val show: TvShow
)

// ----------------------------------------------------------------------
// Modelos de Historial de Búsquedas (Práctica 5 - Ejercicio 3.1)
// ----------------------------------------------------------------------
@Serializable
data class SearchHistoryItem(
    val id: Int = 0,
    val query: String,
    val category: String = "tv_shows",
    val timestamp: String? = null,
    @SerialName("user_id") val userId: Int = 0
)

@Serializable
data class HistoryResponse(
    val status: String,
    val history: List<SearchHistoryItem> = emptyList()
)

// ----------------------------------------------------------------------
// Modelos de Favoritos (Práctica 5 - Ejercicio 2 y 3)
// ----------------------------------------------------------------------
@Serializable
data class FavoriteItem(
    val id: Int = 0,
    @SerialName("item_id") val itemId: String,
    val title: String,
    val genres: String = "",
    val rating: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    val summary: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user_id") val userId: Int = 0
)

@Serializable
data class FavoritesResponse(
    val status: String,
    val favorites: List<FavoriteItem> = emptyList()
)

// ----------------------------------------------------------------------
// Modelos de Sincronización Offline y Cola de Acciones (Ejercicio 1.2)
// ----------------------------------------------------------------------
@Serializable
data class OfflineAction(
    val action: String, // "add" | "remove"
    @SerialName("item_id") val itemId: String,
    val title: String? = null,
    val genres: String? = null,
    val rating: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val summary: String? = null,
    val timestamp: Long = 0L
)

@Serializable
data class SyncPayload(
    val actions: List<OfflineAction> = emptyList()
)

@Serializable
data class SyncResponse(
    val status: String,
    val message: String,
    @SerialName("processed_count") val processedCount: Int = 0,
    @SerialName("last_sync_timestamp") val lastSyncTimestamp: String? = null,
    val favorites: List<FavoriteItem> = emptyList()
)

@Serializable
data class SyncStatusCounts(
    val tasks: Int = 0,
    val favorites: Int = 0,
    val history: Int = 0
)

@Serializable
data class SyncStatusResponse(
    val status: String,
    @SerialName("server_time") val serverTime: String,
    val counts: SyncStatusCounts
)

// ----------------------------------------------------------------------
// Modelos de Auditoría de Administrador
// ----------------------------------------------------------------------
@Serializable
data class AdminAuditHistoryResponse(
    val status: String,
    @SerialName("target_user") val targetUser: UserData? = null,
    val history: List<SearchHistoryItem> = emptyList()
)

@Serializable
data class AdminAuditFavoritesResponse(
    val status: String,
    @SerialName("target_user") val targetUser: UserData? = null,
    val favorites: List<FavoriteItem> = emptyList()
)

// ----------------------------------------------------------------------
// Modelos de Recomendación Verificable (Ejercicio 3.2)
// ----------------------------------------------------------------------
@Serializable
data class RecommendationItem(
    val showId: Long,
    val title: String,
    val genres: List<String>,
    val rating: Double?,
    val imageUrl: String?,
    val summary: String?,
    val ruleReason: String, // Explicación determinista del por qué se recomienda
    val matchScore: Double // Puntuación de coincidencia de géneros
)
