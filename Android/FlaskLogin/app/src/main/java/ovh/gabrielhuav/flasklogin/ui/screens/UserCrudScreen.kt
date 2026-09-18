package ovh.gabrielhuav.flasklogin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import kotlinx.coroutines.launch
import org.json.JSONObject
import ovh.gabrielhuav.flasklogin.data.api.RetrofitClient
import ovh.gabrielhuav.flasklogin.data.model.AdminCreateUserRequest
import ovh.gabrielhuav.flasklogin.data.model.AdminUpdateUserRequest
import ovh.gabrielhuav.flasklogin.data.model.UserDto
import ovh.gabrielhuav.flasklogin.data.session.SessionManager

@Composable
fun UserCrudScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val apiService = remember { RetrofitClient.getApiService(context) }

    val usersList = remember { mutableStateListOf<UserDto>() }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Diálogos de CRUD de Usuarios
    var showCreateDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<UserDto?>(null) }
    var userToDelete by remember { mutableStateOf<UserDto?>(null) }

    val currentUserId = sessionManager.getUserId()

    val loadUsers: () -> Unit = {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val response = apiService.getUsers()
                isLoading = false
                if (response.isSuccessful) {
                    val body = response.body()
                    usersList.clear()
                    if (body != null) {
                        usersList.addAll(body.users)
                    }
                } else {
                    val errorJson = response.errorBody()?.string()
                    val msg = try {
                        JSONObject(errorJson ?: "").optString("message", "Error al listar usuarios (${response.code()})")
                    } catch (e: Exception) {
                        "Error del servidor: ${response.code()}"
                    }
                    errorMessage = msg
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Error de conexión: ${e.localizedMessage}"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadUsers()
    }

    val handleCreateUser: (String, String, String) -> Unit = { username, password, role ->
        showCreateDialog = false
        coroutineScope.launch {
            try {
                val response = apiService.createUser(AdminCreateUserRequest(username, password, role))
                if (response.isSuccessful) {
                    Toast.makeText(context, "Usuario '$username' creado", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    val errorJson = response.errorBody()?.string()
                    val msg = try { JSONObject(errorJson ?: "").optString("message", "Error") } catch (e: Exception) { "Error" }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val handleUpdateUser: (Int, String?, String?, String?) -> Unit = { id, username, password, role ->
        userToEdit = null
        coroutineScope.launch {
            try {
                val response = apiService.updateUser(id, AdminUpdateUserRequest(username, password, role))
                if (response.isSuccessful) {
                    Toast.makeText(context, "Usuario actualizado", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    val errorJson = response.errorBody()?.string()
                    val msg = try { JSONObject(errorJson ?: "").optString("message", "Error") } catch (e: Exception) { "Error" }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val handleDeleteUser: (Int) -> Unit = { id ->
        userToDelete = null
        coroutineScope.launch {
            try {
                val response = apiService.deleteUser(id)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Usuario eliminado", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    val errorJson = response.errorBody()?.string()
                    val msg = try { JSONObject(errorJson ?: "").optString("message", "Error al eliminar") } catch (e: Exception) { "Error" }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Usuario")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Gestión de Usuarios",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Panel de Administrador (CRUD)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = loadUsers) {
                    Icon(Icons.Default.Refresh, contentDescription = "Recargar usuarios")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(onClick = loadUsers) { Text("Reintentar") }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(usersList, key = { it.id }) { user ->
                        val isSelf = user.id == currentUserId
                        val isAdminUser = user.role == "admin"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAdminUser) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isAdminUser) Icons.Default.Shield else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isAdminUser) Color(0xFF512DA8) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.username,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (isSelf) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(Tú)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    text = if (isAdminUser) "ADMIN" else "USER",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = if (isAdminUser) Color(0xFFEDE7F6) else Color(0xFFE0F2F1),
                                                labelColor = if (isAdminUser) Color(0xFF512DA8) else Color(0xFF00796B)
                                            )
                                        )

                                        if (user.createdAt != null) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = user.createdAt,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }

                                Row {
                                    IconButton(onClick = { userToEdit = user }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar Usuario",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (!isSelf) {
                                        IconButton(onClick = { userToDelete = user }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar Usuario",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo Crear Usuario (Admin)
    if (showCreateDialog) {
        var createUsername by remember { mutableStateOf("") }
        var createPassword by remember { mutableStateOf("") }
        var createRole by remember { mutableStateOf("user") }
        var dialogError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nuevo Usuario") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = createUsername,
                        onValueChange = { createUsername = it; dialogError = null },
                        label = { Text("Nombre de Usuario *") },
                        isError = dialogError != null,
                        supportingText = dialogError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = createPassword,
                        onValueChange = { createPassword = it },
                        label = { Text("Contraseña inicial *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Rol a asignar:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = createRole == "user",
                            onClick = { createRole = "user" },
                            label = { Text("Usuario Estándar") }
                        )
                        FilterChip(
                            selected = createRole == "admin",
                            onClick = { createRole = "admin" },
                            label = { Text("Administrador") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (createUsername.trim().length < 3) {
                            dialogError = "El usuario debe tener al menos 3 caracteres"
                        } else if (createPassword.length < 4) {
                            dialogError = "La contraseña debe tener al menos 4 caracteres"
                        } else {
                            handleCreateUser(createUsername.trim(), createPassword, createRole)
                        }
                    }
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo Editar Usuario (Admin)
    userToEdit?.let { targetUser ->
        var editUsername by remember { mutableStateOf(targetUser.username) }
        var editPassword by remember { mutableStateOf("") }
        var editRole by remember { mutableStateOf(targetUser.role) }
        var editError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { userToEdit = null },
            title = { Text("Modificar Usuario #${targetUser.id}") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it; editError = null },
                        label = { Text("Nombre de Usuario") },
                        isError = editError != null,
                        supportingText = editError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editPassword,
                        onValueChange = { editPassword = it },
                        label = { Text("Nueva Contraseña (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Rol:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = editRole == "user",
                            onClick = { editRole = "user" },
                            label = { Text("Usuario Estándar") }
                        )
                        FilterChip(
                            selected = editRole == "admin",
                            onClick = { editRole = "admin" },
                            label = { Text("Administrador") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editUsername.trim().length < 3) {
                            editError = "El usuario debe tener al menos 3 caracteres"
                        } else {
                            handleUpdateUser(
                                targetUser.id,
                                if (editUsername.trim() != targetUser.username) editUsername.trim() else null,
                                if (editPassword.isNotEmpty()) editPassword else null,
                                if (editRole != targetUser.role) editRole else null
                            )
                        }
                    }
                ) {
                    Text("Guardar Cambios")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToEdit = null }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo Eliminar Usuario
    userToDelete?.let { targetUser ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Confirmar Eliminación") },
            text = {
                Text("¿Estás seguro de que deseas eliminar permanentemente al usuario '${targetUser.username}'? Esta acción borrará todos sus datos.")
            },
            confirmButton = {
                Button(onClick = { handleDeleteUser(targetUser.id) }) { Text("Eliminar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}
