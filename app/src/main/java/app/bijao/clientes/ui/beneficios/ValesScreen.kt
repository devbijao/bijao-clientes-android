package app.bijao.clientes.ui.beneficios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.Vale
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.ui.explorar.EstadoVacio

/** Historial de vales emitidos al canjear -- el código solo se veía una vez,
 * en el sheet de canje; esta es la bitácora. Equivalente de `ValesView.swift`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValesScreen(onAtras: () -> Unit) {
    var vales by remember { mutableStateOf<List<Vale>>(emptyList()) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        cargando = true
        error = null
        try {
            vales = ApiClient.service.vales().datoOLanzar().data
        } catch (e: Exception) {
            error = e.message ?: "No pudimos cargar tus vales."
        } finally {
            cargando = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis vales") },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                cargando && vales.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null && vales.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.WifiOff,
                    titulo = "No pudimos cargar tus vales",
                    descripcion = error ?: "",
                )
                vales.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.ConfirmationNumber,
                    titulo = "Sin vales todavía",
                    descripcion = "Cuando canjees una recompensa, aparece aquí.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(vales, key = { it.codigo }) { vale -> ValeRow(vale) }
                }
            }
        }
    }
}

@Composable
private fun ValeRow(vale: Vale) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(vale.recompensa ?: "Recompensa", style = MaterialTheme.typography.titleSmall)
            Text(
                vale.negocio ?: "Negocio",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                vale.codigo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val (texto, color) = estiloEstado(vale.estado)
        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(texto, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

private fun estiloEstado(estado: String?): Pair<String, Color> = when (estado) {
    "CANJEADO" -> "Canjeado" to Color(0xFF2E7D32)
    "VENCIDO" -> "Vencido" to Color(0xFFC62828)
    "PENDIENTE" -> "Pendiente" to Color(0xFFF57C00)
    else -> (estado ?: "—") to Color.Gray
}
