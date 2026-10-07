package app.bijao.clientes.ui.historial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.Compra
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.pdf.formatearFecha
import app.bijao.clientes.core.pdf.parsearISO
import app.bijao.clientes.ui.explorar.EstadoVacio
import java.text.NumberFormat
import java.util.Locale

/** Compras de la persona en todos sus negocios, una sola línea de tiempo --
 * equivalente de `HistorialView.swift`. Se entra desde Perfil, no es un tab
 * propio: consulta ocasional, no algo que se revise a diario. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(onAtras: () -> Unit, onCompraClick: (String) -> Unit) {
    var compras by remember { mutableStateOf<List<Compra>>(emptyList()) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        cargando = true
        error = null
        try {
            compras = ApiClient.service.historial().datoOLanzar().data
        } catch (e: Exception) {
            error = e.message ?: "No pudimos cargar tu historial."
        } finally {
            cargando = false
        }
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
                cargando && compras.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null && compras.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.WifiOff,
                    titulo = "No pudimos cargar tu historial",
                    descripcion = error ?: "",
                )
                compras.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.Receipt,
                    titulo = "Sin compras todavía",
                    descripcion = "Tus compras en negocios Bijao aparecerán aquí.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(compras, key = { it.id }) { compra ->
                        CompraRow(compra = compra, onClick = { onCompraClick(compra.id) })
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
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
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
