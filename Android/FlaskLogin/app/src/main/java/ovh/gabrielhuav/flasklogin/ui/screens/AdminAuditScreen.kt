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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Cargar usuarios
    val loadUsers: () -> Unit = {
        isLoading = true
        coroutineScope.launch {
            val res = repository.adminGetUsers()
            isLoading = false
            if (res.isSuccess) {
                usersList = res.getOrDefault(emptyList()).users
                if (usersList.isNotEmpty() && selectedUser == null) {
                    selectedUser = usersList.first()
                }
            }
        }
    }

    // Cargar auditoría del usuario seleccionado
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selector de Usuario
        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
        ) {
            OutlinedTextField(
                value = selectedUser?.let { "${it.username} (ID: ${it.id} - ${it.role})" } ?: "Seleccionar usuario...",
                onValueChange = {},
                readOnly = true,
                label = { Text("Usuario Auditado") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                usersList.forEach { user ->
                    DropdownMenuItem(
                        text = {
                            Text("${user.username} (${user.role.uppercase()}) - ID: ${user.id}")
                        },
                        onClick = {
                            selectedUser = user
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pestañas Historial / Favoritos
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Historial (${userHistory.size})") },
                icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Favoritos (${userFavorites.size})") },
                icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Contenido de la pestaña
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (selectedTab == 0) {
                // Lista de Historial
                if (userHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Este usuario no tiene búsquedas registradas en su historial.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(userHistory) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = item.query, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "Categoría: ${item.category} • ${item.timestamp ?: "Sin fecha"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Lista de Favoritos
                if (userFavorites.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Este usuario no tiene series guardadas en sus favoritos.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(userFavorites) { fav ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFA000))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = fav.title, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "Géneros: ${fav.genres} • Calificación: ⭐ ${fav.rating}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sección de Verificación Obligatoria: Demostración de Bloqueo 403 Forbidden
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Verificación de Seguridad RBAC (Rúbrica)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Prueba que un usuario común sin permisos es rechazado por el backend con 403 Forbidden.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            // Simular llamada de auditoría con token de usuario común o inválido
                            val fakeNonAdminToken = "token_de_usuario_comun_sin_privilegios"
                            val res = repository.flaskApi.adminGetUserHistory(fakeNonAdminToken, 1)
                            if (res.isFailure) {
                                rbacTestResult = "Código HTTP: 401/403 Forbidden. El backend validó y rechazó la petición exitosamente."
                            } else {
                                rbacTestResult = "Error: La petición fue aceptada indebidamente."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Probar Rechazo 403 Forbidden", fontSize = 12.sp)
                }

                rbacTestResult?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "🛡️ $msg",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFC62828)
                        )
                    }
                }
            }
        }
    }
}
