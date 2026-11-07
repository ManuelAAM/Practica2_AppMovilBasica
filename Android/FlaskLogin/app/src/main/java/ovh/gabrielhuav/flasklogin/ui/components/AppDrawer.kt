package ovh.gabrielhuav.flasklogin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AppDestination(val title: String) {
    LOGIN("Inicio de Sesión"),
    REGISTER("Registro de Usuario"),
    TASKS("Gestión de Tareas (CRUD P2)"),
    TV_SEARCH("Buscar Series (TVMaze API)"),
    FAVORITES("Mis Series Favoritas"),
    RECOMMENDATIONS("Recomendaciones"),
    PROFILE("Mi Perfil"),
    USER_MANAGEMENT("Gestión de Usuarios (Admin)"),
    ADMIN_AUDIT("Auditoría de Usuarios (Admin)")
}

@Composable
fun AppDrawerContent(
    currentDestination: AppDestination,
    isLoggedIn: Boolean,
    currentUsername: String?,
    isAdmin: Boolean,
    onNavigate: (AppDestination) -> Unit,
    onLogout: () -> Unit,
    onOpenNetworkStatus: () -> Unit = {}
) {
    ModalDrawerSheet {
        // Cabecera del Drawer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Avatar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Práctica 5 - KMP Móviles",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (isLoggedIn) {
                                "Usuario: ${currentUsername ?: "Activo"}"
                            } else {
                                "Estado: No autenticado"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }

                if (isLoggedIn) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (isAdmin) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isAdmin) "ROL: ADMINISTRADOR" else "ROL: USUARIO ESTÁNDAR",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isLoggedIn) {
            // Opción: Inicio de Sesión
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                label = { Text("Inicio de Sesión") },
                selected = currentDestination == AppDestination.LOGIN,
                onClick = { onNavigate(AppDestination.LOGIN) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            // Opción: Registro de Usuario
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                label = { Text("Registro de Usuario") },
                selected = currentDestination == AppDestination.REGISTER,
                onClick = { onNavigate(AppDestination.REGISTER) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        } else {
            // Sección Exploración y API Pública
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Tv, contentDescription = null) },
                label = { Text("Buscar Series (TVMaze)") },
                selected = currentDestination == AppDestination.TV_SEARCH,
                onClick = { onNavigate(AppDestination.TV_SEARCH) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Star, contentDescription = null) },
                label = { Text("Mis Favoritos") },
                selected = currentDestination == AppDestination.FAVORITES,
                onClick = { onNavigate(AppDestination.FAVORITES) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFB300)) },
                label = { Text("Recomendaciones") },
                selected = currentDestination == AppDestination.RECOMMENDATIONS,
                onClick = { onNavigate(AppDestination.RECOMMENDATIONS) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            // Opción: Tareas CRUD (P2)
            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null) },
                label = { Text("Tareas CRUD (Backend P2)") },
                selected = currentDestination == AppDestination.TASKS,
                onClick = { onNavigate(AppDestination.TASKS) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            // Opción: Estado de Red y Sincronización (Offline)
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                label = { Text("Estado de Red y Sync") },
                selected = false,
                onClick = onOpenNetworkStatus,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Mi Perfil
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                label = { Text("Mi Perfil") },
                selected = currentDestination == AppDestination.PROFILE,
                onClick = { onNavigate(AppDestination.PROFILE) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            // Menús de Administrador
            if (isAdmin) {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.SupervisorAccount, contentDescription = null) },
                    label = { Text("Gestión de Usuarios (Admin)") },
                    selected = currentDestination == AppDestination.USER_MANAGEMENT,
                    onClick = { onNavigate(AppDestination.USER_MANAGEMENT) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                    label = { Text("Auditoría Historial/Favs (Admin)") },
                    selected = currentDestination == AppDestination.ADMIN_AUDIT,
                    onClick = { onNavigate(AppDestination.ADMIN_AUDIT) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        if (isLoggedIn) {
            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                label = { Text("Cerrar Sesión") },
                selected = false,
                onClick = onLogout,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "KMP • Android & iOS • API TVMaze • Offline Cache",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
