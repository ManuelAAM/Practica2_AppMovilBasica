package ovh.gabrielhuav.flasklogin.shared.cache

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ovh.gabrielhuav.flasklogin.shared.model.FavoriteItem
import ovh.gabrielhuav.flasklogin.shared.model.OfflineAction
import ovh.gabrielhuav.flasklogin.shared.model.RecommendationItem
import ovh.gabrielhuav.flasklogin.shared.model.SearchHistoryItem
import ovh.gabrielhuav.flasklogin.shared.model.TaskItem
import ovh.gabrielhuav.flasklogin.shared.model.TvShow
import ovh.gabrielhuav.flasklogin.shared.model.UserData

/**
 * Gestor de persistencia y caché local multiplataforma (KMP).
 * Permite almacenar y consultar datos en memoria y simular almacenamiento
 * estructurado persistente para Android e iOS en modo offline.
 */
class LocalCacheManager {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    // Almacenamiento reactivo local
    private val cachedTasks = mutableListOf<TaskItem>()
    private val cachedFavorites = mutableListOf<FavoriteItem>()
    private val cachedHistory = mutableListOf<SearchHistoryItem>()
    private val cachedTvShows = mutableMapOf<Long, TvShow>()
    private val cachedRecommendations = mutableListOf<RecommendationItem>()
    private val offlineQueue = mutableListOf<OfflineAction>()

    var activeSessionUser: UserData? = null
    var activeToken: String? = null
    var lastSyncTimestamp: String = "No sincronizado aún"

    // ----------------------------------------------------------------------
    // Tareas (CRUD Local / Offline)
    // ----------------------------------------------------------------------
    fun saveTasks(tasks: List<TaskItem>) {
        cachedTasks.clear()
        cachedTasks.addAll(tasks)
        updateTimestamp()
    }

    fun getTasks(): List<TaskItem> = cachedTasks.toList()

    fun addTaskLocally(task: TaskItem) {
        cachedTasks.add(0, task)
    }

    fun updateTaskLocally(taskId: Int, status: String, title: String? = null, description: String? = null) {
        val index = cachedTasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val old = cachedTasks[index]
            cachedTasks[index] = old.copy(
                status = status,
                title = title ?: old.title,
                description = description ?: old.description
            )
        }
    }

    fun deleteTaskLocally(taskId: Int) {
        cachedTasks.removeAll { it.id == taskId }
    }

    // ----------------------------------------------------------------------
    // Favoritos y Cola de Operaciones Offline (Ejercicio 1.2)
    // ----------------------------------------------------------------------
    fun saveFavorites(favorites: List<FavoriteItem>) {
        cachedFavorites.clear()
        cachedFavorites.addAll(favorites)
        updateTimestamp()
    }

    fun getFavorites(): List<FavoriteItem> = cachedFavorites.toList()

    fun isFavorite(itemId: String): Boolean {
        return cachedFavorites.any { it.itemId == itemId }
    }

    /**
     * Encola una acción de favoritos cuando la aplicación se encuentra en modo offline
     * y actualiza el caché local inmediatamente para una UX reactiva sin bloqueos.
     */
    fun enqueueOfflineAction(action: OfflineAction) {
        // Idempotencia local: si ya existe una acción opuesta pendiente para el mismo item, resolverla
        offlineQueue.removeAll { it.itemId == action.itemId }
        offlineQueue.add(action)

        if (action.action == "add") {
            if (cachedFavorites.none { it.itemId == action.itemId }) {
                cachedFavorites.add(
                    0,
                    FavoriteItem(
                        id = (1000..9999).random(),
                        itemId = action.itemId,
                        title = action.title ?: "Show ${action.itemId}",
                        genres = action.genres ?: "",
                        rating = action.rating ?: "",
                        imageUrl = action.imageUrl ?: "",
                        summary = action.summary ?: ""
                    )
                )
            }
        } else if (action.action == "remove") {
            cachedFavorites.removeAll { it.itemId == action.itemId }
        }
    }

    fun getPendingOfflineActions(): List<OfflineAction> = offlineQueue.toList()

    fun clearPendingOfflineActions() {
        offlineQueue.clear()
    }

    fun pendingOfflineActionsCount(): Int = offlineQueue.size

    // ----------------------------------------------------------------------
    // Historial de Búsquedas
    // ----------------------------------------------------------------------
    fun saveHistory(history: List<SearchHistoryItem>) {
        cachedHistory.clear()
        cachedHistory.addAll(history)
    }

    fun getHistory(): List<SearchHistoryItem> = cachedHistory.toList()

    fun addHistoryQueryLocally(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            cachedHistory.removeAll { it.query.equals(trimmed, ignoreCase = true) }
            cachedHistory.add(0, SearchHistoryItem(query = trimmed, timestamp = currentFormattedTime()))
        }
    }

    fun clearHistoryLocally() {
        cachedHistory.clear()
    }

    // ----------------------------------------------------------------------
    // Caché de Series de TV (TVMaze)
    // ----------------------------------------------------------------------
    fun cacheTvShow(show: TvShow) {
        cachedTvShows[show.id] = show
    }

    fun getCachedTvShow(id: Long): TvShow? = cachedTvShows[id]

    fun getAllCachedTvShows(): List<TvShow> = cachedTvShows.values.toList()

    // ----------------------------------------------------------------------
    // Recomendaciones
    // ----------------------------------------------------------------------
    fun saveRecommendations(recommendations: List<RecommendationItem>) {
        cachedRecommendations.clear()
        cachedRecommendations.addAll(recommendations)
    }

    fun getRecommendations(): List<RecommendationItem> = cachedRecommendations.toList()

    // ----------------------------------------------------------------------
    // Gestión de Estampas Temporales
    // ----------------------------------------------------------------------
    fun updateTimestamp(customTimestamp: String? = null) {
        lastSyncTimestamp = customTimestamp ?: currentFormattedTime()
    }

    private fun currentFormattedTime(): String {
        return try {
            val now: Instant = Clock.System.now()
            val dt = now.toLocalDateTime(TimeZone.currentSystemDefault())
            "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}-${dt.dayOfMonth.toString().padStart(2, '0')} " +
                    "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}:${dt.second.toString().padStart(2, '0')}"
        } catch (e: Exception) {
            "Reciente"
        }
    }
}
