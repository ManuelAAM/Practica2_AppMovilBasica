package ovh.gabrielhuav.flasklogin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject
import ovh.gabrielhuav.flasklogin.data.api.RetrofitClient
import ovh.gabrielhuav.flasklogin.data.model.UpdateProfileRequest
import ovh.gabrielhuav.flasklogin.data.model.UserDto
import ovh.gabrielhuav.flasklogin.data.session.SessionManager

@Composable
fun ProfileScreen(
    onProfileUpdated: (newUsername: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val apiService = remember { RetrofitClient.getApiService(context) }

    var userProfile by remember { mutableStateOf<UserDto?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }

    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var isUpdating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val loadProfile: () -> Unit = {
        isLoadingProfile = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val response = apiService.getProfile()
                isLoadingProfile = false
                if (response.isSuccessful) {
                    val user = response.body()?.user
                    userProfile = user
                    if (user != null) {
                        newUsername = user.username
                    }
                } else {
                    errorMessage = "Error al cargar datos del perfil (${response.code()})"
                }
            } catch (e: Exception) {
                isLoadingProfile = false
                errorMessage = "Error de conexión: ${e.localizedMessage}"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadProfile()
    }

    val updateProfile: () -> Unit = {
        errorMessage = null
        successMessage = null

        when {
            newUsername.trim().length < 3 -> {
                errorMessage = "El nombre de usuario debe tener al menos 3 caracteres"
            }
            newPassword.isNotEmpty() && newPassword.length < 4 -> {
                errorMessage = "La nueva contraseña debe tener al menos 4 caracteres"
            }
            else -> {
                isUpdating = true
                coroutineScope.launch {
                    try {
                        val req = UpdateProfileRequest(
                            username = if (newUsername.trim() != userProfile?.username) newUsername.trim() else null,
                            password = if (newPassword.isNotEmpty()) newPassword else null
                        )
                        val response = apiService.updateProfile(req)
                        isUpdating = false
                        if (response.isSuccessful) {
                            val body = response.body()
                            if (body != null) {
                                userProfile = body.user
                                newPassword = ""
                                val updatedToken = body.token
                                if (!updatedToken.isNullOrEmpty()) {
                                    sessionManager.updateUsernameAndToken(updatedToken, body.user?.username ?: "")
                                }
                                successMessage = "¡Tus datos fueron actualizados correctamente!"
                                Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                                onProfileUpdated(body.user?.username ?: "")
                            }
                        } else {
                            val errorJson = response.errorBody()?.string()
                            val msg = try {
                                JSONObject(errorJson ?: "").optString("message", "Error al actualizar (${response.code()})")
                            } catch (e: Exception) {
                                "Error: Código ${response.code()}"
                            }
                            errorMessage = msg
                        }
                    } catch (e: Exception) {
                        isUpdating = false
                        errorMessage = "Error de conexión: ${e.localizedMessage}"
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        if (isLoadingProfile) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Mi Perfil de Usuario",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Aquí puedes consultar y modificar tu propia información",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Badge de Rol
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isAdmin = sessionManager.isAdmin()
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = if (isAdmin) "Rol: Administrador" else "Rol: Usuario Estándar",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = if (isAdmin) Icons.Default.Shield else Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isAdmin) Color(0xFFEDE7F6) else Color(0xFFE0F2F1),
                            labelColor = if (isAdmin) Color(0xFF512DA8) else Color(0xFF00796B)
                        )
                    )
                }

                if (userProfile?.createdAt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Registrado el: ${userProfile?.createdAt}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Mensajes de Feedback
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (successMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Text(
                            text = successMessage ?: "",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Campo Nombre de Usuario
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = {
                        newUsername = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Nombre de Usuario") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Campo Nueva Contraseña
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Nueva Contraseña (opcional)") },
                    supportingText = { Text("Déjalo vacío si no deseas cambiar tu contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = updateProfile,
                    enabled = !isUpdating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Guardar Cambios en Mi Perfil")
                    }
                }
            }
        }
    }
}
