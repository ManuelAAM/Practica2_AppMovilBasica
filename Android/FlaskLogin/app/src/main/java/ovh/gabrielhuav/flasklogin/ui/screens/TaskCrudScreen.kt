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
import androidx.compose.runtime.collectAsState
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
import ovh.gabrielhuav.flasklogin.shared.model.TaskItem
import ovh.gabrielhuav.flasklogin.shared.repository.AppRepository
import ovh.gabrielhuav.flasklogin.ui.components.CreateTaskDialog
import ovh.gabrielhuav.flasklogin.ui.components.DeleteTaskDialog
import ovh.gabrielhuav.flasklogin.ui.components.EditTaskDialog
import ovh.gabrielhuav.flasklogin.ui.components.SyncBanner

@Composable
fun TaskCrudScreen(
    repository: AppRepository,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val tasksList = remember { mutableStateListOf<TaskItem>() }
    var isLoading by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isOnline by repository.isOnlineFlow.collectAsState()
    val lastSyncTimestamp = repository.cache.lastSyncTimestamp
    val pendingActionsCount = repository.cache.pendingOfflineActionsCount()

    // Estados para diálogos modales de CRUD
    var showCreateDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskItem?>(null) }

    val currentUsername = repository.currentUser?.username ?: "Usuario"
    val isLoggedIn = repository.currentToken != null

    // Cargar tareas (READ)
    val loadTasks: () -> Unit = {
        if (isLoggedIn) {
            isLoading = true
            errorMessage = null
            coroutineScope.launch {
                val res = repository.getTasks(forceRefresh = true)
                isLoading = false
                if (res.isSuccess) {
                    tasksList.clear()
                    tasksList.addAll(res.getOrDefault(emptyList()))
                } else {
                    errorMessage = res.exceptionOrNull()?.message ?: "Error al obtener tareas"
                }
            }
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            loadTasks()
        }
    }

    // CREATE
    val handleCreateTask: (String, String, String) -> Unit = { title, desc, status ->
        showCreateDialog = false
        coroutineScope.launch {
            val res = repository.createTask(title, desc)
            if (res.isSuccess) {
                val created = res.getOrNull()
                if (created != null) {
                    tasksList.add(0, created)
                }
                Toast.makeText(context, "Tarea creada exitosamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Fallo al crear la tarea", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // UPDATE
    val handleUpdateTask: (Int, String, String, String) -> Unit = { id, title, desc, status ->
        taskToEdit = null
        coroutineScope.launch {
            val res = repository.updateTask(id, status, title, desc)
            if (res.isSuccess) {
                val index = tasksList.indexOfFirst { it.id == id }
                if (index != -1) {
                    tasksList[index] = tasksList[index].copy(
                        title = title,
                        description = desc,
                        status = status
                    )
                }
                Toast.makeText(context, "Tarea actualizada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // DELETE
    val handleDeleteTask: (Int) -> Unit = { id ->
        taskToDelete = null
        coroutineScope.launch {
            val res = repository.deleteTask(id)
            if (res.isSuccess) {
                tasksList.removeAll { it.id == id }
                Toast.makeText(context, "Tarea eliminada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // TOGGLE STATUS
    val toggleTaskStatus: (TaskItem) -> Unit = { task ->
        val newStatus = if (task.status == "completed") "pending" else "completed"
        coroutineScope.launch {
            val res = repository.updateTask(task.id, newStatus)
            if (res.isSuccess) {
                val index = tasksList.indexOfFirst { it.id == task.id }
                if (index != -1) {
                    tasksList[index] = tasksList[index].copy(status = newStatus)
                }
            }
        }
    }

    if (!isLoggedIn) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Sesión requerida",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Por favor inicia sesión para gestionar tus tareas.",
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateToLogin) {
                        Text("Iniciar Sesión")
                    }
                }
            }
        }
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Tarea")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Banner de Conectividad y Sincronización
            SyncBanner(
                isOnline = isOnline,
                lastSyncTimestamp = lastSyncTimestamp,
                pendingActionsCount = pendingActionsCount,
                isSyncing = isSyncing,
                onSyncClick = {
                    coroutineScope.launch {
                        isSyncing = true
                        val count = repository.syncPendingOfflineActions().getOrDefault(0)
                        isSyncing = false
                        Toast.makeText(context, "Sincronizado ($count operaciones resueltas)", Toast.LENGTH_SHORT).show()
                        loadTasks()
                    }
                },
                onToggleConnectivity = {
                    repository.setNetworkConnectivity(!isOnline)
                    Toast.makeText(
                        context,
                        if (!isOnline) "Modo Conectado activado" else "Modo Offline simulado (Modo Avión)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            // Encabezado de la pantalla
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mis Tareas (CRUD)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Usuario: $currentUsername • Total: ${tasksList.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = loadTasks) {
                    Icon(Icons.Default.Refresh, contentDescription = "Recargar")
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
                        Text("Sincronizando tareas...")
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
                        .weight(1f)
                        .padding(horizontal = 16.dp),
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
