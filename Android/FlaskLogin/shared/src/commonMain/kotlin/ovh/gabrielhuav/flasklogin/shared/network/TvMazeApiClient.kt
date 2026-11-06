package ovh.gabrielhuav.flasklogin.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import ovh.gabrielhuav.flasklogin.shared.model.TvSearchResult
import ovh.gabrielhuav.flasklogin.shared.model.TvShow

class TvMazeApiClient(
    private val client: HttpClient = createPlatformHttpClient()
) {
    companion object {
        const val BASE_URL = "https://api.tvmaze.com"
    }

    /**
     * Búsqueda por texto en tiempo real contra TVMaze API.
     * Retorna una lista tipada de resultados con puntuación y ficha de show.
     */
    suspend fun searchShows(query: String): Result<List<TvSearchResult>> {
        return try {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                return Result.success(emptyList())
            }

            val response = client.get("$BASE_URL/search/shows") {
                parameter("q", trimmed)
            }

            if (response.status == HttpStatusCode.OK) {
                val results: List<TvSearchResult> = response.body()
                Result.success(results)
            } else {
                Result.failure(Exception("Error HTTP ${response.status.value}: ${response.status.description}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Consulta de detalle de un show por su identificador numérico.
     */
    suspend fun getShowDetails(showId: Long): Result<TvShow> {
        return try {
            val response = client.get("$BASE_URL/shows/$showId")
            if (response.status == HttpStatusCode.OK) {
                val show: TvShow = response.body()
                Result.success(show)
            } else {
                Result.failure(Exception("Error HTTP ${response.status.value}: ${response.status.description}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
