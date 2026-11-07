package ovh.gabrielhuav.flasklogin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovh.gabrielhuav.flasklogin.data.session.SessionManager
import ovh.gabrielhuav.flasklogin.shared.model.UserData
import ovh.gabrielhuav.flasklogin.shared.repository.AppRepository
import ovh.gabrielhuav.flasklogin.ui.components.AppDestination
import ovh.gabrielhuav.flasklogin.ui.components.AppDrawerContent
import ovh.gabrielhuav.flasklogin.ui.components.SyncBanner
import ovh.gabrielhuav.flasklogin.ui.screens.AdminAuditScreen
import ovh.gabrielhuav.flasklogin.ui.screens.FavoritesScreen
import ovh.gabrielhuav.flasklogin.ui.screens.LoginScreen
import ovh.gabrielhuav.flasklogin.ui.screens.ProfileScreen
import ovh.gabrielhuav.flasklogin.ui.screens.RecommendationsScreen
import ovh.gabrielhuav.flasklogin.ui.screens.RegisterScreen
import ovh.gabrielhuav.flasklogin.ui.screens.TaskCrudScreen
import ovh.gabrielhuav.flasklogin.ui.screens.TvSearchScreen
import ovh.gabrielhuav.flasklogin.ui.screens.UserCrudScreen
import ovh.gabrielhuav.flasklogin.ui.theme.FlaskLoginTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlaskLoginTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    val isEmulator = remember {
        android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk" == android.os.Build.PRODUCT
    }
    val defaultBaseUrl = if (isEmulator) "http://10.0.2.2:5000" else "http://127.0.0.1:5000"

    val appRepository = remember {
        AppRepository().apply {
            flaskApi.baseUrl = defaultBaseUrl
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Estados reactivos de sesión y rol
    var isLoggedIn by remember { mutableStateOf(sessionManager.isLoggedIn()) }
    var currentUsername by remember { mutableStateOf(sessionManager.getUsername()) }
    var isAdmin by remember { mutableStateOf(sessionManager.isAdmin()) }

    // Sincronizar repositorio KMP con la sesión persistida
    LaunchedEffect(isLoggedIn) {
        val token = sessionManager.getToken()
        if (token != null && isLoggedIn) {
            appRepository.setSession(
                token = token,
                user = UserData(
                    id = sessionManager.getUserId(),
                    username = sessionManager.getUsername() ?: "Usuario",
                    role = sessionManager.getRole() ?: "user"
                )
            )
        }
    }

    // Destino inicial dinámico
    var currentDestination by remember {
        mutableStateOf(
            if (isLoggedIn) AppDestination.TV_SEARCH else AppDestination.LOGIN
        )
    }

    // Estados de Conectividad y Sincronización
    val isOnline by appRepository.isOnlineFlow.collectAsState()
    var isSyncing by remember { mutableStateOf(false) }
    var pendingCount by remember { mutableStateOf(appRepository.cache.pendingOfflineActionsCount()) }
    var lastSync by remember { mutableStateOf(appRepository.cache.lastSyncTimestamp) }

    // El banner superior sólo aparece al entrar a la app y se oculta automáticamente a los 5 segundos
    var showSyncBanner by remember { mutableStateOf(true) }
    var showNetworkStatusDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showSyncBanner) {
        if (showSyncBanner) {
            delay(5000L)
            showSyncBanner = false
        }
    }

    LaunchedEffect(isOnline, isSyncing, currentDestination) {
        pendingCount = appRepository.cache.pendingOfflineActionsCount()
        lastSync = appRepository.cache.lastSyncTimestamp
    }

    val closeDrawerAndNavigate: (AppDestination) -> Unit = { destination ->
        currentDestination = destination
        coroutineScope.launch {
            drawerState.close()
        }
    }

    val performLogout: () -> Unit = {
        sessionManager.clearSession()
        appRepository.clearSession()
        isLoggedIn = false
        currentUsername = null
        isAdmin = false
        currentDestination = AppDestination.LOGIN
        Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
        coroutineScope.launch {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                currentDestination = currentDestination,
                isLoggedIn = isLoggedIn,
                currentUsername = currentUsername,
                isAdmin = isAdmin,
                onNavigate = { destination ->
                    closeDrawerAndNavigate(destination)
                },
                onLogout = performLogout,
                onOpenNetworkStatus = {
                    coroutineScope.launch {
                        drawerState.close()
                    }
                    showNetworkStatusDialog = true
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentDestination.title,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Abrir Menú de Navegación"
                            )
                        }
                    },
                    actions = {
                        // Icono permanente para inspeccionar el estado de red y sincronización en cualquier momento
                        if (isLoggedIn && currentDestination != AppDestination.LOGIN && currentDestination != AppDestination.REGISTER) {
                            IconButton(onClick = { showNetworkStatusDialog = true }) {
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = "Estado de Red y Sincronización",
                                    tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Banner interactivo de Sincronización: visible solo los primeros 5 segundos al entrar o tras interactuar
                AnimatedVisibility(
                    visible = showSyncBanner && isLoggedIn && currentDestination != AppDestination.LOGIN && currentDestination != AppDestination.REGISTER,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SyncBanner(
                        isOnline = isOnline,
                        lastSyncTimestamp = lastSync,
                        pendingActionsCount = pendingCount,
                        isSyncing = isSyncing,
                        onSyncClick = {
                            isSyncing = true
                            coroutineScope.launch {
                                val res = appRepository.syncPendingOfflineActions()
                                pendingCount = appRepository.cache.pendingOfflineActionsCount()
                                lastSync = appRepository.cache.lastSyncTimestamp
                                isSyncing = false
                                val count = res.getOrDefault(0)
                                val msg = if (res.isSuccess) {
                                    if (count > 0) "Sincronizados $count cambios pendientes" else "Todo al día"
                                } else {
                                    "Error al sincronizar"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onToggleConnectivity = {
                            val newOnline = !isOnline
                            appRepository.setNetworkConnectivity(newOnline)
                            pendingCount = appRepository.cache.pendingOfflineActionsCount()
                            showSyncBanner = true
                            val msg = if (newOnline) "🟢 Conectado" else "🟠 Modo Offline simulado"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentDestination) {
                        AppDestination.LOGIN -> {
                            LoginScreen(
                                repository = appRepository,
                                onLoginSuccess = { username, role ->
                                    val token = appRepository.currentToken ?: "session_active"
                                    sessionManager.saveAuthSession(
                                        token = token,
                                        userId = appRepository.currentUser?.id ?: 1,
                                        username = username,
                                        role = role
                                    )
                                    isLoggedIn = true
                                    currentUsername = username
                                    isAdmin = role.equals("admin", ignoreCase = true)
                                    showSyncBanner = true // Mostrar el banner 5 segundos al entrar
                                    currentDestination = AppDestination.TV_SEARCH
                                },
                                onNavigateToRegister = {
                                    currentDestination = AppDestination.REGISTER
                                }
                            )
                        }

                        AppDestination.REGISTER -> {
                            RegisterScreen(
                                repository = appRepository,
                                onNavigateToLogin = {
                                    currentDestination = AppDestination.LOGIN
                                }
                            )
                        }

                        AppDestination.TV_SEARCH -> {
                            TvSearchScreen(
                                repository = appRepository
                            )
                        }

                        AppDestination.FAVORITES -> {
                            FavoritesScreen(
                                repository = appRepository,
                                onNavigateToSearch = {
                                    currentDestination = AppDestination.TV_SEARCH
                                }
                            )
                        }

                        AppDestination.RECOMMENDATIONS -> {
                            RecommendationsScreen(
                                repository = appRepository
                            )
                        }

                        AppDestination.TASKS -> {
                            TaskCrudScreen(
                                repository = appRepository,
                                onNavigateToLogin = {
                                    currentDestination = AppDestination.LOGIN
                                }
                            )
                        }

                        AppDestination.PROFILE -> {
                            ProfileScreen(
                                onProfileUpdated = { updatedUsername ->
                                    currentUsername = updatedUsername
                                }
                            )
                        }

                        AppDestination.USER_MANAGEMENT -> {
                            if (isAdmin) {
                                UserCrudScreen()
                            } else {
                                ProfileScreen(
                                    onProfileUpdated = { updatedUsername ->
                                        currentUsername = updatedUsername
                                    }
                                )
                            }
                        }

                        AppDestination.ADMIN_AUDIT -> {
                            if (isAdmin) {
                                AdminAuditScreen(
                                    repository = appRepository
                                )
                            } else {
                                ProfileScreen(
                                    onProfileUpdated = { updatedUsername ->
                                        currentUsername = updatedUsername
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dedicado para verificar estado de red y sincronización en cualquier momento sin estorbar
    if (showNetworkStatusDialog) {
        AlertDialog(
            onDismissRequest = { showNetworkStatusDialog = false },
            icon = {
                Icon(
                    imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Conectividad y Sincronización",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isOnline) "🟢 Conectado al backend en tiempo real" else "🟠 Modo Offline activo (Consultas contra memoria local)",
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOnline) Color(0xFF1B5E20) else Color(0xFFBF360C),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Última sincronización: $lastSync",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Acciones en cola pendiente: $pendingCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val newOnline = !isOnline
                                appRepository.setNetworkConnectivity(newOnline)
                                pendingCount = appRepository.cache.pendingOfflineActionsCount()
                                showSyncBanner = true
                                val msg = if (newOnline) "🟢 Conectado" else "🟠 Modo Offline simulado"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isOnline) "Simular Avión" else "Simular Wi-Fi", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                isSyncing = true
                                showSyncBanner = true
                                coroutineScope.launch {
                                    val res = appRepository.syncPendingOfflineActions()
                                    pendingCount = appRepository.cache.pendingOfflineActionsCount()
                                    lastSync = appRepository.cache.lastSyncTimestamp
                                    isSyncing = false
                                    val count = res.getOrDefault(0)
                                    val msg = if (res.isSuccess) {
                                        if (count > 0) "Sincronizados $count cambios pendientes" else "Todo al día"
                                    } else {
                                        "Error al sincronizar"
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sincronizar", fontSize = 11.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showSyncBanner = true
                            showNetworkStatusDialog = false
                            Toast.makeText(context, "Mostrando barra superior durante 5 segundos", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Mostrar barra superior (5 seg)", fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNetworkStatusDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
