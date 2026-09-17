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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject
import ovh.gabrielhuav.flasklogin.data.api.RetrofitClient
import ovh.gabrielhuav.flasklogin.data.model.TaskItem
import ovh.gabrielhuav.flasklogin.data.model.TaskRequest
import ovh.gabrielhuav.flasklogin.data.session.SessionManager
import ovh.gabrielhuav.flasklogin.ui.components.CreateTaskDialog
import ovh.gabrielhuav.flasklogin.ui.components.DeleteTaskDialog
import ovh.gabrielhuav.flasklogin.ui.components.EditTaskDialog

@Composable
fun TaskCrudScreen(
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val apiService = remember { RetrofitClient.getApiService(context) }

    val tasksList = remember { mutableStateListOf<TaskItem>() }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Estados para diálogos modales de CRUD
    var showCreateDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskItem?>(null) }

    val isLoggedIn = sessionManager.isLoggedIn()
    val currentUsername = sessionManager.getUsername() ?: "Usuario"

    // Función para Cargar/Leer Tareas (READ)
    val loadTasks: () -> Unit = {
        if (isLoggedIn) {
            isLoading = true
            errorMessage = null
            coroutineScope.launch {
                try {
                    val response = apiService.getTasks()
                    isLoading = false
                    if (response.isSuccessful) {
                        val body = response.body()
                        tasksList.clear()
                        if (body != null) {
                            tasksList.addAll(body.tasks)
                        }
                    } else {
                        val errorJson = response.errorBody()?.string()
                        val msg = try {
                            JSONObject(errorJson ?: "").optString("message", "Error al obtener tareas (${response.code()})")
                        } catch (e: Exception) {
                            "Error del servidor: Código ${response.code()}"
                        }
                        errorMessage = msg
                    }
                } catch (e: Exception) {
                    isLoading = false
                    errorMessage = "Error de conexión: ${e.localizedMessage}"
                }
            }
        }
    }

    // Cargar al entrar a la pantalla
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            loadTasks()
        }
    }

    // Función para Crear Tarea (CREATE)
    val handleCreateTask: (String, String, String) -> Unit = { title, desc, status ->
        showCreateDialog = false
        coroutineScope.launch {
            try {
                val response = apiService.createTask(TaskRequest(title, desc, status))
                if (response.isSuccessful) {
                    Toast.makeText(context, "Tarea creada exitosamente", Toast.LENGTH_SHORT).show()
                    loadTasks()
                } else {
                    Toast.makeText(context, "Error al crear tarea: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Función para Actualizar Tarea (UPDATE)
    val handleUpdateTask: (Int, String, String, String) -> Unit = { id, title, desc, status ->
        taskToEdit = null
        coroutineScope.launch {
            try {
                val response = apiService.updateTask(id, TaskRequest(title, desc, status))
                if (response.isSuccessful) {
                    Toast.makeText(context, "Tarea actualizada", Toast.LENGTH_SHORT).show()
                    loadTasks()
                } else {
                    Toast.makeText(context, "Error al actualizar: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Función para Alternar Estado Rápido (Completada/Pendiente)
    val toggleTaskStatus: (TaskItem) -> Unit = { task ->
        val newStatus = if (task.status == "completed") "pending" else "completed"
        coroutineScope.launch {
            try {
                val response = apiService.updateTask(task.id, TaskRequest(task.title, task.description, newStatus))
                if (response.isSuccessful) {
                    loadTasks()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cambiar estado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función para Eliminar Tarea (DELETE)
    val handleDeleteTask: (Int) -> Unit = { id ->
        taskToDelete = null
        coroutineScope.launch {
            try {
                val response = apiService.deleteTask(id)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Tarea eliminada exitosamente", Toast.LENGTH_SHORT).show()
                    loadTasks()
                } else {
                    Toast.makeText(context, "Error al eliminar: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Si el usuario NO está autenticado
    if (!isLoggedIn) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sesión Requerida",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Para realizar operaciones CRUD (Crear, Leer, Actualizar y Eliminar tareas), debes iniciar sesión en el backend REST.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ir a Iniciar Sesión")
                    }
                }
            }
        }
        return
    }

    // Si el usuario SÍ está autenticado
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Tarea")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Barra superior de la pantalla CRUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mis Tareas",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sesión activa: $currentUsername",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = loadTasks) {
                    Icon(Icons.Default.Refresh, contentDescription = "Recargar lista")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Estado de Error
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = loadTasks) {
                            Text("Reintentar conexión")
                        }
                    }
                }
            }

            // Estado de Carga
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Cargando tareas desde el backend REST...")
                    }
                }
            } else if (tasksList.isEmpty() && errorMessage == null) {
                // Estado Vacío
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No tienes tareas registradas.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pulsa el botón '+' para agregar tu primera tarea.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Crear Tarea")
                        }
                    }
                }
            } else {
                // Lista de Tareas (READ)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(tasksList, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleStatus = { toggleTaskStatus(task) },
                            onEdit = { taskToEdit = task },
                            onDelete = { taskToDelete = task }
                        )
                    }
                }
            }
        }
    }

    // Diálogo CREATE
    if (showCreateDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, desc, status ->
                handleCreateTask(title, desc, status)
            }
        )
    }

    // Diálogo EDIT / UPDATE
    taskToEdit?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { taskToEdit = null },
            onConfirm = { title, desc, status ->
                handleUpdateTask(task.id, title, desc, status)
            }
        )
    }

    // Diálogo DELETE
    taskToDelete?.let { task ->
        DeleteTaskDialog(
            task = task,
            onDismiss = { taskToDelete = null },
            onConfirm = { handleDeleteTask(task.id) }
        )
    }
}

@Composable
fun TaskCard(
    task: TaskItem,
    onToggleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = task.status == "completed"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono para alternar estado
            IconButton(onClick = onToggleStatus) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isCompleted) "Completada" else "Pendiente",
                    tint = if (isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Información de la Tarea
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = onToggleStatus,
                        label = {
                            Text(
                                text = if (isCompleted) "Completada" else "Pendiente",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isCompleted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            labelColor = if (isCompleted) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                    )

                    if (task.createdAt != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = task.createdAt,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Botones de acción: Editar y Eliminar
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar Tarea",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar Tarea",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
