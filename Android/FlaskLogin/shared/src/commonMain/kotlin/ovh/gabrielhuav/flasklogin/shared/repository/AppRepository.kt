package ovh.gabrielhuav.flasklogin.shared.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ovh.gabrielhuav.flasklogin.shared.cache.LocalCacheManager
import ovh.gabrielhuav.flasklogin.shared.engine.RecommendationEngine
import ovh.gabrielhuav.flasklogin.shared.model.AdminAuditFavoritesResponse
import ovh.gabrielhuav.flasklogin.shared.model.AdminAuditHistoryResponse
import ovh.gabrielhuav.flasklogin.shared.model.AdminUsersResponse
import ovh.gabrielhuav.flasklogin.shared.model.AuthResponse
import ovh.gabrielhuav.flasklogin.shared.model.FavoriteItem
import ovh.gabrielhuav.flasklogin.shared.model.GoogleAuthRequest
import ovh.gabrielhuav.flasklogin.shared.model.OfflineAction
import ovh.gabrielhuav.flasklogin.shared.model.RecommendationItem
import ovh.gabrielhuav.flasklogin.shared.model.SearchHistoryItem
import ovh.gabrielhuav.flasklogin.shared.model.TaskItem
import ovh.gabrielhuav.flasklogin.shared.model.TvSearchResult
import ovh.gabrielhuav.flasklogin.shared.model.TvShow
import ovh.gabrielhuav.flasklogin.shared.model.UserData
import ovh.gabrielhuav.flasklogin.shared.network.FlaskApiClient
import ovh.gabrielhuav.flasklogin.shared.network.TvMazeApiClient

class AppRepository(
    val flaskApi: FlaskApiClient = FlaskApiClient(),
    val tvMazeApi: TvMazeApiClient = TvMazeApiClient(),
    val cache: LocalCacheManager = LocalCacheManager()
) {

    private val _isOnlineFlow = MutableStateFlow(true)
    val isOnlineFlow: StateFlow<Boolean> = _isOnlineFlow.asStateFlow()

    fun setNetworkConnectivity(isOnline: Boolean) {
        _isOnlineFlow.value = isOnline
    }

    val isOnline: Boolean
        get() = _isOnlineFlow.value

    val currentToken: String?
        get() = cache.activeToken

    val currentUser: UserData?
        get() = cache.activeSessionUser

    fun setSession(token: String, user: UserData) {
        cache.activeToken = token
        cache.activeSessionUser = user
    }

    fun clearSession() {
        cache.activeToken = null
        cache.activeSessionUser = null
    }

    // ----------------------------------------------------------------------
    // Autenticación
    // ----------------------------------------------------------------------
    suspend fun login(username: String, pass: String): Result<AuthResponse> {
        val res = flaskApi.login(username, pass)
        res.onSuccess { auth ->
            if (auth.token != null) {
                setSession(
                    auth.token,
                    UserData(
                        id = auth.userId ?: 0,
                        username = auth.username ?: username,
                        role = auth.role ?: "user",
                        authProvider = "native"
                    )
                )
            }
        }
        return res
    }

    suspend fun register(username: String, pass: String): Result<AuthResponse> {
        return flaskApi.register(username, pass)
    }

    suspend fun googleAuth(email: String, name: String, googleId: String): Result<AuthResponse> {
        val req = GoogleAuthRequest(email = email, name = name, googleId = googleId)
        val res = flaskApi.googleAuth(req)
        res.onSuccess { auth ->
            if (auth.token != null) {
                setSession(
                    auth.token,
                    UserData(
                        id = auth.userId ?: 0,
                        username = auth.username ?: email.split("@")[0],
                        role = auth.role ?: "user",
                        authProvider = "google"
                    )
                )
            }
        }
        return res
    }

    // ----------------------------------------------------------------------
    // Tareas (CRUD con Soporte Offline y Caché)
    // ----------------------------------------------------------------------
    suspend fun getTasks(forceRefresh: Boolean = false): Result<List<TaskItem>> {
        val token = currentToken
        if (token != null && isOnline) {
            val remoteRes = flaskApi.getTasks(token)
            if (remoteRes.isSuccess) {
                val tasks = remoteRes.getOrNull()?.tasks ?: emptyList()
                cache.saveTasks(tasks)
                return Result.success(tasks)
            }
        }
        // Fallback offline a caché local
        return Result.success(cache.getTasks())
    }

    suspend fun createTask(title: String, description: String): Result<TaskItem> {
        val token = currentToken
        if (token != null && isOnline) {
            val remoteRes = flaskApi.createTask(token, title, description)
            if (remoteRes.isSuccess) {
                val created = remoteRes.getOrNull()?.task
                if (created != null) {
                    cache.addTaskLocally(created)
                    return Result.success(created)
                }
            }
        }
        // Creación local offline
        val localTask = TaskItem(
            id = (1000..9999).random(),
            title = title,
            description = description,
            status = "pending",
            userId = currentUser?.id ?: 0
        )
        cache.addTaskLocally(localTask)
        return Result.success(localTask)
    }

    suspend fun updateTask(taskId: Int, status: String, title: String? = null, description: String? = null): Result<Boolean> {
        val token = currentToken
        if (token != null && isOnline) {
            flaskApi.updateTask(token, taskId, status, title, description)
        }
        cache.updateTaskLocally(taskId, status, title, description)
        return Result.success(true)
    }

    suspend fun deleteTask(taskId: Int): Result<Boolean> {
        val token = currentToken
        if (token != null && isOnline) {
            flaskApi.deleteTask(token, taskId)
        }
        cache.deleteTaskLocally(taskId)
        return Result.success(true)
    }

    // ----------------------------------------------------------------------
    // Búsqueda en API Pública (TVMaze) e Historial
    // ----------------------------------------------------------------------
    suspend fun searchTvShows(query: String): Result<List<TvSearchResult>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return Result.success(emptyList())

        // Registrar en historial local
        cache.addHistoryQueryLocally(trimmed)

        // Registrar en backend si hay sesión y red
        val token = currentToken
        if (token != null && isOnline) {
            try {
                flaskApi.addSearchHistory(token, trimmed)
            } catch (e: Exception) {
                // Silencioso para no bloquear la búsqueda principal
            }
        }

        // Consultar API pública de TVMaze
        if (isOnline) {
            val res = tvMazeApi.searchShows(trimmed)
            res.onSuccess { results ->
                results.forEach { r -> cache.cacheTvShow(r.show) }
            }
            if (res.isSuccess) return res
        }

        // Si no hay red, buscar coincidencias en el caché local
        val localMatches = cache.getAllCachedTvShows()
            .filter { it.name.contains(trimmed, ignoreCase = true) || it.genresDisplay.contains(trimmed, ignoreCase = true) }
            .map { TvSearchResult(score = 1.0, show = it) }

        return Result.success(localMatches)
    }

    suspend fun getShowDetails(showId: Long): Result<TvShow> {
        val cached = cache.getCachedTvShow(showId)
        if (cached != null) return Result.success(cached)

        if (isOnline) {
            val res = tvMazeApi.getShowDetails(showId)
            res.onSuccess { cache.cacheTvShow(it) }
            return res
        }

        return Result.failure(Exception("Sin conexión a internet y el show no está en caché."))
    }

    suspend fun getHistory(): List<SearchHistoryItem> {
        val token = currentToken
        if (token != null && isOnline) {
            val remoteRes = flaskApi.getSearchHistory(token)
            remoteRes.onSuccess { cache.saveHistory(it.history) }
        }
        return cache.getHistory()
    }

    suspend fun clearHistory(): Result<Boolean> {
        val token = currentToken
        if (token != null && isOnline) {
            flaskApi.clearSearchHistory(token)
        }
        cache.clearHistoryLocally()
        return Result.success(true)
    }

    // ----------------------------------------------------------------------
    // Favoritos y Sincronización de Cola Offline
    // ----------------------------------------------------------------------
    suspend fun getFavorites(forceRefresh: Boolean = false): List<FavoriteItem> {
        val token = currentToken
        if (token != null && isOnline && (forceRefresh || cache.getFavorites().isEmpty())) {
            val remoteRes = flaskApi.getFavorites(token)
            remoteRes.onSuccess { cache.saveFavorites(it.favorites) }
        }
        return cache.getFavorites()
    }

    suspend fun toggleFavorite(show: TvShow): Boolean {
        val itemId = show.id.toString()
        val isFav = cache.isFavorite(itemId)
        val token = currentToken

        if (isFav) {
            // Quitar de favoritos
            if (token != null && isOnline) {
                flaskApi.removeFavorite(token, itemId)
                val current = cache.getFavorites().filterNot { it.itemId == itemId }
                cache.saveFavorites(current)
            } else {
                // Encolar acción offline
                cache.enqueueOfflineAction(OfflineAction(action = "remove", itemId = itemId))
            }
            return false
        } else {
            // Añadir a favoritos
            val action = OfflineAction(
                action = "add",
                itemId = itemId,
                title = show.name,
                genres = show.genres.joinToString(", "),
                rating = show.rating?.average?.toString() ?: "",
                imageUrl = show.image?.medium ?: "",
                summary = show.cleanSummary
            )

            if (token != null && isOnline) {
                flaskApi.addFavorite(
                    token = token,
                    itemId = itemId,
                    title = show.name,
                    genres = show.genres.joinToString(", "),
                    rating = show.rating?.average?.toString() ?: "",
                    imageUrl = show.image?.medium ?: "",
                    summary = show.cleanSummary
                )
                val current = cache.getFavorites().toMutableList()
                current.add(
                    0,
                    FavoriteItem(
                        id = (1000..9999).random(),
                        itemId = itemId,
                        title = show.name,
                        genres = show.genres.joinToString(", "),
                        rating = show.rating?.average?.toString() ?: "",
                        imageUrl = show.image?.medium ?: "",
                        summary = show.cleanSummary
                    )
                )
                cache.saveFavorites(current)
            } else {
                cache.enqueueOfflineAction(action)
            }
            return true
        }
    }

    /**
     * Sincroniza la cola de operaciones acumuladas sin conexión con el backend REST.
     * Retorna la cantidad de operaciones resueltas.
     */
    suspend fun syncPendingOfflineActions(): Result<Int> {
        val token = currentToken ?: return Result.failure(Exception("Sesión requerida"))
        val pendingActions = cache.getPendingOfflineActions()

        if (pendingActions.isEmpty()) {
            // Si no hay acciones pendientes, simplemente refrescar estado
            val favsRes = flaskApi.getFavorites(token)
            favsRes.onSuccess { cache.saveFavorites(it.favorites) }
            val statusRes = flaskApi.getSyncStatus(token)
            statusRes.onSuccess { cache.updateTimestamp(it.serverTime) }
            return Result.success(0)
        }

        val syncRes = flaskApi.syncOfflineFavorites(token, pendingActions)
        return if (syncRes.isSuccess) {
            val response = syncRes.getOrNull()
            cache.clearPendingOfflineActions()
            if (response != null) {
                cache.saveFavorites(response.favorites)
                cache.updateTimestamp(response.lastSyncTimestamp)
                Result.success(response.processedCount)
            } else {
                Result.success(pendingActions.size)
            }
        } else {
            Result.failure(syncRes.exceptionOrNull() ?: Exception("Error de sincronización"))
        }
    }

    // ----------------------------------------------------------------------
    // Recomendaciones Verificables (Ejercicio 3.2)
    // ----------------------------------------------------------------------
    suspend fun getRecommendations(): List<RecommendationItem> {
        val favs = getFavorites()
        val history = getHistory()
        val cachedShows = cache.getAllCachedTvShows()

        val recommendations = RecommendationEngine.generateRecommendations(
            favorites = favs,
            history = history,
            extraCandidates = cachedShows
        )
        cache.saveRecommendations(recommendations)
        return recommendations
    }

    // ----------------------------------------------------------------------
    // Auditoría Administrativa (Solo Administrador)
    // ----------------------------------------------------------------------
    suspend fun adminGetUsers(): Result<AdminUsersResponse> {
        val token = currentToken ?: return Result.failure(Exception("Sesión requerida"))
        return flaskApi.adminGetUsers(token)
    }

    suspend fun adminGetUserHistory(userId: Int): Result<AdminAuditHistoryResponse> {
        val token = currentToken ?: return Result.failure(Exception("Sesión requerida"))
        return flaskApi.adminGetUserHistory(token, userId)
    }

    suspend fun adminGetUserFavorites(userId: Int): Result<AdminAuditFavoritesResponse> {
        val token = currentToken ?: return Result.failure(Exception("Sesión requerida"))
        return flaskApi.adminGetUserFavorites(token, userId)
    }
}
