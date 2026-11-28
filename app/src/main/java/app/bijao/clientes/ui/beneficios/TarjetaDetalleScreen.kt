package app.bijao.clientes.ui.beneficios

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bijao.clientes.core.models.MovimientoTarjeta
import app.bijao.clientes.core.models.Tarjeta
import app.bijao.clientes.core.models.TarjetaDetalle
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import kotlinx.coroutines.CancellationException

private const val VISIBLES = 5

/**
 * Una tarjeta de sellos: la tarjeta, qué se gana y cuándo, cómo se suman
 * sellos y el historial («Tus sellos»). Espejo de
 * `beneficios/tarjetas/[id]/page.tsx` y de `TarjetaDetalleView` en iOS;
 * contrato `GET /api/app/beneficios/{id}`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarjetaDetalleScreen(recompensaId: String, onAtras: () -> Unit) {
    var detalle by remember { mutableStateOf<TarjetaDetalle?>(null) }
    var logo by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var verTodo by remember { mutableStateOf(false) }

    LaunchedEffect(recompensaId) {
        try {
            val d = TarjetaDetalle.desde(ApiClient.service.tarjeta(recompensaId).datoOLanzar().data)
            detalle = d
            logo = try { ApiClient.service.negocioDetalle(d.tarjeta.negocio.id).datoOLanzar().data.logoUrl } catch (e: Exception) {
                if (e is CancellationException) throw e
                null
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            error = e.message ?: "No encontramos esta tarjeta"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detalle?.tarjeta?.negocio?.nombre ?: "Tarjeta", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a Beneficios")
                    }
                },
            )
        },
    ) { padding ->
        val d = detalle
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                d == null && error != null -> Text(error ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(32.dp))
                d == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> Contenido(d, logo, verTodo) { verTodo = !verTodo }
            }
        }
    }
}

@Composable
private fun Contenido(d: TarjetaDetalle, logo: String?, verTodo: Boolean, onVerTodo: () -> Unit) {
    val t = d.tarjeta
    val color = colorDeHex(t.color) ?: ColorTarjetaPorDefecto
    val faltan = maxOf(0, t.meta - t.saldo)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TarjetaSellos(t, logoUrl = logo)
        if (d.googleWallet) BotonGoogleWallet(t.recompensaId)

        if (!t.agotada) {
            Seccion {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).background(Color(0xFFFFF8E6), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(24.dp))
                    }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(
                            if (faltan == 0) "Tarjeta completa: ${t.textoPremio}"
                            else "Con $faltan ${if (faltan == 1) "sello" else "sellos"} más: ${t.textoPremio}",
                            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 19.sp,
                        )
                        Text(
                            if (faltan == 0) "Tu premio va camino a Bonos."
                            else "Llega solo a tu billetera como voucher. Tendrás ${t.premioDias ?: 30} días para usarlo.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Seccion {
            Text("Cómo sumar sellos", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Paso("1", "Muestra tu código Bijao en caja antes de pagar.", color)
            Paso("+1", "Cada compra suma 1 sello al cobrarla.", color)
            Paso(null, condicion(t), color)
        }

        Seccion {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tus sellos", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                if (d.historial.size > VISIBLES) {
                    TextButton(onClick = onVerTodo) { Text(if (verTodo) "Ver menos" else "Ver todo") }
                }
            }
            if (d.historial.isEmpty()) {
                Text("Aún no tienes sellos en esta tarjeta.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val lista = if (verTodo) d.historial else d.historial.take(VISIBLES)
                lista.forEachIndexed { i, m ->
                    Movimiento(m, t.meta, color)
                    if (i < lista.size - 1) HorizontalDivider()
                }
            }
        }

        Text("Al completar la tarjeta, el premio pasa a Bonos y empiezas una tarjeta nueva desde cero.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp))
    }
}

private fun condicion(t: Tarjeta): String = when {
    t.aplicaCualquierCompra -> "Suma con cualquier producto o servicio de ${t.negocio.nombre ?: "este negocio"}."
    !t.sumaCon.isNullOrEmpty() -> "Solo suma si la compra incluye ${FormatoBonos.enumerar(t.sumaCon)}."
    else -> "Solo suma con productos concretos. Pregunta en el negocio cuáles."
}

@Composable
private fun Seccion(contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = contenido,
    )
}

/** Marca redonda en el color de la tarjeta; `null` = un visto. */
@Composable
private fun Marca(texto: String?, color: Color) {
    Box(
        Modifier.defaultMinSize(26.dp, 26.dp).background(color.aclarado(0.12f), CircleShape).padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (texto == null) Icon(Icons.Filled.Check, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        else Text(texto, color = color, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun Paso(marca: String?, texto: String, color: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Marca(marca, color)
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 12.dp, top = 3.dp))
    }
}

@Composable
private fun Movimiento(m: MovimientoTarjeta, meta: Int, color: Color) {
    val detalle = when {
        m.tipo == "ACUMULACION" && (m.monto ?: 0.0) > 0 -> "compra de ${FormatoBonos.pesos(m.monto)}"
        m.tipo == "CANJE" -> "el premio pasó a Bonos"
        else -> m.motivo.lowercase()
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (m.tipo == "CANJE") {
            Box(Modifier.size(26.dp).background(color.aclarado(0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            }
        } else {
            Marca(if (m.sellos > 0) "+${m.sellos}" else "${m.sellos}", color)
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                    append(FormatoBonos.diaCorto(m.fecha))
                }
                append(" · $detalle")
            },
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        Text("${m.saldo}/$meta", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
