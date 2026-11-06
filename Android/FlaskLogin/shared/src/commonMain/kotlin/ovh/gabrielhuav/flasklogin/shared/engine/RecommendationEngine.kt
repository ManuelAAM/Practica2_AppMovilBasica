package ovh.gabrielhuav.flasklogin.shared.engine

import ovh.gabrielhuav.flasklogin.shared.model.FavoriteItem
import ovh.gabrielhuav.flasklogin.shared.model.RecommendationItem
import ovh.gabrielhuav.flasklogin.shared.model.SearchHistoryItem
import ovh.gabrielhuav.flasklogin.shared.model.TvImage
import ovh.gabrielhuav.flasklogin.shared.model.TvRating
import ovh.gabrielhuav.flasklogin.shared.model.TvShow

object RecommendationEngine {

    /**
     * Catálogo base de series emblemáticas de TVMaze precargadas para evaluación determinista
     * y reproducción fidedigna tanto online como en modo offline (Cold Start y candidatos).
     */
    val candidateShows: List<TvShow> = listOf(
        TvShow(
            id = 169,
            name = "Breaking Bad",
            genres = listOf("Drama", "Crime", "Thriller"),
            rating = TvRating(9.2),
            image = TvImage(
                medium = "https://static.tvmaze.com/uploads/images/medium_portrait/0/2400.jpg",
                original = "https://static.tvmaze.com/uploads/images/original_untouched/0/2400.jpg"
            ),
            summary = "Walter White, un profesor de química diagnosticado con cáncer, se adentra en el bajo mundo del narcotráfico."
        ),
        TvShow(
            id = 2993,
            name = "Stranger Things",
            genres = listOf("Drama", "Fantasy", "Horror", "Sci-Fi"),
            rating = TvRating(8.6),
            image = TvImage(
                medium = "https://static.tvmaze.com/uploads/images/medium_portrait/394/986240.jpg",
                original = "https://static.tvmaze.com/uploads/images/original_untouched/394/986240.jpg"
            ),
            summary = "En Hawkins, Indiana, un grupo de niños descubre experimentos secretos y una niña con poderes telequinéticos."
        ),
        TvShow(
            id = 139,
            name = "Girls",
            genres = listOf("Drama", "Romance", "Comedy"),
            rating = TvRating(6.6),
            image = TvImage(
                medium = "https://static.tvmaze.com/uploads/images/medium_portrait/31/78286.jpg",
                original = "https://static.tvmaze.com/uploads/images/original_untouched/31/78286.jpg"
            ),
            summary = "La vida de cuatro jóvenes veinteañeras que intentan abrirse paso en la ciudad de Nueva York."
        ),
        TvShow(
            id = 82,
            name = "Game of Thrones",
            genres = listOf("Drama", "Action", "Fantasy", "Adventure"),
            rating = TvRating(9.0),
            image = TvImage(
                medium = "https://static.tvmaze.com/uploads/images/medium_portrait/190/476117.jpg",
                original = "https://static.tvmaze.com/uploads/images/original_untouched/190/476117.jpg"
            ),
            summary = "Nueve familias nobles luchan por el control sobre las tierras míticas de Poniente."
        ),
        TvShow(
            id = 17861,
            name = "Dark",
            genres = listOf("Drama", "Mystery", "Sci-Fi", "Crime"),
            rating = TvRating(8.9),
            image = TvImage(
                medium = "https://static.tvmaze.com/uploads/images/medium_portrait/150/376043.jpg",
                original = "https://static.tvmaze.com/uploads/images/original_untouched/150/376043.jpg"
            ),
            summary = "La desaparición de dos niños en la ciudad de Winden saca a la luz relaciones fracturadas y viajes en el tiempo."
        ),
        TvShow(
            id = 305,
            name = "Black Mirror",
            genres = listOf("Drama", "Sci-Fi", "Thriller"),
            rating = TvRating(8.2),
            image = TvImage(
                medium = "https://static.tvmaze.com/uploads/images/medium_portrait/462/1155097.jpg",
                original = "https://static.tvmaze.com/uploads/images/original_untouched/462/1155097.jpg"
            ),
            summary = "Serie antológica de ciencia ficción que explora el lado oscuro de la tecnología y la condición humana."
        ),
        TvShow(
            id = 431,
            name = "Friends",
            genres = listOf("Comedy", "Romance"),
            rating = TvRating(8.8),
            image = TvImage(
                medium = "https://static.tvmaze.com/uploads/images/medium_portrait/41/104550.jpg",
                original = "https://static.tvmaze.com/uploads/images/original_untouched/41/104550.jpg"
            ),
            summary = "Seis amigos veinteañeros comparten aventuras, amores y risas en Manhattan."
        ),
        TvShow(
            id = 526,
            name = "The Office",
            genres = listOf("Comedy"),
            rating = TvRating(8.9),
            image = TvImage(
                medium = "https://static.tvmaze.com/uploads/images/medium_portrait/450/1126786.jpg",
                original = "https://static.tvmaze.com/uploads/images/original_untouched/450/1126786.jpg"
            ),
            summary = "Comedia en formato falso documental sobre el día a día de los empleados de la papelera Dunder Mifflin."
        ),
        TvShow(
            id = 41349,
            name = "Arcane",
            genres = listOf("Action", "Adventure", "Animation", "Fantasy", "Sci-Fi"),
            rating = TvRating(9.0),
            image = TvImage(
                medium = "https://static.tvmaze.com/uploads/images/medium_portrait/526/1315577.jpg",
                original = "https://static.tvmaze.com/uploads/images/original_untouched/526/1315577.jpg"
            ),
            summary = "En medio del conflicto entre las ciudades gemelas Piltover y Zaun, dos hermanas luchan en bandos opuestos."
        )
    )

    /**
     * Genera recomendaciones verificables utilizando el historial y favoritos del usuario.
     * Regla Reproducible: Frecuencia de Géneros (Genre Frequency Matching).
     *
     * 1. Extrae los géneros preferidos de los favoritos (peso x2) e historial de búsquedas (peso x1).
     * 2. Pondera las series candidatas por el solapamiento de géneros y calificación.
     * 3. Excluye series que ya estén en favoritos.
     * 4. Garantiza un mínimo de 3 sugerencias con su respectiva justificación explicable.
     * 5. Resuelve el estado frío (sin historial ni favoritos) mostrando series destacadas de alta calificación.
     */
    fun generateRecommendations(
        favorites: List<FavoriteItem>,
        history: List<SearchHistoryItem>,
        extraCandidates: List<TvShow> = emptyList()
    ): List<RecommendationItem> {
        val pool = (candidateShows + extraCandidates).distinctBy { it.id }
        val favoriteIds = favorites.map { it.itemId }.toSet()

        // 1. Calcular histograma de frecuencias de género
        val genreFrequency = mutableMapOf<String, Int>()

        favorites.forEach { fav ->
            val genresList = fav.genres.split(",").map { it.trim() }.filter { it.isNotBlank() }
            genresList.forEach { g ->
                genreFrequency[g] = (genreFrequency[g] ?: 0) + 2 // Peso doble a favoritos
            }
        }

        history.forEach { hist ->
            val queryUpper = hist.query.uppercase()
            pool.forEach { show ->
                if (show.name.uppercase().contains(queryUpper)) {
                    show.genres.forEach { g ->
                        genreFrequency[g] = (genreFrequency[g] ?: 0) + 1
                    }
                }
            }
        }

        // Si no hay favoritos ni historial con coincidencia (Cold Start)
        if (genreFrequency.isEmpty() || favorites.isEmpty()) {
            return pool
                .filter { it.id.toString() !in favoriteIds }
                .sortedByDescending { it.rating?.average ?: 0.0 }
                .take(4)
                .map { show ->
                    RecommendationItem(
                        showId = show.id,
                        title = show.name,
                        genres = show.genres,
                        rating = show.rating?.average,
                        imageUrl = show.image?.medium,
                        summary = show.cleanSummary,
                        ruleReason = "Sugerencia inicial destacada por alta calificación (${show.rating?.average ?: "N/A"} ⭐). Añade series a Favoritos para personalizar tus recomendaciones.",
                        matchScore = show.rating?.average ?: 5.0
                    )
                }
        }

        // Top géneros del usuario
        val topGenres = genreFrequency.entries.sortedByDescending { it.value }.take(3).map { it.key }

        // 2. Puntuar candidatos
        val scoredList = pool
            .filter { it.id.toString() !in favoriteIds }
            .map { show ->
                var overlapScore = 0.0
                val matchedGenres = mutableListOf<String>()

                show.genres.forEach { g ->
                    val weight = genreFrequency[g] ?: 0
                    if (weight > 0) {
                        overlapScore += weight
                        matchedGenres.add(g)
                    }
                }

                // Bonificación por rating normalizado
                val ratingScore = (show.rating?.average ?: 6.0) / 10.0 * 2.0
                val totalScore = overlapScore + ratingScore

                val reason = if (matchedGenres.isNotEmpty()) {
                    "Coincide con tus géneros preferidos: ${matchedGenres.joinToString(", ")} (Afinidad: ${totalScore.toInt()} pts)"
                } else {
                    "Sugerencia complementaria por alta popularidad (${show.genres.joinToString(", ")})"
                }

                RecommendationItem(
                    showId = show.id,
                    title = show.name,
                    genres = show.genres,
                    rating = show.rating?.average,
                    imageUrl = show.image?.medium,
                    summary = show.cleanSummary,
                    ruleReason = reason,
                    matchScore = totalScore
                )
            }
            .sortedByDescending { it.matchScore }

        // Garantizar al menos 3 recomendaciones
        val result = scoredList.take(5)
        return if (result.size >= 3) {
            result
        } else {
            // Completar con las mejores calificadas del pool si faltan
            val existingIds = result.map { it.showId }.toSet()
            val fillers = pool
                .filter { it.id !in existingIds && it.id.toString() !in favoriteIds }
                .sortedByDescending { it.rating?.average ?: 0.0 }
                .take(3 - result.size)
                .map { show ->
                    RecommendationItem(
                        showId = show.id,
                        title = show.name,
                        genres = show.genres,
                        rating = show.rating?.average,
                        imageUrl = show.image?.medium,
                        summary = show.cleanSummary,
                        ruleReason = "Sugerencia por catálogo y afinidad temática.",
                        matchScore = show.rating?.average ?: 5.0
                    )
                }
            result + fillers
        }
    }
}
