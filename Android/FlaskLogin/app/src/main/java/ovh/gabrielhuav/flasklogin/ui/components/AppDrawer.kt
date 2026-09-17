package ovh.gabrielhuav.flasklogin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.unit.dp

enum class AppDestination(val title: String) {
    LOGIN("Inicio de Sesión"),
    REGISTER("Registro de Usuario"),
    CRUD("Operaciones CRUD (Tareas)")
}

@Composable
fun AppDrawerContent(
    currentDestination: AppDestination,
    isLoggedIn: Boolean,
    currentUsername: String?,
    onNavigate: (AppDestination) -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet {
        // Cabecera del Drawer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(24.dp)
        ) {
            Column {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Avatar de Usuario",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Práctica 2 - Móviles",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (isLoggedIn) "Usuario: ${currentUsername ?: "Activo"}" else "Estado: No autenticado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Opción 1: Inicio de Sesión
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Lock, contentDescription = null) },
            label = { Text("Inicio de Sesión") },
            selected = currentDestination == AppDestination.LOGIN,
            onClick = { onNavigate(AppDestination.LOGIN) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        // Opción 2: Registro de Usuario
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
            label = { Text("Registro de Usuario") },
            selected = currentDestination == AppDestination.REGISTER,
            onClick = { onNavigate(AppDestination.REGISTER) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        // Opción 3: Operaciones CRUD
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) },
            label = { Text("Operaciones CRUD") },
            selected = currentDestination == AppDestination.CRUD,
            onClick = { onNavigate(AppDestination.CRUD) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Cerrar sesión si está autenticado
        if (isLoggedIn) {
            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                label = { Text("Cerrar Sesión") },
                selected = false,
                onClick = onLogout,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        // Pie de navegación con información de API
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Backend REST: 10.0.2.2:5000",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
