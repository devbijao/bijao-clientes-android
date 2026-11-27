package app.bijao.clientes.ui.beneficios

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** «Quitar animaciones» de Android (la escala de animaciones en 0): el
 * equivalente de «reducir movimiento» de iOS y de la web. */
@Composable
fun movimientoReducido(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

private val ANCHO_TALON = 104.dp

/**
 * El voucher que se arranca cuando el cajero escanea el QR del bono. Espejo
 * de `components/bonos/VoucherArrancado.tsx` y de `VoucherArrancadoView` en
 * iOS: entra volteándose, cae el sello «RECLAMADO», el desprendible se tensa,
 * se rasga con borde dentado y cae mientras salta el confeti; lo que queda se
 * corre a la izquierda. Cuando llega el cobro (`saldoFinal`), el saldo baja
 * contando.
 *
 * Con las animaciones del sistema apagadas se ve directamente el final.
 */
@Composable
fun VoucherArrancado(
    color: String?,
    etiqueta: String,
    negocio: String?,
    valor: String,
    saldoInicial: Double?,
    saldoFinal: Double?,
    montoUsado: Double?,
    onTerminado: () -> Unit,
) {
    val fondo = colorDeHex(color) ?: ColorBonoPorDefecto
    val talonFondo = fondo.conNegro(0.18f)
    val quieto = movimientoReducido()
    val alTerminar by rememberUpdatedState(onTerminado)

    val entrada = remember { Animatable(if (quieto) 1f else 0f) }
    val sello = remember { Animatable(if (quieto) 1f else 0f) }
    val tension = remember { Animatable(0f) }
    val vuelo = remember { Animatable(if (quieto) 2f else 0f) }   // 0 en su sitio, 1 arriba, 2 cayó
    val corrido = remember { Animatable(if (quieto) 1f else 0f) }
    val saldo = remember { Animatable((saldoInicial ?: 0.0).toFloat()) }
    var roto by remember { mutableStateOf(quieto) }
    var confeti by remember { mutableStateOf(false) }
    var listo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!quieto) {
            entrada.animateTo(1f, tween(320, easing = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f)))
            sello.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
            for (g in listOf(-3f, 3f, -4f, 4f, -2f, 0f)) tension.animateTo(g, tween(53))
            roto = true
            confeti = true
            val corre = async { corrido.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
            vuelo.animateTo(1f, tween(250, easing = LinearOutSlowInEasing))
            vuelo.animateTo(2f, tween(750, easing = CubicBezierEasing(0.45f, 0f, 0.7f, 0.4f)))
            corre.await()
            delay(100)
        }
        listo = true
        alTerminar()
    }
    // El saldo baja contando cuando ya se rasgó y llegó el cobro.
    LaunchedEffect(listo, saldoFinal) {
        if (listo && saldoFinal != null && saldoInicial != null) {
            if (quieto) saldo.snapTo(saldoFinal.toFloat())
            else saldo.animateTo(saldoFinal.toFloat(), tween(700, easing = FastOutSlowInEasing))
        }
    }

    val queda = saldoFinal != null && listo
    Box(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
            .semantics {
                contentDescription = "Bono reclamado en ${negocio ?: "el negocio"}" +
                    (montoUsado?.let { ": ${FormatoBonos.pesos(it)}" } ?: "")
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.widthIn(max = 358.dp).fillMaxWidth()) {
            Row(
                Modifier
                    .height(170.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val e = entrada.value
                        rotationY = -90f * (1 - e)
                        scaleX = 0.92f + 0.08f * e; scaleY = scaleX
                        alpha = e
                        cameraDistance = 12 * density
                    },
            ) {
                // Desprendible.
                Box(
                    Modifier
                        .width(ANCHO_TALON)
                        .fillMaxHeight()
                        .graphicsLayer {
                            val v = vuelo.value
                            transformOrigin = TransformOrigin(1f, 0f)
                            if (v <= 1f) {
                                translationX = -40.dp.toPx() * v
                                translationY = -70.dp.toPx() * v
                                rotationZ = tension.value + (-30f) * v
                                scaleX = 1f - 0.05f * v
                            } else {
                                val k = v - 1f
                                translationX = (-40f + 160f * k) * density
                                translationY = (-70f + 630f * k) * density
                                rotationZ = -30f - 290f * k
                                scaleX = 0.95f - 0.73f * k
                                alpha = if (k >= 0.9f) (1f - (k - 0.9f) / 0.1f).coerceIn(0f, 1f) else 1f
                            }
                            scaleY = scaleX
                        }
                        .clip(if (roto) BordeRasgado(derecho = true) else RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                        .background(talonFondo),
                ) {
                    Column(
                        Modifier.fillMaxHeight().padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("RECLAMADO EN", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                        Column {
                            Text(negocio ?: "Negocio", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                            Text(montoUsado?.let { FormatoBonos.pesos(it) } ?: "…", color = Color.White, fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        }
                        CodigoDeBarras(Color.White, Modifier.height(20.dp), listOf(2, 1, 3, 1, 2, 1, 1, 3, 2, 1, 2, 3))
                    }
                    Text(
                        "RECLAMADO", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp,
                        modifier = Modifier
                            .offset(x = 8.dp, y = 112.dp)
                            .graphicsLayer {
                                rotationZ = -16f
                                val s = 2.4f - 1.4f * sello.value
                                scaleX = s; scaleY = s
                                alpha = sello.value.coerceIn(0f, 1f)
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .background(talonFondo)
                            .border(3.dp, Color.White, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                // Cuerpo.
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer { translationX = -52.dp.toPx() * corrido.value }
                        .shadow(10.dp, if (roto) BordeRasgado(derecho = false) else RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp),
                            ambientColor = AzulSombra.copy(alpha = 0.18f), spotColor = AzulSombra.copy(alpha = 0.18f))
                        .clip(if (roto) BordeRasgado(derecho = false) else RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp))
                        .background(fondo),
                ) {
                    Column(
                        Modifier.fillMaxHeight().padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(etiqueta.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.4.sp, modifier = Modifier.padding(end = 40.dp))
                        Column {
                            Text(if (queda) "Te queda" else if (saldoInicial != null) "Saldo" else " ",
                                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (saldoInicial != null) FormatoBonos.pesos(saldo.value.toDouble()) else valor,
                                color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.2).sp,
                                maxLines = 1, lineHeight = 42.sp,
                            )
                        }
                        Text(if (queda) "Sigue en tu billetera" else "Aplicándose en caja", color = Color.White, fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                    Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = Color.White,
                        modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).size(32.dp))
                }
            }
            // Perforación y muescas, hasta que se rasga.
            if (!roto) {
                val fondoPagina = MaterialTheme.colorScheme.background
                Canvas(Modifier.width(ANCHO_TALON + 9.dp).height(170.dp).graphicsLayer { alpha = entrada.value }) {
                    val x = ANCHO_TALON.toPx()
                    drawLine(
                        Color.White.copy(alpha = 0.6f), Offset(x, 12.dp.toPx()), Offset(x, size.height - 12.dp.toPx()),
                        strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                    )
                    drawCircle(fondoPagina, 9.dp.toPx(), Offset(x, 0f))
                    drawCircle(fondoPagina, 9.dp.toPx(), Offset(x, size.height))
                }
            }
            Confeti(PiezaConfeti.chorro(), confeti, Modifier.offset(x = ANCHO_TALON).size(1.dp))
        }
    }
}

/** Zigzag en el lado derecho del desprendible o en el izquierdo del cuerpo. */
private class BordeRasgado(private val derecho: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val diente = 7 * density.density
        val n = 22
        val p = Path()
        if (derecho) {
            p.moveTo(0f, 0f)
            for (i in 0..n) p.lineTo(size.width - if (i % 2 == 1) diente else 0f, size.height * i / n)
            p.lineTo(0f, size.height)
        } else {
            p.moveTo(size.width, 0f)
            p.lineTo(size.width, size.height)
            for (i in n downTo 0) p.lineTo(if (i % 2 == 1) diente else 0f, size.height * i / n)
        }
        p.close()
        return Outline.Generic(p)
    }
}
