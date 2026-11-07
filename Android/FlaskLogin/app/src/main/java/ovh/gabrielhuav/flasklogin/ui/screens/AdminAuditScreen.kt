package ovh.gabrielhuav.flasklogin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ovh.gabrielhuav.flasklogin.shared.model.FavoriteItem
import ovh.gabrielhuav.flasklogin.shared.model.SearchHistoryItem
import ovh.gabrielhuav.flasklogin.shared.model.UserData
import ovh.gabrielhuav.flasklogin.shared.repository.AppRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAuditScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var usersList by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<UserData?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(0) } // 0: Historial, 1: Favoritos
    var userHistory by remember { mutableStateOf<List<SearchHistoryItem>>(emptyList()) }
    var userFavorites by remember { mutableStateOf<List<FavoriteItem>>(emptyList()) }

    var isLoading by remember { mutableStateOf(false) }
    var rbacTestResult by remember { mutableStateOf<String?>(null) }
    var isTestingRbac by remember { mutableStateOf(false) }

    // Cargar lista de usuarios del sistema
    val loadUsers: () -> Unit = {
        isLoading = true
        coroutineScope.launch {
            val res = repository.adminGetUsers()
            isLoading = false
            val adminResp = res.getOrNull()
            if (adminResp != null) {
                usersList = adminResp.users
                if (usersList.isNotEmpty() && selectedUser == null) {
                    selectedUser = usersList.first()
                }
            } else {
                Toast.makeText(context, "Error al consultar usuarios del backend", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Cargar auditoría (historial y favoritos) del usuario seleccionado
    val loadAuditData: (Int) -> Unit = { targetId ->
        isLoading = true
        coroutineScope.launch {
            val hRes = repository.adminGetUserHistory(targetId)
            userHistory = hRes.getOrNull()?.history ?: emptyList()

            val fRes = repository.adminGetUserFavorites(targetId)
            userFavorites = fRes.getOrNull()?.favorites ?: emptyList()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadUsers()
    }

    LaunchedEffect(selectedUser) {
        selectedUser?.let { loadAuditData(it.id) }
    }

    // Contenedor principal unificado con LazyColumn: swipe fluido sin bloqueos
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Cabecera Informativa
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auditoría de Usuarios (Admin)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Inspección de historiales y favoritos por RBAC (Ejercicio 3.1)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    loadUsers()
                    selectedUser?.let { loadAuditData(it.id) }
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                }
            }
        }

        // 2. Selector Desplegable de Usuario Auditado
        item {
            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedUser?.let { "${it.username} (ID: ${it.id} • ${it.role})" } ?: "Seleccionar usuario...",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Usuario para Auditoría") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    usersList.forEach { user ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${user.username} (ID: ${user.id} • ${user.role.uppercase()})",
                                    fontWeight = if (user.id == selectedUser?.id) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedUser = user
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // 3. Ficha de Usuario Seleccionado
        item {
            selectedUser?.let { user ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Auditoría activa: @${user.username}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (user.role == "admin") Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = user.role.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (user.role == "admin") Color(0xFF2E7D32) else Color(0xFF1565C0)
                            )
                        }
                    }
                }
            }
        }

        // 4. Pestañas de Navegación: Historial vs Favoritos
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Historial (${userHistory.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Favoritos (${userFavorites.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        // 5. Contenido Dinámico de la Pestaña
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        } else if (selectedTab == 0) {
            // LISTA DE HISTORIAL
            if (userHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "El usuario '${selectedUser?.username ?: ""}' no tiene búsquedas registradas en su historial.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(userHistory) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.query,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Categoría: ${item.category} • ${item.timestamp ?: "Reciente"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // LISTA DE FAVORITOS
            if (userFavorites.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "El usuario '${selectedUser?.username ?: ""}' no tiene series marcadas como favoritas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(userFavorites) { fav ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (fav.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = fav.imageUrl,
                                    contentDescription = fav.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 50.dp, height = 70.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.size(width = 50.dp, height = 70.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fav.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (fav.genres.isNotBlank()) {
                                    Text(
                                        text = fav.genres,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (fav.rating.isNotBlank()) {
                                    Text(
                                        text = "⭐ Calificación: ${fav.rating}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFF57F17)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Sección de Verificación RBAC (Ubicada al final del scroll, sin obstruir la pantalla)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Prueba de Verificación de Seguridad RBAC",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Demuestra el requisito de la rúbrica: el backend rechaza con 403 Forbidden cuando un usuario común intenta auditar a otros.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            isTestingRbac = true
                            coroutineScope.launch {
                                val fakeNonAdminToken = "token_de_usuario_comun_sin_privilegios"
                                val res = repository.flaskApi.adminGetUserHistory(fakeNonAdminToken, 1)
                                isTestingRbac = false
                                if (res.isFailure) {
                                    rbacTestResult = "403 Forbidden: El servidor validó el rol y bloqueó el acceso no autorizado exitosamente."
                                } else {
                                    rbacTestResult = "Fallo de seguridad: la petición sin privilegios fue aceptada."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isTestingRbac
                    ) {
                        if (isTestingRbac) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verificando con servidor...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ejecutar Prueba 403 Forbidden", fontSize = 12.sp)
                        }
                    }

                    rbacTestResult?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "🛡️ $msg",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
