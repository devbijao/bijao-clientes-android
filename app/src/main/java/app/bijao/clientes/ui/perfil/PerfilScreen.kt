package app.bijao.clientes.ui.perfil

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.auth.AuthManager
import app.bijao.clientes.core.seguridad.BloqueoApp
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.runtime.collectAsState
import androidx.fragment.app.FragmentActivity
import app.bijao.clientes.core.models.Perfil
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.ui.FotoPerfil
import app.bijao.clientes.core.ui.PortadaPerfil
import kotlinx.coroutines.launch

private const val PREFS = "bijao_clientes"
private const val CLAVE_NOTIFICACIONES = "notificaciones_activadas"
private val ALTO_PORTADA = 210.dp
private val TAMANO_FOTO = 116.dp

/**
 * Perfil al estilo WhatsApp (F2 de `MDs/plan_apps_nativas_previo_revision.md`):
 * portada arriba, la foto montada sobre ella, nombre, @ e «Info», y debajo las
 * funciones agrupadas. Se abre en modo vista: los cambios se hacen en «Editar»
 * (arriba a la derecha). Equivalente de `PerfilView.swift`.
 */
@Composable
fun PerfilScreen(
    perfil: Perfil,
    onEditar: () -> Unit,
    onVerHistorial: () -> Unit,
    onVerContactos: () -> Unit,
    onVerCreditos: () -> Unit,
    onVerComunidad: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    var miUsuario by remember { mutableStateOf(perfil.usuario) }
    LaunchedEffect(Unit) {
        try {
            miUsuario = ApiClient.service.miUsuario().datoOLanzar().usuario
        } catch (e: Exception) {
            // Sin @ la fila de Contactos simplemente no lo muestra.
        }
    }
    var notificacionesActivadas by remember { mutableStateOf(prefs.getBoolean(CLAVE_NOTIFICACIONES, true)) }
    var confirmandoBorrado by remember { mutableStateOf(false) }
    var borrando by remember { mutableStateOf(false) }
    var errorBorrado by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Cabecera: portada, foto, nombre, @ e info.
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomCenter) {
                    PortadaPerfil(perfil.portadaUrl, alto = ALTO_PORTADA, modifier = Modifier.padding(bottom = TAMANO_FOTO / 2))
                    FotoPerfil(perfil.avatarUrl, perfil.nombre, tamano = TAMANO_FOTO, aro = true)
                }
                Text(
                    perfil.nombre?.takeIf { it.isNotBlank() } ?: "Sin nombre",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp, start = 24.dp, end = 24.dp),
                )
                miUsuario?.let { Text("@$it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                val bio = perfil.bio?.takeIf { it.isNotBlank() }
                if (bio != null) {
                    Text(bio, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp))
                } else {
                    TextButton(onClick = onEditar) { Text("Añadir una info") }
                }
            }

            Grupo {
                Fila("Historial de compras", Icons.Filled.ShoppingBag, Color(0xFFFF9500), onClick = onVerHistorial)
                Separador()
                Fila("Mis créditos", Icons.Filled.CreditCard, Color(0xFF34C759), onClick = onVerCreditos)
                Separador()
                Fila("Comunidad", Icons.Filled.Groups, Color(0xFFAF52DE), onClick = onVerComunidad)
                Separador()
                Fila("Contactos", Icons.Filled.People, Color(0xFF007AFF), detalle = miUsuario?.let { "@$it" }, onClick = onVerContactos)
            }

            Column {
                Grupo {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconoFila(Icons.Filled.NotificationsActive, Color(0xFFFF3B30))
                        Text("Promociones y novedades", modifier = Modifier.weight(1f).padding(start = 14.dp))
                        Switch(
                            checked = notificacionesActivadas,
                            onCheckedChange = {
                                notificacionesActivadas = it
                                prefs.edit().putBoolean(CLAVE_NOTIFICACIONES, it).apply()
                            },
                        )
                    }
                }
                Text(
                    "El envío real de notificaciones llega con una fase siguiente.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp),
                )
            }

            if (BloqueoApp.disponible(context)) {
                val activado by BloqueoApp.activado.collectAsState()
                Column {
                    Grupo {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconoFila(Icons.Filled.Fingerprint, Color(0xFF34C759))
                            Text("Bloquear con huella o rostro", modifier = Modifier.weight(1f).padding(start = 14.dp))
                            Switch(checked = activado, onCheckedChange = { nuevo ->
                                val actividad = context as? FragmentActivity
                                if (nuevo && actividad != null) scope.launch { BloqueoApp.activar(actividad) }
                                else if (!nuevo) BloqueoApp.desactivar(context)
                            })
                        }
                    }
                    Text(
                        "Al abrir Bijao, o al volver después de un minuto, se pide tu huella, tu rostro o el bloqueo del teléfono. Tu sesión sigue abierta.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp),
                    )
                }
            }

            Grupo {
                Fila("Cerrar sesión", Icons.AutoMirrored.Filled.Logout, Color.Gray, chevron = false) {
                    scope.launch { AuthManager.salir() }
                }
                Separador()
                // Derecho de supresión: Google Play y la App Store exigen poder borrar
                // la cuenta desde la misma app.
                Fila("Eliminar mi cuenta", Icons.Filled.DeleteForever, Color(0xFFFF3B30), chevron = false, rojo = true) {
                    if (!borrando) confirmandoBorrado = true
                }
            }
            errorBorrado?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp))
            }
        }

        FilledTonalButton(
            onClick = onEditar,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp),
        ) { Text("Editar", fontWeight = FontWeight.SemiBold) }
    }

    if (confirmandoBorrado) {
        AlertDialog(
            onDismissRequest = { if (!borrando) confirmandoBorrado = false },
            title = { Text("¿Eliminar tu cuenta de Bijao Clientes?") },
            text = {
                Text(
                    "Se borra tu cuenta y el vínculo con los negocios donde compras. Tus " +
                        "compras y sellos siguen siendo registros de cada negocio, como una " +
                        "factura: eso no desaparece, pero deja de estar unido a ti aquí.",
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !borrando,
                    onClick = {
                        scope.launch {
                            borrando = true
                            errorBorrado = null
                            try {
                                ApiClient.service.eliminarCuenta().datoOLanzar()
                                confirmandoBorrado = false
                                AuthManager.salir()
                            } catch (e: Exception) {
                                errorBorrado = "No pudimos eliminar la cuenta. Inténtalo de nuevo en un momento."
                                confirmandoBorrado = false
                            } finally {
                                borrando = false
                            }
                        }
                    },
                ) {
                    if (borrando) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    else Text("Sí, eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(enabled = !borrando, onClick = { confirmandoBorrado = false }) { Text("Cancelar") }
            },
        )
    }
}

// ── Piezas de la lista (como los grupos de WhatsApp) ──────────────────────────

@Composable
private fun Grupo(contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surface),
        content = contenido,
    )
}

@Composable
private fun Separador() {
    HorizontalDivider(Modifier.padding(start = 62.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
}

@Composable
private fun IconoFila(icono: ImageVector, color: Color) {
    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(color), contentAlignment = Alignment.Center) {
        Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Fila(
    titulo: String,
    icono: ImageVector,
    color: Color,
    detalle: String? = null,
    chevron: Boolean = true,
    rojo: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconoFila(icono, color)
        Text(titulo, color = if (rojo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = 14.dp))
        detalle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
        if (chevron) {
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp).size(14.dp))
        }
    }
}
