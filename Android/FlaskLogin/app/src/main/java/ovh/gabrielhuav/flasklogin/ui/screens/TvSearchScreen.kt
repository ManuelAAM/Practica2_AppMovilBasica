package ovh.gabrielhuav.flasklogin.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ovh.gabrielhuav.flasklogin.shared.model.TvSearchResult
import ovh.gabrielhuav.flasklogin.shared.model.TvShow
import ovh.gabrielhuav.flasklogin.shared.repository.AppRepository

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TvSearchScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<TvSearchResult>>(emptyList()) }
    var searchHistory by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var selectedShowForDetail by remember { mutableStateOf<TvShow?>(null) }
    var favoritesIds by remember { mutableStateOf(setOf<String>()) }

    // Cargar historial y favoritos al iniciar
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Explorador de Series (TVMaze API)",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Consulta en tiempo real con historial y persistencia offline",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campo de Búsqueda
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
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

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

        // Historial de Búsquedas (Chips horizontales)
        if (searchHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
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
                searchHistory.take(6).forEach { term ->
                    AssistChip(
                        onClick = { performSearch(term) },
                        label = { Text(term) },
                        leadingIcon = {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Resultados
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Consultando TVMaze API...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Error al consultar API",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { performSearch(searchQuery) },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            } else if (hasSearched && searchResults.isEmpty()) {
                // Estado Vacío (Práctica 5 - Verificación Obligatoria Escenario 3)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Sin resultados para \"$searchQuery\"",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No se encontraron series que coincidan con tu término. Intenta con nombres en inglés o términos más generales.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else if (!hasSearched) {
                // Sugerencias iniciales si no ha buscado
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Busca tus series favoritas",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Ingresa un título arriba para consultar sinopsis, calificación y géneros en TVMaze.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(searchResults) { result ->
                        val show = result.show
                        val isFav = favoritesIds.contains(show.id.toString())

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedShowForDetail = show },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Tarjeta visual de icono/tipo
                                Surface(
                                    modifier = Modifier.size(56.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
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
                                        // Botón de Favorito
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val nowFav = repository.toggleFavorite(show)
                                                    favoritesIds = if (nowFav) {
                                                        favoritesIds + show.id.toString()
                                                    } else {
                                                        favoritesIds - show.id.toString()
                                                    }
                                                    val msg = if (nowFav) "¡Añadida a favoritos!" else "Eliminada de favoritos"
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = "Favorito",
                                                tint = if (isFav) Color(0xFFFFA000) else Color.Gray
                                            )
                                        }
                                    }

                                    // Rating y Estado
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

                                    // Géneros
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

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = show.cleanSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Detalle Completo de la Serie
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
                            favoritesIds = if (nowFav) {
                                favoritesIds + show.id.toString()
                            } else {
                                favoritesIds - show.id.toString()
                            }
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
                    Text(if (isFav) "Quitar de Favoritos" else "Guardar en Favoritos")
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
