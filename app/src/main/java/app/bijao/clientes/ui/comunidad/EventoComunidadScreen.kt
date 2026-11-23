package app.bijao.clientes.ui.comunidad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.bijao.clientes.core.models.AvanceComunidad
import app.bijao.clientes.core.models.EstadoComunidad
import app.bijao.clientes.core.models.EventoComunidad
import app.bijao.clientes.core.models.MensajeHiloComunidad
import app.bijao.clientes.core.models.PremioComunidad
import app.bijao.clientes.core.models.PreguntaComunidad
import app.bijao.clientes.core.models.ResponderComunidadInput
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.ApiException
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.ui.ColorExito
import app.bijao.clientes.ui.explorar.EstadoVacio
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * El evento de Comunidad visto por el participante. Espejo de
 * `perfil/comunidad/[slug]/page.tsx` en la web y de `EventoComunidadView.swift`
 * en iOS (D16–D19 de MDs/plan_comunidad.md).
 *
 * Cada pregunta es un hilo en vivo tipo chat. Se consulta el estado cada 3 s
 * mandando la última `version`; si nada cambió, el backend responde
 * `sin_cambios` y no se repinta. Con la app en segundo plano no se consulta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventoComunidadScreen(slug: String, onAtras: () -> Unit) {
    var estado by remember { mutableStateOf<EstadoComunidad?>(null) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val ciclo = LocalLifecycleOwner.current.lifecycle

    suspend fun consultar() {
        try {
            val nuevo = ApiClient.service.estadoComunidad(slug, estado?.version).datoOLanzar()
            if (nuevo.sinCambios && estado != null) return
            estado = nuevo
            mensajeError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            if (estado == null) {
                mensajeError = if (e.estado == 404) "Este evento no existe o aún no está publicado." else e.message
            }
        } catch (e: Exception) {
            if (estado == null) mensajeError = "No pudimos cargar el evento."
        }
    }

    LaunchedEffect(slug) {
        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                consultar()
                delay(3_000)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comunidad") },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (estado?.evento?.enVivo == true) {
                        EtiquetaComunidad("En vivo", MaterialTheme.colorScheme.error, punto = true)
                        Spacer(Modifier.width(16.dp))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val actual = estado
            val evento = actual?.evento
            val avance = actual?.avance
            when {
                actual != null && evento != null && avance != null -> Contenido(actual, evento, avance, slug) {
                    scope.launch { consultar() }
                }
                mensajeError != null -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EstadoVacio(icono = Icons.Filled.EventBusy, titulo = mensajeError ?: "", descripcion = "")
                    TextButton(onClick = { scope.launch { consultar() } }) { Text("Reintentar") }
                }
                else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun Contenido(
    estado: EstadoComunidad,
    evento: EventoComunidad,
    avance: AvanceComunidad,
    slug: String,
    alEnviar: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { TarjetaEvento(evento, avance) }
        estado.premio?.let { premio -> item { TarjetaPremio(premio) } }
        if (evento.estado == "cerrado") {
            item {
                Text(
                    "El evento terminó: ya no se reciben respuestas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                )
            }
        }
        item {
            Text(
                "Preguntas",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        if (estado.preguntas.isEmpty()) {
            item {
                Text(
                    "Las preguntas aparecerán aquí durante el evento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(vertical = 24.dp),
                )
            }
        }
        items(estado.preguntas, key = { it.clave }) { pregunta ->
            Pregunta(pregunta, slug, evento.enVivo, alEnviar)
        }
        item {
            Text(
                "Cuenta una respuesta aprobada por pregunta. Necesitas ${avance.minimo} para recibir el premio.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
            )
        }
    }
}

// ── Tarjeta del evento y premio ──────────────────────────────────────────────

private val GradienteEvento = Brush.linearGradient(
    listOf(Color(0xFF007BFF), Color(0xFF1A5FC4), Color(0xFF0D1A47)),
)

@Composable
private fun TarjetaEvento(evento: EventoComunidad, avance: AvanceComunidad) {
    val forma = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(GradienteEvento)
            .border(1.dp, Color.White.copy(alpha = 0.25f), forma),
    ) {
        evento.imagenUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (evento.enVivo) {
                Row(
                    Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).background(Color(0xFFFF8FA3), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("En vivo · Comunidad", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Text(
                listOfNotNull(evento.organizador?.takeIf { it.isNotBlank() }, FechasComunidad.larga(evento.fechaInicio))
                    .joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
            )
            Text(evento.titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
            evento.descripcion?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            }
            Progreso(avance)
            Text(
                when {
                    avance.cumple -> "¡Listo! Ya tienes las participaciones para el premio."
                    avance.minimo - avance.aprobadas == 1 -> "Te falta 1 participación aprobada para el premio."
                    else -> "Te faltan ${avance.minimo - avance.aprobadas} participaciones aprobadas para el premio."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun Progreso(avance: AvanceComunidad) {
    val hechas = minOf(avance.aprobadas, avance.minimo)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .semantics { contentDescription = "${avance.aprobadas} de ${avance.minimo} participaciones aprobadas" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (avance.minimo <= 10) {
            repeat(avance.minimo) { i ->
                val llena = i < avance.aprobadas
                val relleno by animateColorAsState(if (llena) Color.White else Color.White.copy(alpha = 0.1f), label = "paso")
                Box(
                    Modifier
                        .size(22.dp)
                        .background(relleno, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = if (llena) 0.9f else 0.4f), CircleShape),
                )
            }
            Spacer(Modifier.weight(1f))
        } else {
            val fraccion by animateFloatAsState(hechas.toFloat() / avance.minimo, label = "avance")
            LinearProgressIndicator(
                progress = { fraccion },
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.weight(1f),
            )
        }
        Text("$hechas de ${avance.minimo}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun TarjetaPremio(premio: PremioComunidad) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(ColorExito.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = ColorExito, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("¡Cumpliste el reto!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = ColorExito)
        }
        premio.titulo?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
        premio.descripcion?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        premio.comoReclamar?.takeIf { it.isNotBlank() }?.let {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Cómo reclamarlo: ") }
                    append(it)
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                    .padding(12.dp),
            )
        }
    }
}

// ── Pregunta ─────────────────────────────────────────────────────────────────

private fun resumen(p: PreguntaComunidad): String {
    val t = p.totalHilo
    val a = p.aprobadasHilo
    return "$t respuesta${if (t == 1) "" else "s"} · $a aprobada${if (a == 1) "" else "s"}"
}

@Composable
private fun Pregunta(p: PreguntaComunidad, slug: String, enVivo: Boolean, alEnviar: () -> Unit) {
    when {
        p.estado == "oculta" -> PreguntaOculta(p)
        p.estado == "abierta" && enVivo -> PreguntaActiva(p, slug, alEnviar)
        else -> PreguntaCompacta(p)
    }
}

@Composable
private fun PreguntaOculta(p: PreguntaComunidad) {
    val borde = MaterialTheme.colorScheme.outline
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = borde,
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                )
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "Pregunta ${p.orden} · Se abre durante el evento",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PreguntaCompacta(p: PreguntaComunidad) {
    var abierto by remember { mutableStateOf(false) }
    val giro by animateFloatAsState(if (abierto) 180f else 0f, label = "flecha")
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { abierto = !abierto },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(32.dp).background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("P${p.orden}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.texto.orEmpty(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Cerrada · ${resumen(p)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (p.yaSuma) {
                Spacer(Modifier.width(6.dp))
                EtiquetaComunidad("Te suma", ColorExito)
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = if (abierto) "Ocultar respuestas" else "Ver respuestas",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(giro),
            )
        }
        AnimatedVisibility(abierto) {
            Box(Modifier.padding(top = 12.dp)) { Hilo(p, vivo = false) }
        }
    }
}

@Composable
private fun PreguntaActiva(p: PreguntaComunidad, slug: String, alEnviar: () -> Unit) {
    val azul = MaterialTheme.colorScheme.primary
    val forma = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(BorderStroke(2.dp, azul), forma),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EtiquetaComunidad("P${p.orden} · En vivo", azul, punto = true)
                Spacer(Modifier.weight(1f))
                Text(resumen(p), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(p.texto.orEmpty(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Box(Modifier.background(MaterialTheme.colorScheme.background).padding(12.dp)) { Hilo(p, vivo = true) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Redactor(p, slug, alEnviar)
    }
}

// ── Hilo ─────────────────────────────────────────────────────────────────────

/** El hilo de una pregunta, como un chat: lo de otros a la izquierda, lo mío a la derecha. */
@Composable
private fun Hilo(p: PreguntaComunidad, vivo: Boolean) {
    val hilo = p.hilo
    if (hilo.isEmpty()) {
        Text(
            if (vivo) "Aún nadie responde. ¡Sé la primera persona!" else "Nadie respondió esta pregunta.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        )
        return
    }

    val lista = rememberLazyListState(initialFirstVisibleItemIndex = hilo.lastIndex)
    // Si la persona está abajo siguiendo el chat o subió a leer algo viejo.
    var pegadoAbajo by remember { mutableStateOf(true) }
    LaunchedEffect(lista) {
        snapshotFlow { lista.canScrollForward }.collect { pegadoAbajo = !it }
    }
    // Llegan mensajes: si estaba abajo, o el nuevo es mío, se baja con ellos.
    val ultimo = hilo.last()
    LaunchedEffect(ultimo.id) {
        if (pegadoAbajo || ultimo.mia) lista.animateScrollToItem(hilo.lastIndex + if (p.totalHilo > hilo.size) 1 else 0)
    }

    // Lo nuevo o lo que cambió de estado se ilumina un momento. La primera carga no ilumina nada.
    var vistos by remember { mutableStateOf<Map<String, String>?>(null) }
    var destacados by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(hilo) {
        val ahora = hilo.associate { it.id to it.estado }
        val antes = vistos
        vistos = ahora
        if (antes == null) return@LaunchedEffect
        val cambiaron = hilo.filter { antes[it.id] != it.estado }.map { it.id }.toSet()
        if (cambiaron.isEmpty()) return@LaunchedEffect
        destacados = cambiaron
        delay(2_200)
        destacados = emptySet()
    }

    LazyColumn(
        state = lista,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .semantics { contentDescription = "Respuestas a la pregunta ${p.orden}" },
    ) {
        if (p.totalHilo > hilo.size) {
            item {
                Text(
                    "Mostrando las últimas ${hilo.size} de ${p.totalHilo}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        items(hilo, key = { it.id }) { m -> Burbuja(m, destacada = m.id in destacados) }
    }
}

@Composable
private fun Burbuja(m: MensajeHiloComunidad, destacada: Boolean) {
    val ajenaRechazada = m.estado == "rechazada" && !m.mia
    val azul = MaterialTheme.colorScheme.primary
    val anillo by animateColorAsState(if (destacada) azul.copy(alpha = 0.35f) else Color.Transparent, label = "destacado")
    val fondo = when {
        m.mia && m.estado == "rechazada" -> MaterialTheme.colorScheme.surfaceContainerHighest
        m.mia -> AzulMioComunidad
        ajenaRechazada -> Color.Transparent
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val colorTexto = when {
        m.mia && m.estado != "rechazada" -> Color.White
        ajenaRechazada -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    val forma = RoundedCornerShape(
        topStart = if (m.mia) 16.dp else 6.dp,
        topEnd = if (m.mia) 6.dp else 16.dp,
        bottomStart = 16.dp,
        bottomEnd = 16.dp,
    )
    val borde = MaterialTheme.colorScheme.outline

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (m.mia) Arrangement.End else Arrangement.Start,
    ) {
        if (!m.mia) {
            Box(
                Modifier.padding(top = 16.dp).size(28.dp).background(azul.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(m.nombre.take(1).uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = azul)
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(
            Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (m.mia) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                listOf(if (m.mia) "Tú" else m.nombre, FechasComunidad.hora(m.createdAt)).filter { it.isNotEmpty() }.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Text(
                if (ajenaRechazada) "Respuesta no aprobada por el moderador" else m.texto.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = if (ajenaRechazada) FontStyle.Italic else FontStyle.Normal,
                color = colorTexto,
                modifier = Modifier
                    .border(4.dp, anillo, forma)
                    .clip(forma)
                    .background(fondo)
                    .then(
                        if (ajenaRechazada) {
                            Modifier.drawBehind {
                                drawRoundRect(
                                    color = borde,
                                    cornerRadius = CornerRadius(16.dp.toPx()),
                                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                                )
                            }
                        } else Modifier,
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
            when (m.estado) {
                "aprobada" -> EtiquetaComunidad("Aprobada ✓", ColorExito)
                "rechazada" -> EtiquetaComunidad("No aprobada", MaterialTheme.colorScheme.error)
                else -> EtiquetaComunidad("En revisión", ColorAvisoComunidad)
            }
            if (m.mia && m.estado == "rechazada") {
                Text(
                    "No cuenta para el premio" + (m.motivo?.let { ": $it" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

// ── Redactor ─────────────────────────────────────────────────────────────────

/** Caja para responder. Solo se puede escribir: pegar, arrastrar o meter texto de golpe se bloquea. */
@Composable
private fun Redactor(p: PreguntaComunidad, slug: String, alEnviar: () -> Unit) {
    if (p.yaSuma) {
        Text(
            "Esta pregunta ya te suma 1 participación. No necesitas responderla otra vez.",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = ColorExito,
            modifier = Modifier.padding(16.dp),
        )
        return
    }
    if (p.enviosRestantes == 0) {
        Text(
            "Ya usaste todos tus envíos en esta pregunta.",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
        return
    }

    var texto by remember(p.id) { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var aviso by remember { mutableStateOf(false) }
    var marcaAviso by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val puedeEnviar = texto.trim().length >= 3 && !enviando

    LaunchedEffect(marcaAviso) {
        if (marcaAviso == 0) return@LaunchedEffect
        aviso = true
        delay(3_500)
        aviso = false
    }

    fun enviar() {
        val id = p.id ?: return
        if (!puedeEnviar) return
        enviando = true
        error = null
        scope.launch {
            try {
                ApiClient.service.responderComunidad(slug, id, ResponderComunidadInput(texto.trim())).datoOLanzar()
                texto = ""
                alEnviar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "No pudimos enviar tu respuesta"
            } finally {
                enviando = false
            }
        }
    }

    val restantes = p.enviosRestantes
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            CampoSinPegar(
                texto = texto,
                onCambio = { texto = it },
                placeholder = if (p.misEnvios > 0) "Responder de nuevo…" else "Escribe tu respuesta…",
                alIntentarPegar = { marcaAviso++ },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(18.dp))
                    .semantics { contentDescription = "Tu respuesta a la pregunta ${p.orden}" },
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = ::enviar,
                enabled = puedeEnviar,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (puedeEnviar || enviando) AzulMioComunidad else MaterialTheme.colorScheme.surfaceContainerHighest,
                        CircleShape,
                    ),
            ) {
                if (enviando) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar respuesta",
                        tint = if (puedeEnviar) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Row(Modifier.padding(horizontal = 4.dp)) {
            Text(
                if (aviso) "Aquí no se puede pegar: escribe con tus palabras."
                else "Solo escribir, sin pegar · $restantes envío${if (restantes == 1) "" else "s"} restante${if (restantes == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (aviso) FontWeight.SemiBold else FontWeight.Normal,
                color = if (aviso) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text("${texto.length}/$MAX_TEXTO_COMUNIDAD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        error?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                    .padding(10.dp),
            )
        }
    }
}
