package app.bijao.clientes.ui.beneficios

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bijao.clientes.core.models.Bono
import app.bijao.clientes.core.models.BonoGuardado
import app.bijao.clientes.core.models.Tarjeta
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

private enum class Pestana(val nombre: String) { BONOS("Bonos"), TARJETAS("Tarjetas"), GUARDADOS("Guardados") }

/** La ceremonia solo para premios frescos; la franja, una semana. */
private const val CEREMONIA_DIAS = 3L
private const val FRANJA_DIAS = 7L

private fun reciente(b: Bono, dias: Long): Boolean {
    val emitido = FormatoBonos.instante(b.emitidoAt) ?: return false
    return Duration.between(emitido, Instant.now()) < Duration.ofDays(dias)
}

/**
 * Beneficios: bonos, tarjetas de sellos y lo ya usado (Guardados), en tres
 * pestañas. Espejo de `beneficios/page.tsx` + `_tarjetas.tsx` en la web y de
 * `BeneficiosView` en iOS; plan en `MDs/plan_bonos.md` (F3 y F5).
 *
 * Abre en Bonos si hay alguno vigente; si no, en Tarjetas. Ya no hay
 * «Canjear» ni vales: al completar una tarjeta el premio llega solo a Bonos,
 * y la pestaña Tarjetas lo celebra la primera vez que la persona vuelve
 * (sellos que caen, ceremonia del premio, franja «¡Completaste la tarjeta!»).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiosScreen(onBonoClick: (String) -> Unit, onTarjetaClick: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val recuerdos = RecuerdosBeneficios(LocalContext.current)

    // Saveable: al volver del detalle de un bono o una tarjeta, sigue en la misma pestaña.
    var pestana by rememberSaveable { mutableStateOf<Pestana?>(null) }
    var bonos by remember { mutableStateOf<List<Bono>>(emptyList()) }
    var guardados by remember { mutableStateOf<List<BonoGuardado>>(emptyList()) }
    var errorBonos by remember { mutableStateOf<String?>(null) }
    var tarjetas by remember { mutableStateOf<List<Tarjeta>>(emptyList()) }
    var errorTarjetas by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var logos by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var nuevos by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var premios by remember { mutableStateOf<List<Bono>>(emptyList()) }
    var cerrados by remember { mutableStateOf(recuerdos.avisosCerrados) }
    var ceremonia by remember { mutableStateOf<Pair<Tarjeta, Bono>?>(null) }
    var ceremoniaRevisada by remember { mutableStateOf(false) }

    /** El premio más nuevo aún sin celebrar, si es fresco y su tarjeta está. */
    fun revisarCeremonia() {
        if (ceremoniaRevisada || errorBonos != null || errorTarjetas != null) return
        ceremoniaRevisada = true
        val vistos = recuerdos.premiosVistos
        val bono = premios.firstOrNull { it.id !in vistos && reciente(it, CEREMONIA_DIAS) }
        val tarjeta = bono?.let { b -> tarjetas.firstOrNull { it.recompensaId == b.tarjeta } }
        if (bono != null && tarjeta != null) ceremonia = tarjeta to bono
        recuerdos.premiosVistos = vistos + premios.map { it.id }
    }

    suspend fun cargar() {
        cargando = true
        try {
            coroutineScope {
                val pedidoBonos = async { runCatching { ApiClient.service.bonos().datoOLanzar() } }
                val pedidoTarjetas = async { runCatching { ApiClient.service.beneficios().datoOLanzar().data } }
                pedidoBonos.await()
                    .onSuccess { bonos = it.data; guardados = it.guardados; errorBonos = null }
                    .onFailure { if (it is CancellationException) throw it; errorBonos = "No pudimos cargar tus bonos. Inténtalo de nuevo en un momento." }
                pedidoTarjetas.await()
                    .onSuccess { tarjetas = it; errorTarjetas = null }
                    .onFailure { if (it is CancellationException) throw it; errorTarjetas = "No pudimos cargar tus tarjetas. Inténtalo de nuevo en un momento." }
            }
        } finally {
            cargando = false
        }

        // Sellos ganados desde la última visita.
        recuerdos.saldos?.let { antes ->
            nuevos = tarjetas.mapNotNull { t ->
                val previo = antes[t.recompensaId] ?: return@mapNotNull null
                if (t.saldo > previo) t.recompensaId to minOf(3, t.saldo - previo) else null
            }.toMap()
        }
        if (errorTarjetas == null) recuerdos.saldos = tarjetas.associate { it.recompensaId to it.saldo }

        premios = bonos.filter { it.origen == "FIDELIDAD" && it.estaActivo && it.tarjeta != null }
            .sortedByDescending { it.emitidoAt ?: "" }

        if (pestana == null) pestana = if (bonos.isEmpty()) Pestana.TARJETAS else Pestana.BONOS
        ceremoniaRevisada = false
        if (pestana == Pestana.TARJETAS) revisarCeremonia()

        // El logo no viene en /beneficios (son base64): uno por negocio, aparte.
        tarjetas.map { it.negocio.id }.distinct().filterNot { logos.containsKey(it) }.forEach { id ->
            try {
                ApiClient.service.negocioDetalle(id).datoOLanzar().data.logoUrl?.let { logos = logos + (id to it) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    LaunchedEffect(Unit) { cargar() }
    LaunchedEffect(pestana) { if (pestana == Pestana.TARJETAS) revisarCeremonia() }

    ceremonia?.let { (tarjeta, bono) ->
        CeremoniaPremio(
            tarjeta = tarjeta, bono = bono, logoUrl = logos[tarjeta.negocio.id],
            onVer = { ceremonia = null; pestana = Pestana.BONOS; onBonoClick(bono.id) },
            onCerrar = { ceremonia = null },
        )
    }

    Column(Modifier.fillMaxSize()) {
        Text("Mis beneficios", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
            Pestana.entries.forEachIndexed { i, p ->
                val conteo = when (p) {
                    Pestana.BONOS -> bonos.size
                    Pestana.TARJETAS -> tarjetas.size
                    Pestana.GUARDADOS -> 0
                }
                SegmentedButton(
                    selected = (pestana ?: Pestana.TARJETAS) == p,
                    onClick = { pestana = p },
                    enabled = pestana != null,
                    shape = SegmentedButtonDefaults.itemShape(i, Pestana.entries.size),
                    icon = {},
                    label = { Text(if (conteo > 0) "${p.nombre} · $conteo" else p.nombre, maxLines = 1, fontSize = 13.sp) },
                )
            }
        }

        PullToRefreshBox(isRefreshing = cargando && pestana != null, onRefresh = { scope.launch { cargar() } }, modifier = Modifier.weight(1f)) {
            when (pestana) {
                null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                Pestana.BONOS -> ListaBonos(bonos, errorBonos, onBonoClick) { scope.launch { cargar() } }
                Pestana.GUARDADOS -> ListaGuardados(guardados)
                Pestana.TARJETAS -> {
                    val franja = premios.firstOrNull { reciente(it, FRANJA_DIAS) && it.id !in cerrados }
                        .takeIf { ceremonia == null }
                    val conAviso = tarjetas.firstOrNull { (nuevos[it.recompensaId] ?: 0) > 0 && it.saldo < it.meta }
                        .takeIf { ceremonia == null }
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = if (conAviso != null) 110.dp else 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (franja != null) {
                                item(key = "franja") {
                                    FranjaPremio(
                                        negocio = tarjetas.firstOrNull { it.recompensaId == franja.tarjeta }?.negocio?.nombre,
                                        onVer = { onBonoClick(franja.id) },
                                        onCerrar = { cerrados = cerrados + franja.id; recuerdos.avisosCerrados = cerrados },
                                    )
                                }
                            }
                            when {
                                errorTarjetas != null && tarjetas.isEmpty() -> item { ErrorConReintento(errorTarjetas!!) { scope.launch { cargar() } } }
                                tarjetas.isEmpty() && !cargando -> item {
                                    Vacio("Aún no tienes tarjetas",
                                        "Se crean solas cuando compras en un negocio Bijao con programa de sellos y muestras tu código en caja.")
                                }
                                else -> items(tarjetas, key = { it.recompensaId }) { t ->
                                    TarjetaSellos(
                                        t, logoUrl = logos[t.negocio.id],
                                        animarSellos = if (ceremonia?.first?.recompensaId == t.recompensaId) 0 else nuevos[t.recompensaId] ?: 0,
                                        modifier = Modifier.clickable { onTarjetaClick(t.recompensaId) },
                                    )
                                }
                            }
                        }
                        if (conAviso != null) {
                            AvisoSellos(conAviso, nuevos[conAviso.recompensaId] ?: 0, Modifier.align(Alignment.BottomCenter)) { nuevos = emptyMap() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListaBonos(bonos: List<Bono>, error: String?, onBonoClick: (String) -> Unit, onReintentar: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            error != null && bonos.isEmpty() -> item { ErrorConReintento(error, onReintentar) }
            bonos.isEmpty() -> item {
                Vacio("Aún no tienes bonos", "Aquí llegan los premios de eventos, los regalos de Bijao y los de tus tarjetas de sellos.")
            }
            else -> {
                items(bonos, key = { it.id }) { b ->
                    Voucher(b, Modifier.alpha(if (b.estaPausado) 0.6f else 1f).clip(RoundedCornerShape(18.dp)).clickable { onBonoClick(b.id) })
                }
                item {
                    Text("Toca un bono en el negocio para mostrar su código en caja. Lo que te quede de saldo sigue aquí.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ListaGuardados(guardados: List<BonoGuardado>) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (guardados.isEmpty()) {
            item { Vacio("Nada guardado todavía", "Cuando uses un bono, su comprobante queda guardado aquí.") }
        } else {
            items(guardados, key = { "${it.bono}-${it.numero ?: it.tipo}-${it.fecha}" }) { FilaGuardado(it) }
        }
    }
}

@Composable
private fun FilaGuardado(g: BonoGuardado) {
    val usado = g.tipo == "USO"
    val tinta = Color(0xFF3A3F4B)
    val sello = if (usado) Color(0xFF8F2219) else Color(0xFF5B6170)
    Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE3E6EC))) {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(76.dp).fillMaxHeight().padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(g.numero ?: "SIN USAR", color = tinta, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                Box(Modifier.size(8.dp, 16.dp).clip(RoundedCornerShape(2.dp)).background(colorDeHex(g.color) ?: ColorBonoPorDefecto))
            }
            Canvas(Modifier.width(2.dp).fillMaxHeight()) {
                drawLine(tinta.copy(alpha = 0.25f), Offset(1f, 0f), Offset(1f, size.height), 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(listOfNotNull(g.etiqueta, g.negocio).joinToString(" · "), color = tinta, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(if (usado) "−${FormatoBonos.pesos(g.monto)}" else g.nombre ?: "Bono", color = tinta, fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(if (usado) FormatoBonos.dia(g.fecha) else "Venció el ${FormatoBonos.dia(g.fecha)} sin usarse", color = tinta,
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        Text(
            if (usado) "USADO" else "VENCIDO", color = sello, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).rotate(-12f)
                .border(2.5.dp, sello, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun FranjaPremio(negocio: String?, onVer: () -> Unit, onCerrar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFF8E6))
            .border(1.dp, AmarilloPremio.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f).clickable(onClick = onVer).padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = Color(0xFFB45309))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF3D2A00))) {
                        append("¡Completaste la tarjeta${negocio?.let { " de $it" } ?: ""}!")
                    }
                    append(" Tu premio ya está en Bonos.")
                },
                color = Color(0xFF3D3420), fontSize = 13.sp, lineHeight = 17.sp, modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color(0xFF3D2A00))
        }
        IconButton(onClick = onCerrar) {
            Icon(Icons.Filled.Close, contentDescription = "Ocultar aviso", tint = Color(0xFF3D2A00).copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AvisoSellos(t: Tarjeta, n: Int, modifier: Modifier, onCerrar: () -> Unit) {
    Row(
        modifier.padding(16.dp).fillMaxWidth().shadow(12.dp, RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("+$n ${if (n == 1) "sello" else "sellos"} en ${t.negocio.nombre ?: "el negocio"}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text("${if (t.faltan == 1) "Te falta 1" else "Te faltan ${t.faltan}"} para ${t.textoPremio}.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onCerrar) {
            Icon(Icons.Filled.Close, contentDescription = "Ocultar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Vacio(titulo: String, texto: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ErrorConReintento(texto: String, onReintentar: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        TextButton(onClick = onReintentar) { Text("Reintentar") }
    }
}
