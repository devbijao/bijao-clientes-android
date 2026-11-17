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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.Compra
import app.bijao.clientes.core.models.ConsumoApp
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.pdf.formatearFecha
import app.bijao.clientes.core.pdf.parsearISO
import app.bijao.clientes.ui.explorar.EstadoVacio
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

private const val POR_PAGINA = 50

/** Compras de la persona en todos sus negocios, más los consumos de su
 * suscripción (un corte de un plan de 4, p. ej.), en una sola línea de tiempo
 * -- `GET /api/app/historial` + `GET /api/app/consumos`, mezclados con
 * `mezclarLineaDeTiempo`. Equivalente de `HistorialView.swift`. Se entra
 * desde Perfil, no es un tab propio: consulta ocasional, no algo que se
 * revise a diario.
 *
 * Un consumo cubierto al 100% por el plan no genera venta -- si falla su
 * carga, el historial de compras se sigue mostrando igual. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(onAtras: () -> Unit, onCompraClick: (String) -> Unit, onConsumoClick: (String) -> Unit) {
    var compras by remember { mutableStateOf<List<Compra>>(emptyList()) }
    var totalCompras by remember { mutableStateOf(0) }
    var consumos by remember { mutableStateOf<List<ConsumoApp>>(emptyList()) }
    var totalConsumos by remember { mutableStateOf(0) }
    var cargando by remember { mutableStateOf(false) }
    var cargandoMas by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun cargar() {
        cargando = true
        error = null
        try {
            val historial = ApiClient.service.historial(limite = POR_PAGINA).datoOLanzar()
            compras = historial.data
            totalCompras = historial.total
        } catch (e: Exception) {
            error = "Inténtalo de nuevo en un momento."
        }
        try {
            val respuesta = ApiClient.service.consumos(limite = POR_PAGINA).datoOLanzar()
            consumos = respuesta.data
            totalConsumos = respuesta.total
        } catch (e: Exception) {
            // Los consumos son un añadido: si fallan, el historial de compras se muestra igual.
        }
        cargando = false
    }
    LaunchedEffect(Unit) { cargar() }

    val hayMasCompras = compras.size < totalCompras
    val hayMasConsumos = consumos.size < totalConsumos
    val movimientos = mezclarLineaDeTiempo(compras, consumos, hayMasCompras, hayMasConsumos)

    suspend fun verMas() {
        cargandoMas = true
        if (hayMasCompras) {
            try {
                val nuevas = ApiClient.service.historial(desde = compras.size, limite = POR_PAGINA).datoOLanzar()
                compras = compras + nuevas.data
            } catch (e: Exception) { /* se reintenta con el botón */ }
        }
        if (hayMasConsumos) {
            try {
                val nuevos = ApiClient.service.consumos(desde = consumos.size, limite = POR_PAGINA).datoOLanzar()
                consumos = consumos + nuevos.data
            } catch (e: Exception) { /* se reintenta con el botón */ }
        }
        cargandoMas = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial") },
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
                cargando && movimientos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null && movimientos.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.WifiOff,
                    titulo = "No pudimos cargar tu historial",
                    descripcion = error ?: "",
                )
                movimientos.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.Receipt,
                    titulo = "Sin compras todavía",
                    descripcion = "Tus compras en negocios Bijao aparecerán aquí.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(movimientos, key = { it.id }) { movimiento ->
                        when (movimiento) {
                            is Movimiento.DeCompra -> CompraRow(compra = movimiento.compra, onClick = { onCompraClick(movimiento.compra.id) })
                            is Movimiento.DeConsumo -> ConsumoRow(consumo = movimiento.consumo, onClick = { onConsumoClick(movimiento.consumo.id) })
                        }
                    }
                    if (hayMasCompras || hayMasConsumos) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                if (cargandoMas) {
                                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                                } else {
                                    TextButton(onClick = { scope.launch { verMas() } }) { Text("Ver más") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompraRow(compra: Compra, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(compra.negocio.nombre ?: "Negocio", style = MaterialTheme.typography.titleSmall)
                compra.fecha?.let { fecha ->
                    parsearISO(fecha)?.let {
                        Text(
                            formatearFecha(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).apply { maximumFractionDigits = 0 }.format(compra.total),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun ConsumoRow(consumo: ConsumoApp, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(consumo.negocio.nombre ?: "Negocio", style = MaterialTheme.typography.titleSmall)
                Text(
                    (if (consumo.cantidad > 1) "${formatoCantidadConsumo(consumo.cantidad)} × " else "") + (consumo.producto ?: "Servicio"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                consumo.fecha?.let { fecha ->
                    parsearISO(fecha)?.let {
                        Text(
                            formatearFecha(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                "Incluido\nen tu plan",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
    }
}

internal fun formatoCantidadConsumo(n: Double): String =
    if (n == n.toLong().toDouble()) n.toLong().toString() else n.toString()
