package ovh.gabrielhuav.flasklogin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import ovh.gabrielhuav.flasklogin.ui.components.SyncBanner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import ovh.gabrielhuav.flasklogin.data.session.SessionManager
import ovh.gabrielhuav.flasklogin.shared.model.UserData
import ovh.gabrielhuav.flasklogin.shared.repository.AppRepository
import ovh.gabrielhuav.flasklogin.ui.components.AppDestination
import ovh.gabrielhuav.flasklogin.ui.components.AppDrawerContent
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
            when {
                !isLoggedIn -> AppDestination.LOGIN
                isAdmin -> AppDestination.TV_SEARCH
                else -> AppDestination.TV_SEARCH
            }
        )
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
                onLogout = performLogout
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
                // Banner interactivo de Sincronización y Modo Offline (Práctica 5)
                if (isLoggedIn && currentDestination != AppDestination.LOGIN && currentDestination != AppDestination.REGISTER) {
                    val isOnline by appRepository.isOnlineFlow.collectAsState()
                    var isSyncing by remember { mutableStateOf(false) }
                    var pendingCount by remember { mutableStateOf(appRepository.cache.pendingOfflineActionsCount()) }
                    var lastSync by remember { mutableStateOf(appRepository.cache.lastSyncTimestamp) }

                    LaunchedEffect(isOnline, isSyncing, currentDestination) {
                        pendingCount = appRepository.cache.pendingOfflineActionsCount()
                        lastSync = appRepository.cache.lastSyncTimestamp
                    }

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
}
