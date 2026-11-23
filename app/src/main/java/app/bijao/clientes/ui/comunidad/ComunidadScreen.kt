package app.bijao.clientes.ui.comunidad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Forum
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.bijao.clientes.core.models.MiEventoComunidad
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.ui.ColorExito
import app.bijao.clientes.ui.explorar.EstadoVacio
import coil.compose.AsyncImage
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Ámbar de «En revisión» (mismo de las tarjetas de Beneficios). */
val ColorAvisoComunidad = Color(0xFFF57C00)

/** Azul de las burbujas propias y del botón de enviar (mismo #006ee6 de la web). */
val AzulMioComunidad = Color(0xFF006EE6)

/**
 * Perfil › Comunidad: los eventos en vivo y aquellos en los que participé.
 * Espejo de `perfil/comunidad/page.tsx` en la web y de `ComunidadView.swift`
 * en iOS (D15 de MDs/plan_comunidad.md).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComunidadScreen(onAtras: () -> Unit, onEventoClick: (String) -> Unit) {
    var eventos by remember { mutableStateOf<List<MiEventoComunidad>?>(null) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ciclo = LocalLifecycleOwner.current.lifecycle

    suspend fun cargar() {
        try {
            eventos = ApiClient.service.misEventosComunidad().datoOLanzar()
            error = false
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            if (eventos == null) error = true
        }
    }

    // Igual que la web: se refresca cada 30 s mientras está a la vista.
    LaunchedEffect(Unit) {
        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                cargar()
                delay(30_000)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comunidad") },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a Perfil")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val lista = eventos
            when {
                lista != null && lista.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.Forum,
                    titulo = "No hay eventos por ahora",
                    descripcion = "Cuando asistas a una charla o taller con Bijao, escanea el QR del evento para participar y ganar premios.",
                )
                lista != null -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(lista, key = { it.slug }) { evento ->
                        FilaEvento(evento) { onEventoClick(evento.slug) }
                    }
                }
                error -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EstadoVacio(icono = Icons.Filled.WifiOff, titulo = "No pudimos cargar tus eventos", descripcion = "")
                    TextButton(onClick = { scope.launch { cargar() } }) { Text("Reintentar") }
                }
                else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun FilaEvento(evento: MiEventoComunidad, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            evento.imagenUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(80.dp).height(56.dp).clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (evento.estado == "en_vivo") {
                        EtiquetaComunidad("En vivo", MaterialTheme.colorScheme.error, punto = true)
                    } else {
                        EtiquetaComunidad("Terminado", MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (evento.cumple) EtiquetaComunidad("Premio ganado", ColorExito)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    evento.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitulo(evento),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun subtitulo(e: MiEventoComunidad): String = buildList {
    e.organizador?.takeIf { it.isNotBlank() }?.let(::add)
    add(FechasComunidad.corta(e.fechaInicio))
    if (e.participe) add("${minOf(e.aprobadas, e.minimoPremio)} de ${e.minimoPremio} aprobadas")
}.joinToString(" · ")

/** Pastilla de estado: fondo tenue del color y texto del color, así se lee
 * igual en claro y en oscuro. */
@Composable
fun EtiquetaComunidad(texto: String, color: Color, punto: Boolean = false) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (punto) {
            Box(Modifier.size(6.dp).background(color, CircleShape))
            Spacer(Modifier.width(5.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

/** Fechas en hora de Colombia, como la web (`timeZone: 'America/Bogota'`).
 * `OffsetDateTime` acepta el `+00:00` y los microsegundos del backend. */
object FechasComunidad {
    private val zona = ZoneId.of("America/Bogota")
    private val locale = Locale.forLanguageTag("es-CO")
    private val fCorta = DateTimeFormatter.ofPattern("d MMM, h:mm a", locale)
    private val fLarga = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a", locale)
    private val fHora = DateTimeFormatter.ofPattern("h:mm a", locale)

    private fun formatear(iso: String?, f: DateTimeFormatter): String = try {
        if (iso == null) "" else OffsetDateTime.parse(iso).atZoneSameInstant(zona).format(f)
    } catch (e: Exception) {
        ""
    }

    fun corta(iso: String?) = formatear(iso, fCorta)
    fun larga(iso: String?) = formatear(iso, fLarga)
    fun hora(iso: String?) = formatear(iso, fHora)
}
