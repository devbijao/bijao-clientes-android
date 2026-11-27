package app.bijao.clientes.ui.beneficios

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.bijao.clientes.core.models.Bono
import app.bijao.clientes.core.models.BonoDetalle
import app.bijao.clientes.core.models.MovimientoBono
import app.bijao.clientes.core.models.Negocio
import app.bijao.clientes.core.models.QRBono
import app.bijao.clientes.core.models.ReclamoBono
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Fase { DETALLE, QR, ARRANCADO }
private const val CONSULTA_MS = 2_000L

/**
 * Un bono: el voucher, dónde se usa, sus movimientos y «Usar en caja». Espejo
 * de `beneficios/bonos/[id]/page.tsx` y de `BonoDetalleView` en iOS; contrato
 * en `backend/routers/app_bonos.py`.
 *
 * «Usar en caja» muestra un QR que lleva el bono adentro (se renueva solo cada
 * 90 s). Con el QR abierto la app consulta `/estado` cada 2 s, solo con la
 * pantalla a la vista: un reclamo nuevo (distinto del que había al abrir)
 * significa que el cajero lo escaneó, y ahí el voucher se arranca. Cuando la
 * venta se cobra llegan el monto y el saldo que queda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonoDetalleScreen(bonoId: String, onAtras: () -> Unit, onNegocioClick: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val ciclo = LocalLifecycleOwner.current.lifecycle
    val haptico = LocalHapticFeedback.current

    var detalle by remember { mutableStateOf<BonoDetalle?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var fase by remember { mutableStateOf(Fase.DETALLE) }
    var qr by remember { mutableStateOf<QRBono?>(null) }
    var imagenQR by remember { mutableStateOf<Bitmap?>(null) }
    var restante by remember { mutableIntStateOf(0) }
    var pidiendoQr by remember { mutableStateOf(false) }
    var reclamo by remember { mutableStateOf<ReclamoBono?>(null) }
    var saldoFinal by remember { mutableStateOf<Double?>(null) }
    var animacionLista by remember { mutableStateOf(false) }
    // Id del último reclamo al abrir el QR: uno distinto = lo acaban de escanear.
    var base by remember { mutableStateOf<String?>(null) }
    var directorio by remember { mutableStateOf<List<Negocio>?>(null) }

    suspend fun cargar() {
        try {
            detalle = BonoDetalle.desde(ApiClient.service.bono(bonoId).datoOLanzar().data)
            error = null
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            error = e.message ?: "Inténtalo de nuevo en un momento."
        }
    }

    suspend fun pedirQR() {
        pidiendoQr = true
        aviso = null
        try {
            if (base == null) base = ApiClient.service.estadoBono(bonoId).datoOLanzar().data.reclamo?.id ?: ""
            val nuevo = ApiClient.service.qrBono(bonoId).datoOLanzar().data
            imagenQR = withContext(Dispatchers.Default) { generarBitmapQR(context, nuevo.token, 600) }
            qr = nuevo
            restante = nuevo.ttlSegundos
            fase = Fase.QR
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            aviso = e.message
            fase = Fase.DETALLE
        } finally {
            pidiendoQr = false
        }
    }

    fun cerrarQR() {
        qr = null; imagenQR = null; fase = Fase.DETALLE
    }

    fun terminar() {
        base = null; qr = null; imagenQR = null; reclamo = null; saldoFinal = null; animacionLista = false
        fase = Fase.DETALLE
        scope.launch { cargar() }
    }

    LaunchedEffect(bonoId) { cargar() }

    // Cuenta atrás del QR; al vencer se pide otro solo.
    LaunchedEffect(qr?.token, fase) {
        if (qr == null || fase != Fase.QR) return@LaunchedEffect
        while (restante > 0) {
            delay(1_000)
            restante -= 1
        }
        pedirQR()
    }

    // Con el QR abierto (o esperando el cobro) se pregunta si el cajero ya lo
    // escaneó, solo mientras la pantalla está a la vista.
    val terminada = saldoFinal != null || reclamo?.confirmado == true
    LaunchedEffect(fase == Fase.DETALLE, terminada) {
        if (fase == Fase.DETALLE || terminada) return@LaunchedEffect
        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(CONSULTA_MS)
                // Una consulta fallida no corta: la siguiente lo intenta otra vez.
                val estado = try { ApiClient.service.estadoBono(bonoId).datoOLanzar().data } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    null
                } ?: continue
                val r = estado.reclamo ?: continue
                if (r.id == base) continue
                if (r.noSeAplico) {
                    base = r.id; reclamo = null; qr = null; imagenQR = null
                    aviso = "El cajero no aplicó el bono en esta compra. Sigue intacto en tu billetera."
                    fase = Fase.DETALLE
                    return@repeatOnLifecycle
                }
                if (reclamo?.id != r.id) haptico.performHapticFeedback(HapticFeedbackType.LongPress)
                reclamo = r
                fase = Fase.ARRANCADO
                if (r.confirmado) {
                    saldoFinal = estado.saldo ?: 0.0
                    haptico.performHapticFeedback(HapticFeedbackType.LongPress)
                    return@repeatOnLifecycle
                }
            }
        }
    }

    BackHandler(enabled = fase == Fase.QR) { cerrarQR() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (fase == Fase.DETALLE) detalle?.bono?.nombre ?: "Bono" else "", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { if (fase == Fase.QR) cerrarQR() else onAtras() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (fase == Fase.QR) "Volver al bono" else "Volver a Beneficios")
                    }
                },
            )
        },
    ) { padding ->
        val d = detalle
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                d == null && error != null -> Text(error ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp), textAlign = TextAlign.Center)
                d == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    when (fase) {
                        Fase.DETALLE -> VistaDetalle(
                            d, aviso, pidiendoQr, directorio,
                            onUsar = { scope.launch { pedirQR() } },
                            onNegocioClick = onNegocioClick,
                            onCargarDirectorio = {
                                scope.launch {
                                    directorio = try {
                                        ApiClient.service.directorio(limite = 12).datoOLanzar().data.filterNot { it.id in d.excluidos }
                                    } catch (e: Exception) {
                                        if (e is CancellationException) throw e
                                        emptyList()
                                    }
                                }
                            },
                        )
                        Fase.QR -> VistaQR(d.bono, qr, imagenQR, restante)
                        Fase.ARRANCADO -> reclamo?.let { r ->
                            VistaArrancado(d.bono, r, saldoFinal, animacionLista,
                                onAnimacionLista = { animacionLista = true }, onListo = { terminar() })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VistaDetalle(
    d: BonoDetalle,
    aviso: String?,
    pidiendoQr: Boolean,
    directorio: List<Negocio>?,
    onUsar: () -> Unit,
    onNegocioClick: (String) -> Unit,
    onCargarDirectorio: () -> Unit,
) {
    val b = d.bono
    val colorBono = colorDeHex(b.color) ?: ColorBonoPorDefecto
    Voucher(b, Modifier.alpha(if (b.estaPausado) 0.6f else 1f))

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(b.nombre ?: "Bono", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        b.descripcion?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        var condiciones = "Vence el ${FormatoBonos.dia(b.venceEn)}"
        condiciones += if (b.esDeSaldo) " · puedes usarlo en varias compras hasta agotarlo" else " · se usa una sola vez"
        b.topePorCompra?.takeIf { it > 0 }?.let { condiciones += " · hasta ${FormatoBonos.pesos(it)} por compra" }
        Text(condiciones, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (aviso != null) {
        Text(aviso, color = Color(0xFF78350F), fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFFBEB)).padding(14.dp))
    }

    if (b.estaActivo) {
        Button(
            onClick = onUsar, enabled = !pidiendoQr,
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorBono, contentColor = tintaSobre(b.color),
                disabledContainerColor = colorBono.copy(alpha = 0.7f), disabledContentColor = tintaSobre(b.color)),
        ) {
            if (pidiendoQr) {
                CircularProgressIndicator(Modifier.size(16.dp), color = tintaSobre(b.color), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
            }
            Text("Usar en caja", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    } else {
        Text(if (b.estaPausado) "Este bono está en pausa por ahora." else "Este bono ya no está disponible.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Dónde usarlo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp))
        }
        if (b.alcance == "TODOS") {
            Text("Tú decides: sirve en cualquier negocio Bijao${if (b.uso == "SALDO") " y puedes repartirlo entre varios" else ""}.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (directorio == null) {
                LaunchedEffect(Unit) { onCargarDirectorio() }
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                directorio.forEach { n ->
                    FilaNegocio(n.nombre ?: "Negocio", n.logoUrl, listOfNotNull(n.tipoNegocio, n.distancia ?: n.ciudad).joinToString(" · "),
                        colorBono) { onNegocioClick(n.id) }
                }
            }
        } else {
            d.negocios.forEach { n ->
                FilaNegocio(n.nombre ?: "Negocio", n.logoUrl, n.ciudad ?: "", colorBono) { onNegocioClick(n.id) }
            }
        }
    }

    if (d.movimientos.isNotEmpty()) Movimientos(d.movimientos)
}

@Composable
private fun FilaNegocio(nombre: String, logo: String?, subtitulo: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (logo == null) color else Color.White),
            contentAlignment = Alignment.Center,
        ) {
            if (logo != null) AsyncImage(logo, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            else Text(nombre.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (subtitulo.isNotBlank()) {
                Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Movimientos(lista: List<MovimientoBono>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Movimientos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp)) {
            lista.forEachIndexed { i, m ->
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.negocio ?: "Negocio", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(listOfNotNull(FormatoBonos.dia(m.fecha).ifBlank { null }, m.numero).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("−${FormatoBonos.pesos(m.monto)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                if (i < lista.size - 1) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun VistaQR(b: Bono, qr: QRBono?, imagenQR: Bitmap?, restante: Int) {
    val (valor, _) = FormatoBonos.textos(b)
    val tinta = tintaSobre(b.color)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(colorDeHex(b.color) ?: ColorBonoPorDefecto).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(b.etiqueta.uppercase(), color = tinta, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
        if (b.esEnPesos) Text(valor, color = tinta, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 42.sp)
        Column(
            Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Color.White)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Muestra este código en caja", color = Color(0xFF181818), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                if (imagenQR != null) {
                    Image(imagenQR.asImageBitmap(), contentDescription = "Código QR del bono",
                        modifier = Modifier.fillMaxSize().alpha(if (restante <= 0) 0.25f else 1f))
                } else {
                    CircularProgressIndicator()
                }
            }
            if (qr != null) {
                Text(FormatoBonos.paraDictar(qr.token), color = Color(0xFF181818), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp))
                Text(if (restante > 0) "El código cambia en %d:%02d".format(restante / 60, restante % 60) else "Renovando…",
                    color = Color(0xFF6B7280), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
            }
        }
        Text("${b.donde ?: "En los negocios Bijao"}. El cajero lo escanea con el mismo lector de tu código Bijao.",
            color = tinta, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
    }
}

@Composable
private fun VistaArrancado(
    b: Bono,
    reclamo: ReclamoBono,
    saldoFinal: Double?,
    animacionLista: Boolean,
    onAnimacionLista: () -> Unit,
    onListo: () -> Unit,
) {
    val (valor, _) = FormatoBonos.textos(b)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE6F8EE)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF00913C))
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text("${reclamo.negocio ?: "El negocio"} escaneó tu bono", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            Text(if (reclamo.confirmado && reclamo.monto != null) "Se aplicaron ${FormatoBonos.pesos(reclamo.monto)}" else "Bono reclamado en caja",
                fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
    VoucherArrancado(
        color = b.color, etiqueta = b.etiqueta, negocio = reclamo.negocio, valor = valor,
        saldoInicial = if (b.esEnPesos) b.saldo else null, saldoFinal = if (b.esEnPesos) saldoFinal else null,
        montoUsado = if (reclamo.confirmado) reclamo.monto else null, onTerminado = onAnimacionLista,
    )
    AnimatedVisibility(animacionLista, enter = fadeIn() + slideInVertically { it / 2 }) {
        Column(
            Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (reclamo.confirmado) {
                Text("¡Listo!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(resumenUso(b, reclamo, saldoFinal), style = MaterialTheme.typography.bodyMedium)
                val inicial = b.valorInicial
                if (b.esEnPesos && inicial != null && inicial > 0 && saldoFinal != null) {
                    val usado = (1 - saldoFinal / inicial).toFloat().coerceIn(0f, 1f)
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(Modifier.fillMaxWidth(usado).height(10.dp).clip(CircleShape).background(colorDeHex(b.color) ?: ColorBonoPorDefecto))
                    }
                    Row {
                        Text("Usado ${FormatoBonos.pesos(inicial - saldoFinal)}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Disponible ${FormatoBonos.pesos(saldoFinal)}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                }
                reclamo.numero?.let { numero ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
                        Text("Guardado con el comprobante", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(numero, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Button(onClick = onListo, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("Listo", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Esperando a que ${reclamo.negocio ?: "el negocio"} cobre la venta…", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
    }
}

private fun resumenUso(b: Bono, r: ReclamoBono, saldoFinal: Double?) = buildAnnotatedString {
    val negocio = r.negocio ?: "el negocio"
    if (!b.esEnPesos) {
        append("Usaste tu bono en $negocio.")
        return@buildAnnotatedString
    }
    append("Usaste ")
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(FormatoBonos.pesos(r.monto)) }
    append(" en $negocio.")
    if (saldoFinal != null && saldoFinal > 0) {
        append(" Te quedan ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(FormatoBonos.pesos(saldoFinal)) }
        val donde = b.donde.orEmpty()
        append(" para usar ${donde.take(1).lowercase()}${donde.drop(1)}.")
    } else {
        append(" Ya no le queda saldo.")
    }
}
