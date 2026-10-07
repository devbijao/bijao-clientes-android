package app.bijao.clientes.ui.historial

import android.content.Intent
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.bijao.clientes.core.models.CompraDetalle
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.pdf.formatearFecha
import app.bijao.clientes.core.pdf.generarYGuardarFacturaPDF
import app.bijao.clientes.core.pdf.parsearISO
import app.bijao.clientes.ui.explorar.EstadoVacio
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Detalle de una compra propia -- `GET /api/app/historial/{id}`, equivalente
 * de `CompraDetalleView.swift`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompraDetalleScreen(compraId: String, onAtras: () -> Unit) {
    val context = LocalContext.current
    var detalle by remember { mutableStateOf<CompraDetalle?>(null) }
    var facturaPdf by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(compraId) {
        error = null
        try {
            val nuevoDetalle = ApiClient.service.compraDetalle(compraId).datoOLanzar().data
            detalle = nuevoDetalle
            facturaPdf = withContext(Dispatchers.IO) { generarYGuardarFacturaPDF(context, nuevoDetalle) }
        } catch (e: Exception) {
            error = e.message ?: "No pudimos cargar la compra."
        }
    }

    fun uriFactura(): android.net.Uri? =
        facturaPdf?.let { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compra") },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { padding ->
        when {
            detalle != null -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    val d = detalle!!
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Fila("Negocio", d.negocio ?: "—")
                            d.numeroFactura?.let { Fila("Factura", it) }
                            d.fecha?.let { fecha -> parsearISO(fecha)?.let { Fila("Fecha", formatearFecha(it)) } }
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Productos", style = MaterialTheme.typography.titleSmall)
                            detalle!!.items.forEach { item ->
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    Text(item.nombre ?: "Producto", modifier = Modifier.weight(1f))
                                    Text(
                                        "${"%.0f".format(item.cantidad)} × ${formatoCOP(item.precioUnitario)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    val d = detalle!!
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Fila("Subtotal", formatoCOP(d.subtotal))
                            if (d.descuento > 0) Fila("Descuento", "-${formatoCOP(d.descuento)}")
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Text(formatoCOP(d.total), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                if (facturaPdf != null) {
                    item {
                        Column {
                            OutlinedButton(
                                onClick = {
                                    uriFactura()?.let { uri ->
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text("Ver factura en PDF")
                            }
                            OutlinedButton(
                                onClick = {
                                    uriFactura()?.let { uri ->
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Descargar factura"))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            ) {
                                Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text("Descargar factura en PDF")
                            }
                        }
                    }
                }
            }
            error != null -> EstadoVacio(
                icono = Icons.Filled.WifiOff,
                titulo = "No pudimos cargar la compra",
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

private fun formatoCOP(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).apply { maximumFractionDigits = 0 }.format(valor)
