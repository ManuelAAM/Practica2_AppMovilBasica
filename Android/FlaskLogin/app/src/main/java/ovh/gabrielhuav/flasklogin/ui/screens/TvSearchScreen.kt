package ovh.gabrielhuav.flasklogin.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ovh.gabrielhuav.flasklogin.shared.model.TvSearchResult
import ovh.gabrielhuav.flasklogin.shared.model.TvShow
import ovh.gabrielhuav.flasklogin.shared.repository.AppRepository

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TvSearchScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<TvSearchResult>>(emptyList()) }
    var searchHistory by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var selectedShowForDetail by remember { mutableStateOf<TvShow?>(null) }
    var favoritesIds by remember { mutableStateOf(setOf<String>()) }

    val isOnline by repository.isOnlineFlow.collectAsState()
    val allCachedShows = remember(searchResults, isOnline, hasSearched) { repository.cache.getAllCachedTvShows() }

    val reloadState: () -> Unit = {
        coroutineScope.launch {
            val hist = repository.getHistory()
            searchHistory = hist.map { it.query }.distinct()
            val favs = repository.getFavorites()
            favoritesIds = favs.map { it.itemId }.toSet()
        }
    }

    LaunchedEffect(Unit) {
        reloadState()
    }

    val performSearch: (String) -> Unit = { query ->
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            searchQuery = trimmed
            isLoading = true
            errorMessage = null
            hasSearched = true
            focusManager.clearFocus()
            coroutineScope.launch {
                val res = repository.searchTvShows(trimmed)
                isLoading = false
                if (res.isSuccess) {
                    searchResults = res.getOrDefault(emptyList())
                } else {
                    errorMessage = res.exceptionOrNull()?.message ?: "Error al consultar la API"
                    searchResults = emptyList()
                }
                reloadState()
            }
        }
    }

    // Contenedor unificado con LazyColumn: la cabecera y el buscador se deslizan naturalmente
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Título y Leyenda Informativa
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Explorador de Series (TVMaze API)",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Consulta en tiempo real con historial, portadas y persistencia offline",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 2. Banner de Estado Offline (si la red está inactiva o simulada)
        if (!isOnline) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF3E0),
                    border = BorderStroke(1.dp, Color(0xFFFFB74D))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Modo Offline activo: Las consultas se resuelven contra la memoria local.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFBF360C), fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }

        // 3. Campo de Búsqueda con Botón y Lupa
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ej. Breaking Bad, Stranger Things, Dark...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            hasSearched = false
                            searchResults = emptyList()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performSearch(searchQuery) }),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { performSearch(searchQuery) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buscar")
                }
            }
        }

        // 4. Historial de Búsquedas (Chips Horizontales)
        if (searchHistory.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Búsquedas recientes:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                repository.clearHistory()
                                reloadState()
                            }
                        }
                    ) {
                        Text("Borrar", fontSize = 11.sp)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    searchHistory.take(10).forEach { term ->
                        AssistChip(
                            onClick = { performSearch(term) },
                            label = { Text(term, fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        }

        // 5. Mensaje de Error (si hubo fallo de red)
        if (errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // 6. Contenido Principal: Resultados o Catálogo Local Offline
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }
            }
        } else if (hasSearched && searchResults.isNotEmpty()) {
            // RESULTADOS DE BÚSQUEDA ACTIVOS
            item {
                Text(
                    text = "Resultados encontrados (${searchResults.size}):",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(searchResults) { result ->
                ShowItemCard(
                    show = result.show,
                    isFav = favoritesIds.contains(result.show.id.toString()),
                    onClick = { selectedShowForDetail = result.show },
                    onToggleFav = {
                        coroutineScope.launch {
                            val nowFav = repository.toggleFavorite(result.show)
                            favoritesIds = if (nowFav) favoritesIds + result.show.id.toString() else favoritesIds - result.show.id.toString()
                            val msg = if (nowFav) "¡Añadida a favoritos!" else "Eliminada de favoritos"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        } else if (hasSearched && searchResults.isEmpty()) {
            // SIN RESULTADOS PARA LA BÚSQUEDA
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Sin resultados para \"$searchQuery\"",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (!isOnline) {
                                "Estás en modo sin conexión y esta serie no está en caché local. A continuación puedes ver las series disponibles offline:"
                            } else {
                                "No se encontraron coincidencias en TVMaze. Intenta con nombres en inglés o términos más generales."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Series en caché como alternativa
            if (allCachedShows.isNotEmpty()) {
                item {
                    Text(
                        text = "📺 Series disponibles en memoria local para ver sin conexión:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFE65100)
                    )
                }
                items(allCachedShows) { show ->
                    ShowItemCard(
                        show = show,
                        isFav = favoritesIds.contains(show.id.toString()),
                        onClick = { selectedShowForDetail = show },
                        onToggleFav = {
                            coroutineScope.launch {
                                val nowFav = repository.toggleFavorite(show)
                                favoritesIds = if (nowFav) favoritesIds + show.id.toString() else favoritesIds - show.id.toString()
                                val msg = if (nowFav) "¡Añadida a favoritos!" else "Eliminada de favoritos"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        } else {
            // ESTADO INICIAL (Aún no busca): Mostrar series disponibles en memoria local
            if (allCachedShows.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Series sugeridas (${allCachedShows.size}):",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "🟢 Disponible Offline",
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                items(allCachedShows) { show ->
                    ShowItemCard(
                        show = show,
                        isFav = favoritesIds.contains(show.id.toString()),
                        onClick = { selectedShowForDetail = show },
                        onToggleFav = {
                            coroutineScope.launch {
                                val nowFav = repository.toggleFavorite(show)
                                favoritesIds = if (nowFav) favoritesIds + show.id.toString() else favoritesIds - show.id.toString()
                                val msg = if (nowFav) "¡Añadida a favoritos!" else "Eliminada de favoritos"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal de Detalle de Serie con Portada Grande y Sinopsis
    selectedShowForDetail?.let { show ->
        val isFav = favoritesIds.contains(show.id.toString())
        AlertDialog(
            onDismissRequest = { selectedShowForDetail = null },
            title = {
                Text(
                    text = show.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    val detailPoster = show.image?.original ?: show.image?.medium
                    if (!detailPoster.isNullOrBlank()) {
                        AsyncImage(
                            model = detailPoster,
                            contentDescription = show.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Calificación: ⭐ ${show.rating?.average ?: "Sin calificar"}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF57F17)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Tipo: ${show.type ?: "Serie"}")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Géneros: ${show.genresDisplay}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    if (!show.premiered.isNullOrBlank()) {
                        Text(text = "Estreno: ${show.premiered}", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sinopsis:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = show.cleanSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val nowFav = repository.toggleFavorite(show)
                            favoritesIds = if (nowFav) favoritesIds + show.id.toString() else favoritesIds - show.id.toString()
                            val msg = if (nowFav) "¡Añadida a favoritos!" else "Eliminada de favoritos"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isFav) "Quitar de Favoritos" else "Agregar a Favoritos")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedShowForDetail = null }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

/**
 * Tarjeta reusable para mostrar series con su portada real mediante Coil AsyncImage.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShowItemCard(
    show: TvShow,
    isFav: Boolean,
    onClick: () -> Unit,
    onToggleFav: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            val posterUrl = show.image?.medium
            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = show.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 60.dp, height = 85.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Surface(
                    modifier = Modifier.size(width = 60.dp, height = 85.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = show.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onToggleFav) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorito",
                            tint = if (isFav) Color(0xFFFFA000) else Color.Gray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⭐ ${show.rating?.average ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFF57F17)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${show.status ?: "Emitida"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    show.genres.take(3).forEach { g ->
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = g,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
