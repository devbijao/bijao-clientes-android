package app.bijao.clientes.ui.creditos

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.calculo.CalculadoraCuenta
import app.bijao.clientes.core.models.AbonoCreditoApp
import app.bijao.clientes.core.models.DetalleCreditoApp
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import kotlinx.coroutines.launch
import java.net.URLEncoder

/**
 * Detalle de un crédito: cuánto falta, cuándo vence, los pagos ya hechos y
 * cómo pagar. El pago es manual -- la persona paga por su banca y le avisa
 * al negocio ("Ya pagué"), que registra el abono. Espejo de
 * `creditos/[id]/page.tsx` y de `CreditoDetalleView.swift`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditoDetalleScreen(creditoId: String, onAtras: () -> Unit) {
    var credito by remember { mutableStateOf<DetalleCreditoApp?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    suspend fun cargar() {
        error = null
        try {
            credito = ApiClient.service.creditoDetalle(creditoId).datoOLanzar().data
        } catch (e: Exception) {
            error = "No pudimos cargar el crédito."
        }
    }
    LaunchedEffect(creditoId) { cargar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi crédito") },
                navigationIcon = { IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } },
            )
        },
    ) { padding ->
        val c = credito
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                error != null && c == null -> Column(
                    modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Text(error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { scope.launch { cargar() } }) { Text("Reintentar") }
                }
                c == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> Contenido(c, creditoId, context)
            }
        }
    }
}

@Composable
private fun Contenido(c: DetalleCreditoApp, creditoId: String, context: android.content.Context) {
    val deuda = c.montoTotal + c.montoInteres
    val avance = if (deuda > 0) (c.montoPagado / deuda).coerceIn(0.0, 1.0).toFloat() else 1f
    val pagado = c.estado == "PAGADO"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))), RoundedCornerShape(20.dp))
                    .padding(16.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(c.negocio.nombre ?: "Negocio", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
                    Text(
                        textoEstado(c.estado), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Text(if (pagado) "Total que pagaste" else "Te falta pagar", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
                Text(
                    CalculadoraCuenta.formatoCOP(if (pagado) deuda else c.saldo),
                    color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold,
                )
                LinearProgressIndicator(
                    progress = { avance }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = Color.White, trackColor = Color.White.copy(alpha = 0.25f),
                )
                Text(
                    if (pagado) "Crédito saldado" else "Has pagado ${CalculadoraCuenta.formatoCOP(c.montoPagado)} de ${CalculadoraCuenta.formatoCOP(deuda)}",
                    color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column {
                    FilaInfo("Vencimiento", textoVencimiento(c.base()))
                    if (c.fechaLimite != null && !pagado) FilaInfo("Fecha límite", fechaCorta(c.fechaLimite))
                    FilaInfo("Valor del crédito", CalculadoraCuenta.formatoCOP(c.montoTotal))
                    if (c.montoInteres > 0) FilaInfo("Intereses", CalculadoraCuenta.formatoCOP(c.montoInteres))
                    c.numeroFactura?.let { FilaInfo("Factura", it) }
                }
            }
        }

        if (!pagado) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Cómo pagar", style = MaterialTheme.typography.titleSmall)
                    if (c.comoPagar != null) {
                        BloquePago(c.comoPagar)
                    } else {
                        Text(
                            "${c.negocio.nombre ?: "El negocio"} no tiene datos de pago en la app. Pregúntale cómo prefiere recibir tu pago.",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "Paga por tu banca y toca «Ya pagué»: el negocio verifica y registra tu pago, y el saldo se actualiza aquí.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    YaPague(creditoId = creditoId, saldo = c.saldo, nombreNegocio = c.negocio.nombre ?: "El negocio")

                    c.contactoWhatsapp?.let { contacto ->
                        val texto = "Hola, ya realicé un pago de mi crédito" +
                            (c.numeroFactura?.let { " (factura $it)" } ?: "") + ". Te envío el comprobante."
                        val url = "https://wa.me/${contacto.indicativo}${contacto.numero}?text=${URLEncoder.encode(texto, "UTF-8")}"
                        OutlinedButton(
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = app.bijao.clientes.core.ui.ColorExito),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("Mandar el comprobante por WhatsApp")
                        }
                    }
                }
            }
        }

        item {
            Column {
                Text("Pagos realizados", style = MaterialTheme.typography.titleSmall)
                if (c.abonos.isEmpty()) {
                    Text("Todavía no se ha registrado ningún pago.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.padding(top = 8.dp)) {
                        Column {
                            c.abonos.forEach { a -> FilaAbono(a) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaInfo(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FilaAbono(a: AbonoCreditoApp) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
        Text(CalculadoraCuenta.formatoCOP(a.monto), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        val metodo = a.metodoPago?.let { " · ${it.take(1)}${it.drop(1).lowercase()}" } ?: ""
        Text(fechaCorta(a.fecha) + metodo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Puente para reusar `textoVencimiento(CreditoApp)` desde el detalle, que
 * trae los mismos campos pero en `DetalleCreditoApp`. */
private fun DetalleCreditoApp.base() = app.bijao.clientes.core.models.CreditoApp(
    id = id, negocio = negocio, numeroFactura = numeroFactura, montoTotal = montoTotal,
    montoInteres = montoInteres, montoPagado = montoPagado, saldo = saldo, fechaLimite = fechaLimite,
    diasParaVencer = diasParaVencer, estado = estado, creado = creado,
)
