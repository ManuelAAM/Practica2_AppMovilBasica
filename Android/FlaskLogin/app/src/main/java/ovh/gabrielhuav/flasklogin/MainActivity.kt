package ovh.gabrielhuav.flasklogin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import ovh.gabrielhuav.flasklogin.data.session.SessionManager
import ovh.gabrielhuav.flasklogin.ui.components.AppDestination
import ovh.gabrielhuav.flasklogin.ui.components.AppDrawerContent
import ovh.gabrielhuav.flasklogin.ui.screens.LoginScreen
import ovh.gabrielhuav.flasklogin.ui.screens.ProfileScreen
import ovh.gabrielhuav.flasklogin.ui.screens.RegisterScreen
import ovh.gabrielhuav.flasklogin.ui.screens.TaskCrudScreen
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Estados reactivos de sesión y rol
    var isLoggedIn by remember { mutableStateOf(sessionManager.isLoggedIn()) }
    var currentUsername by remember { mutableStateOf(sessionManager.getUsername()) }
    var isAdmin by remember { mutableStateOf(sessionManager.isAdmin()) }

    // Destino inicial dinámico
    var currentDestination by remember {
        mutableStateOf(
            when {
                !isLoggedIn -> AppDestination.LOGIN
                isAdmin -> AppDestination.USER_MANAGEMENT
                else -> AppDestination.PROFILE
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
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentDestination) {
                    AppDestination.LOGIN -> {
                        LoginScreen(
                            onLoginSuccess = { username, role ->
                                isLoggedIn = true
                                currentUsername = username
                                isAdmin = role.equals("admin", ignoreCase = true)
                                currentDestination = if (isAdmin) AppDestination.USER_MANAGEMENT else AppDestination.PROFILE
                            },
                            onNavigateToRegister = {
                                currentDestination = AppDestination.REGISTER
                            }
                        )
                    }

                    AppDestination.REGISTER -> {
                        RegisterScreen(
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

                    AppDestination.TASKS -> {
                        TaskCrudScreen(
                            onNavigateToLogin = {
                                currentDestination = AppDestination.LOGIN
                            }
                        )
                    }
                }
            }
        }
    }
}