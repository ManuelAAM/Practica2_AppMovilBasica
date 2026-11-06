package ovh.gabrielhuav.flasklogin.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Banner de Conectividad y Sincronización (Práctica 5 - Ejercicio 1.2).
 * Muestra en tiempo real:
 * - Estado de red: Online / Offline
 * - Estampa de última sincronización
 * - Contador de cambios pendientes en cola offline
 * - Botón interactivo para forzar sincronización y reintentos
 */
@Composable
fun SyncBanner(
    isOnline: Boolean,
    lastSyncTimestamp: String,
    pendingActionsCount: Int,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    onToggleConnectivity: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        color = if (isOnline) {
            Color(0xFFE8F5E9) // Verde suave
        } else {
            Color(0xFFFFF3E0) // Naranja suave
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.layout.Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isOnline) "🟢 Conectado" else "🟠 Modo Offline",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isOnline) Color(0xFF1B5E20) else Color(0xFFBF360C)
                        )
                        if (pendingActionsCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFD32F2F), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$pendingActionsCount en cola",
                                    color = Color.white,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = "Última sinc: $lastSyncTimestamp",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Botón interactivo para simular alternancia de conexión (útil para pruebas y capturas)
                TextButton(
                    onClick = onToggleConnectivity,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                ) {
                    Text(
                        text = if (isOnline) "Simular Avión" else "Simular Wi-Fi",
                        fontSize = 11.sp
                    )
                }

                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    IconButton(
                        onClick = onSyncClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sincronizar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
