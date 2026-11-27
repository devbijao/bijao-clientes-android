package app.bijao.clientes.ui.beneficios

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bijao.clientes.core.models.Tarjeta
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val MAX_SELLOS = 20
private val GIROS = listOf(-8f, 5f, -3f, 9f, -6f, 4f, -10f, 7f, -4f, 6f)
private val LADO = 34.dp

private enum class EstadoTarjeta { AGOTADA, COMPLETA, FALTA_UNO, MITAD, PROGRESO }

private fun estadoDe(t: Tarjeta) = when {
    t.agotada -> EstadoTarjeta.AGOTADA
    t.saldo >= t.meta -> EstadoTarjeta.COMPLETA
    t.faltan == 1 -> EstadoTarjeta.FALTA_UNO
    t.meta > 0 && t.saldo * 2 >= t.meta -> EstadoTarjeta.MITAD
    else -> EstadoTarjeta.PROGRESO
}

/** Gris del mismo brillo: la tarjeta agotada se ve «apagada» como en la web
 * (que usa `grayscale`), sin depender de RenderEffect (API 31+). */
private fun Color.enGris(): Color {
    val l = 0.299f * red + 0.587f * green + 0.114f * blue
    return Color(l * 0.92f, l * 0.92f, l * 0.92f, alpha)
}

/**
 * La tarjeta de sellos, la unidad visual de la app. Espejo de
 * `TarjetaSellos.tsx` (F5 de `MDs/plan_bonos.md`) y de `TarjetaSellosView`
 * en iOS: el color y el ícono los elige el negocio, cada sello lleva ese
 * ícono y el último espacio es el premio.
 *
 * Ya no hay botón «Canjear»: al completarla el premio llega solo a Bonos y la
 * tarjeta vuelve a empezar. Si el negocio se quedó sin premios (`agotada`),
 * queda llena, en gris y con telarañas, con una nota debajo.
 *
 * @param animarSellos cuántos de los últimos sellos entran cayendo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TarjetaSellos(
    tarjeta: Tarjeta,
    modifier: Modifier = Modifier,
    logoUrl: String? = null,
    animarSellos: Int = 0,
) {
    val estado = estadoDe(tarjeta)
    val agotada = estado == EstadoTarjeta.AGOTADA
    val base = colorDeHex(tarjeta.color) ?: ColorTarjetaPorDefecto
    val color = if (agotada) base.enGris() else base
    val amarillo = if (agotada) AmarilloPremio.enGris() else AmarilloPremio
    val icono = iconoSello(tarjeta.icono)
    val unoAUno = tarjeta.meta in 1..MAX_SELLOS
    val forma = RoundedCornerShape(22.dp)

    // Sellos que caen: uno por cada sello ganado desde la última visita.
    val hasta = minOf(tarjeta.saldo, tarjeta.meta)
    val desde = if (unoAUno && animarSellos > 0) maxOf(0, hasta - animarSellos) else hasta
    val caidas = remember(tarjeta.recompensaId, animarSellos) {
        (0 until tarjeta.meta).associateWith { i -> Animatable(if (i in desde until hasta) 0f else 1f) }
    }
    val golpe = remember { Animatable(0f) }
    LaunchedEffect(tarjeta.recompensaId, animarSellos) {
        if (desde >= hasta) return@LaunchedEffect
        delay(350)
        for (i in desde until hasta) {
            caidas[i]?.animateTo(1f, tween(450, easing = CubicBezierEasing(0.55f, 0f, 0.9f, 0.5f)))
            launch {
                golpe.animateTo(1f, tween(90))
                golpe.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            delay(150)
        }
    }

    val nota = if (!tarjeta.aplicaCualquierCompra && !agotada) {
        tarjeta.sumaCon?.takeIf { it.isNotEmpty() }?.let { "Suma con: ${FormatoBonos.enumerar(it)}" }
            ?: "Solo suma con productos concretos. Pregunta en el negocio cuáles."
    } else null

    Column(modifier) {
        Box(
            Modifier
                .graphicsLayer {
                    translationY = golpe.value * 5.dp.toPx()
                    rotationZ = golpe.value * 0.8f
                }
                .shadow(12.dp, forma, ambientColor = color.copy(alpha = 0.35f), spotColor = color.copy(alpha = if (agotada) 0.15f else 0.35f))
                .clip(forma)
                .background(Brush.linearGradient(listOf(color, color.conNegro(0.3f)))),
        ) {
            Column {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Encabezado.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            val inicial = (tarjeta.negocio.nombre ?: "B").trim().take(1).uppercase()
                            Text(inicial, color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            if (logoUrl != null) {
                                AsyncImage(model = logoUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                            }
                        }
                        Column(Modifier.weight(1f).padding(start = 10.dp, end = 4.dp)) {
                            Text(tarjeta.negocio.nombre ?: "Negocio", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(tarjeta.nombre ?: "Tarjeta de sellos", color = Color.White, fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold, lineHeight = 18.sp)
                        }
                        Insignia(estado)
                    }

                    if (unoAUno) {
                        FlowRow(
                            modifier = Modifier.widthIn(max = LADO * 5 + 10.dp * 4)
                                .semantics { contentDescription = "$hasta de ${tarjeta.meta} sellos" },
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = 5,
                        ) {
                            for (i in 0 until tarjeta.meta) {
                                Sello(
                                    i = i, meta = tarjeta.meta, lleno = i < tarjeta.saldo, color = color, amarillo = amarillo,
                                    icono = icono, progreso = caidas[i]?.value ?: 1f,
                                )
                            }
                        }
                    } else {
                        val pct = if (tarjeta.meta > 0) (tarjeta.saldo.toFloat() / tarjeta.meta).coerceIn(0f, 1f) else 0f
                        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f))) {
                            Box(Modifier.fillMaxWidth(pct).height(8.dp).clip(CircleShape).background(Color.White))
                        }
                    }

                    if (nota != null) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.size(13.dp).offset(y = 1.dp))
                            Text(nota, color = Color.White.copy(alpha = 0.92f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                lineHeight = 14.sp, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }

                // Pie: cuántos van y qué se gana.
                Row(
                    Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.2f)).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text("$hasta de ${tarjeta.meta} sellos", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.weight(1f).padding(start = 12.dp), horizontalArrangement = Arrangement.End) {
                        Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = amarillo, modifier = Modifier.size(14.dp).offset(y = 1.dp))
                        Text(tarjeta.textoPremio, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End, lineHeight = 15.sp, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
            if (agotada) Telaranas(Modifier.matchParentSize())
        }

        if (agotada) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .offset(y = (-14).dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(Color(0xFFEEF0F4))
                    .border(1.dp, Color(0xFFDCDFE6), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .padding(start = 14.dp, end = 14.dp, top = 22.dp, bottom = 12.dp),
            ) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Color(0xFF4A5060), modifier = Modifier.size(18.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Se agotaron los premios. ") }
                        append("Tus ${tarjeta.saldo} sellos quedan guardados: cuando ${tarjeta.negocio.nombre ?: "el negocio"} reponga premios, el tuyo llega solo a Bonos.")
                    },
                    color = Color(0xFF353A46), fontSize = 12.5.sp, lineHeight = 18.sp, modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun Insignia(estado: EstadoTarjeta) {
    val (texto, fondo, tinta) = when (estado) {
        EstadoTarjeta.COMPLETA -> Triple("¡Completa!", Color(0xFF00C853), Color.White)
        EstadoTarjeta.FALTA_UNO -> Triple("¡Te falta 1!", Color(0xFFFF9F0A), Color(0xFF1D1A12))
        EstadoTarjeta.MITAD -> Triple("Vas a la mitad", Color.White.copy(alpha = 0.2f), Color.White)
        EstadoTarjeta.AGOTADA -> Triple("Agotada", Color.Black.copy(alpha = 0.45f), Color.White)
        EstadoTarjeta.PROGRESO -> return
    }
    Text(
        texto, color = tinta, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(fondo).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Un espacio de la tarjeta. `progreso` 0→1 es la caída del sello (1 = en su sitio). */
@Composable
private fun Sello(
    i: Int, meta: Int, lleno: Boolean, color: Color, amarillo: Color,
    icono: androidx.compose.ui.graphics.vector.ImageVector, progreso: Float,
) {
    val esPremio = i == meta - 1
    val giro = if (esPremio) 0f else GIROS[i % GIROS.size]
    Box(Modifier.size(LADO), contentAlignment = Alignment.Center) {
        // El hueco siempre está; el sello lleno cae encima.
        Canvas(Modifier.matchParentSize()) {
            val trazo = 2.dp.toPx()
            if (esPremio) drawCircle(amarillo.copy(alpha = 0.12f))
            drawCircle(
                color = if (esPremio) amarillo else Color.White.copy(alpha = 0.45f),
                radius = size.minDimension / 2 - trazo / 2,
                style = Stroke(trazo, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
            )
        }
        if (esPremio && !lleno) {
            Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = amarillo, modifier = Modifier.size(17.dp))
        }
        if (lleno) {
            val densidad = LocalDensity.current
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val p = progreso
                        translationY = -150.dp.toPx() * (1 - p)
                        val escala = 3f - 2f * p
                        scaleX = escala; scaleY = escala
                        rotationZ = -40f + (giro + 40f) * p
                        alpha = if (p <= 0f) 0f else 1f
                    }
                    .clip(CircleShape)
                    .background(if (esPremio) amarillo else Color.White)
                    .then(if (esPremio) Modifier.border(2.dp, Color.White, CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (esPremio) Icons.Filled.CardGiftcard else icono, contentDescription = null, tint = color,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

/** Las telarañas de la tarjeta agotada: una grande a la derecha y otra chica a
 * la izquierda, por encima de la franja del pie. Trazos de la web. */
@Composable
private fun Telaranas(modifier: Modifier) {
    Canvas(modifier) {
        val pie = 38.dp.toPx()
        val grande = 120.dp.toPx()
        val sg = grande / 120f
        val ox = size.width - grande
        val oy = size.height - pie - grande
        fun g(x: Float, y: Float) = Offset(ox + x * sg, oy + (120f - y) * sg)
        val trazoG = Path().apply {
            listOf(20f to 100f, 50f to 118f, 2f to 62f, 88f to 120f, 0f to 26f).forEach { (x, y) ->
                moveTo(g(120f, 0f).x, g(120f, 0f).y); lineTo(g(x, y).x, g(x, y).y)
            }
            listOf(
                floatArrayOf(100f, 2f, 104f, 14f, 113f, 18f), floatArrayOf(80f, 2f, 88f, 28f, 120f, 40f),
                floatArrayOf(58f, 2f, 70f, 46f, 120f, 64f), floatArrayOf(36f, 2f, 54f, 66f, 120f, 90f),
                floatArrayOf(14f, 2f, 38f, 86f, 120f, 114f),
            ).forEach { c ->
                moveTo(g(c[0], c[1]).x, g(c[0], c[1]).y)
                quadraticTo(g(c[2], c[3]).x, g(c[2], c[3]).y, g(c[4], c[5]).x, g(c[4], c[5]).y)
            }
            moveTo(g(96f, 44f).x, g(96f, 44f).y); lineTo(g(96f, 70f).x, g(96f, 70f).y)
        }
        drawPath(trazoG, Color.White.copy(alpha = 0.55f), style = Stroke(1.dp.toPx()))
        drawCircle(Color.White.copy(alpha = 0.6f), 3.dp.toPx(), g(96f, 73f))

        val chica = 64.dp.toPx()
        val sc = chica / 70f
        val cy0 = size.height - 40.dp.toPx() - chica
        fun c(x: Float, y: Float) = Offset(x * sc, cy0 + y * sc)
        val trazoC = Path().apply {
            listOf(60f to 0f, 70f to 30f, 28f to 0f, 70f to 62f).forEach { (x, y) ->
                moveTo(c(0f, 70f).x, c(0f, 70f).y); lineTo(c(x, y).x, c(x, y).y)
            }
            listOf(
                floatArrayOf(10f, 58f, 16f, 64f, 20f, 70f), floatArrayOf(18f, 46f, 28f, 58f, 34f, 70f),
                floatArrayOf(26f, 34f, 40f, 50f, 48f, 70f),
            ).forEach { q ->
                moveTo(c(q[0], q[1]).x, c(q[0], q[1]).y)
                quadraticTo(c(q[2], q[3]).x, c(q[2], q[3]).y, c(q[4], q[5]).x, c(q[4], q[5]).y)
            }
        }
        drawPath(trazoC, Color.White.copy(alpha = 0.45f), style = Stroke(1.dp.toPx()))
    }
}
