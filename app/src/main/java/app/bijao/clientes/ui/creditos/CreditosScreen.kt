package app.bijao.clientes.ui.creditos

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.calculo.CalculadoraCuenta
import app.bijao.clientes.core.models.CreditoApp
import app.bijao.clientes.core.models.ListaCreditos
import app.bijao.clientes.core.models.ResumenCreditos
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.ui.ColorExito
import app.bijao.clientes.ui.explorar.EstadoVacio
import kotlinx.coroutines.launch

/**
 * "Mis créditos" (F7) -- lo que la persona debe en los negocios donde
 * compró a crédito. Espejo de `frontend/app/(app-clientes)/creditos/page.tsx`
 * y de `CreditosView.swift`. Solo lectura: pagar o registrar un abono sigue
 * siendo del negocio. Si la cuenta no tiene el celular verificado el
 * servidor no entrega nada (y no dice si hay o no deuda), así que lo único
 * que se puede hacer es invitar a verificarlo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditosScreen(onAtras: () -> Unit, onCreditoClick: (String) -> Unit) {
    var pestana by remember { mutableStateOf("PENDIENTE") }
    var lista by remember { mutableStateOf<ListaCreditos?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    suspend fun cargar() {
        cargando = lista == null
        error = null
        try {
            lista = ApiClient.service.creditos(pestana).datoOLanzar()
        } catch (e: Exception) {
            error = "No pudimos cargar tus créditos."
        }
        cargando = false
    }
    LaunchedEffect(pestana) { cargar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis créditos") },
                navigationIcon = { IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val l = lista
            when {
                cargando && l == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null && l == null -> EstadoVacio(icono = Icons.Filled.WifiOff, titulo = "No pudimos cargar tus créditos", descripcion = error ?: "")
                l != null && !l.celularVerificado -> SinVerificar()
                l != null -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf("PENDIENTE" to "Activos", "PAGADO" to "Pagados").forEachIndexed { i, (valor, etiqueta) ->
                                SegmentedButton(selected = pestana == valor, onClick = { pestana = valor }, shape = SegmentedButtonDefaults.itemShape(i, 2)) {
                                    Text(etiqueta)
                                }
                            }
                        }
                    }

                    if (pestana == "PENDIENTE" && l.data.isNotEmpty()) {
                        item { ResumenCard(l.resumen) }
                    }

                    if (l.data.isEmpty()) {
                        item {
                            EstadoVacio(
                                icono = Icons.Filled.PanTool,
                                titulo = if (pestana == "PENDIENTE") "No tienes créditos pendientes" else "Aún no has terminado de pagar ningún crédito",
                                descripcion = if (pestana == "PENDIENTE") "Cuando un negocio te fíe una compra, la verás aquí."
                                              else "Los créditos que termines de pagar quedarán aquí.",
                            )
                        }
                    } else {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column {
                                    l.data.forEach { c -> FilaCredito(c, onClick = { onCreditoClick(c.id) }) }
                                }
                            }
                        }
                    }

                    if (pestana == "PENDIENTE" && l.data.isNotEmpty()) {
                        item { PreferenciasCobro(modifier = Modifier.padding(top = 10.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SinVerificar() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
        Text("Verifica tu celular para ver tus créditos", style = MaterialTheme.typography.titleSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(
            "Por seguridad, solo mostramos tus deudas cuando confirmamos que el número es tuyo.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun ResumenCard(r: ResumenCreditos) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))), RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text("Total pendiente", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
        Text(
            CalculadoraCuenta.formatoCOP(r.saldoTotal),
            color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold,
        )
        val sufijo = if (r.vencidos > 0) " · ${r.vencidos} ${if (r.vencidos == 1) "vencido" else "vencidos"}" else ""
        Text(
            "${r.activos} ${if (r.activos == 1) "crédito" else "créditos"}$sufijo",
            color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun FilaCredito(c: CreditoApp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(c.negocio.nombre ?: "Negocio", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                textoVencimiento(c) + (c.numeroFactura?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                CalculadoraCuenta.formatoCOP(if (c.estado == "PAGADO") c.montoTotal + c.montoInteres else c.saldo),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            )
            Text(textoEstado(c.estado), style = MaterialTheme.typography.labelSmall, color = colorEstado(c.estado), fontWeight = FontWeight.SemiBold)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
    }
}

/** "Vence hoy", "Vence en 3 días", "Venció hace 2 días"… espejo de
 * `textoVencimiento()` en `frontend/lib/app-clientes/creditos.ts`. */
fun textoVencimiento(c: CreditoApp): String {
    if (c.estado == "PAGADO") return "Pagado"
    val dias = c.diasParaVencer ?: return "Sin fecha de vencimiento"
    if (c.estado == "SIN_FECHA") return "Sin fecha de vencimiento"
    return when {
        dias == 0 -> "Vence hoy"
        dias == 1 -> "Vence mañana"
        dias > 1 -> "Vence el ${fechaCorta(c.fechaLimite)}"
        else -> "Venció hace ${Math.abs(dias)} ${if (Math.abs(dias) == 1) "día" else "días"}"
    }
}

fun textoEstado(estado: String): String = when (estado) {
    "AL_DIA" -> "Al día"
    "POR_VENCER" -> "Por vencer"
    "VENCIDO" -> "Vencido"
    "PAGADO" -> "Pagado"
    else -> "Sin fecha"
}

@Composable
fun colorEstado(estado: String): Color = when (estado) {
    "VENCIDO" -> MaterialTheme.colorScheme.error
    "POR_VENCER" -> MaterialTheme.colorScheme.tertiary
    "AL_DIA" -> ColorExito
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** `fecha_limite` llega como "YYYY-MM-DD"; se arma como fecha LOCAL para no
 * correrse un día (mismo gotcha que `fechaCorta()` en la web). */
fun fechaCorta(iso: String?): String {
    if (iso == null || iso.length < 10) return ""
    val partes = iso.take(10).split("-").mapNotNull { it.toIntOrNull() }
    if (partes.size != 3) return ""
    val meses = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    return "${partes[2]} ${meses[partes[1] - 1]} ${partes[0]}"
}

