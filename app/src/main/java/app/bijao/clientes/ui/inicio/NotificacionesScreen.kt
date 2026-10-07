package app.bijao.clientes.ui.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.Notificacion
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.pdf.formatearFecha
import app.bijao.clientes.core.pdf.parsearISO
import app.bijao.clientes.ui.explorar.EstadoVacio

/** `GET /api/app/notificaciones` + `POST /api/app/notificaciones/leer` --
 * equivalente de `NotificacionesView.swift`. Al abrirse marca todo como
 * leído, no hay "marcar una". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(onCerrar: () -> Unit, onCompraClick: (String) -> Unit) {
    var notificaciones by remember { mutableStateOf<List<Notificacion>>(emptyList()) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        cargando = true
        error = null
        try {
            notificaciones = ApiClient.service.notificaciones(limite = 50).datoOLanzar().data
        } catch (e: Exception) {
            error = e.message ?: "No pudimos cargar tus novedades."
        } finally {
            cargando = false
        }
        try {
            ApiClient.service.marcarNotificacionesLeidas()
        } catch (e: Exception) {
            // No es grave si falla -- el badge se corrige en la próxima apertura.
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novedades") },
                actions = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                cargando && notificaciones.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null && notificaciones.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.WifiOff,
                    titulo = "No pudimos cargar tus novedades",
                    descripcion = error ?: "",
                )
                notificaciones.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.Notifications,
                    titulo = "Sin novedades",
                    descripcion = "Acá aparecen avisos de tus tarjetas y los negocios donde compras.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(notificaciones, key = { it.id }) { notificacion ->
                        FilaNotificacion(
                            notificacion = notificacion,
                            onClick = notificacion.compraId?.let { id -> { onCompraClick(id) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaNotificacion(notificacion: Notificacion, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (notificacion.leida) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.primaryContainer,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.NotificationsActive,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (notificacion.leida) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(notificacion.titulo ?: "Bijao", style = MaterialTheme.typography.titleSmall)
            notificacion.cuerpo?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            notificacion.createdAt?.let { fecha ->
                parsearISO(fecha)?.let {
                    Text(
                        formatearFecha(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (onClick != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        "Ver detalle",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp).padding(start = 2.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
