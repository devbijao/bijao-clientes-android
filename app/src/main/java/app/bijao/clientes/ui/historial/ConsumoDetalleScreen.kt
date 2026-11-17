package app.bijao.clientes.ui.historial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.DetalleConsumo
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.pdf.formatearFecha
import app.bijao.clientes.core.pdf.parsearISO
import app.bijao.clientes.ui.explorar.EstadoVacio
import java.time.format.DateTimeFormatter
import java.util.Locale

/** `GET /api/app/consumos/{id}` -- detalle de un consumo propio de
 * suscripción: qué servicio se usó del plan, quién atendió y cuánto queda.
 * No es una factura (el consumo está incluido en el plan, sin costo
 * adicional). Equivalente de `ConsumoDetalleView.swift`, espejo de
 * `historial/consumo/[id]/page.tsx` en la web. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumoDetalleScreen(consumoId: String, onAtras: () -> Unit) {
    var detalle by remember { mutableStateOf<DetalleConsumo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(consumoId) {
        error = null
        try {
            detalle = ApiClient.service.consumoDetalle(consumoId).datoOLanzar().data
        } catch (e: Exception) {
            error = "Inténtalo de nuevo en un momento."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consumo de suscripción") },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { padding ->
        when {
            detalle != null -> {
                val d = detalle!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Fila("Negocio", d.negocio ?: "—")
                                d.numero?.let { Fila("Comprobante", it) }
                                d.fecha?.let { fecha -> parsearISO(fecha)?.let { Fila("Fecha", formatearFecha(it)) } }
                            }
                        }
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Fila("Servicio", d.producto ?: "—")
                                Fila("Cantidad", formatoCantidadConsumo(d.cantidad))
                                d.plan?.let { Fila("Tu plan", it) }
                                d.atendidoPor?.let { Fila("Te atendió", it) }
                                d.numeroFactura?.let { Fila("Registrado en la factura", it) }
                            }
                        }
                    }
                    val incluido = d.saldo.incluido
                    val restante = d.saldo.restante
                    if (incluido != null && restante != null) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        formatoCantidadConsumo(restante),
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        "${if (restante == 1.0) "te queda" else "te quedan"} de ${formatoCantidadConsumo(incluido)} de ${d.producto ?: "este servicio"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                    )
                                    textoPeriodo(d)?.let {
                                        Text(
                                            "en el periodo $it",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            error != null -> EstadoVacio(
                icono = Icons.Filled.WifiOff,
                titulo = "No pudimos cargar el consumo",
                descripcion = error ?: "",
            )
            else -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun Fila(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(etiqueta, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(valor)
    }
}

/** `periodo_inicio`/`periodo_fin` llegan como "YYYY-MM-DD"; se arman como
 * fecha LOCAL para no correrse un día -- mismo gotcha que en la web y en
 * `ConsumoDetalleView.swift` (iOS). */
private fun textoPeriodo(d: DetalleConsumo): String? {
    val inicio = d.periodoInicio ?: return null
    val fin = d.periodoFin ?: return null
    val formateador = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-CO"))
    fun corta(iso: String): String? = try {
        java.time.LocalDate.parse(iso.take(10)).format(formateador)
    } catch (e: Exception) {
        null
    }
    val a = corta(inicio) ?: return null
    val b = corta(fin) ?: return null
    return "$a – $b"
}
