package app.bijao.clientes.ui.beneficios

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin

/** Una mota de confeti (medidas en dp): sale con velocidad (vx, vy) y la
 * gravedad la baja. */
data class PiezaConfeti(
    val color: Color, val ancho: Float, val plana: Boolean, val redonda: Boolean,
    val x: Float, val y: Float, val vx: Float, val vy: Float, val gravedad: Float, val giro: Float,
) {
    companion object {
        /** Pseudoaleatorio estable: las mismas motas en cada render, como la web. */
        private fun azar(i: Int, k: Int): Float {
            val v = sin(i * 12.9898 + k * 78.233) * 43758.5453
            return (v - floor(v)).toFloat()
        }

        private val COLORES = listOf(0xFFF2B42C, 0xFF007BFF, 0xFF00C853, 0xFFFF9F0A, 0xFF0D1A47, 0xFFEC4899).map { Color(it) }
        private val COLORES_EXPLOSION = listOf(0xFFF2C94C, 0xFF007BFF, 0xFF00C853, 0xFFFF9F0A, 0xFFFFFFFF, 0xFFEC4899).map { Color(it) }

        /** El chorro del voucher que se arranca: hacia arriba y a la derecha. */
        fun chorro(n: Int = 24) = (0 until n).map { i ->
            PiezaConfeti(
                COLORES[i % COLORES.size], round(6 + azar(i, 1) * 6), azar(i, 2) > 0.5f, azar(i, 3) > 0.6f,
                0f, round(20 + azar(i, 4) * 130), (azar(i, 5) - 0.35f) * 340 * 1.1f, (-120 - azar(i, 6) * 260) * 1.1f,
                620 * 1.21f, (azar(i, 7) - 0.5f) * 900,
            )
        }

        /** La explosión de la tarjeta completa: en todas las direcciones. */
        fun explosion(n: Int = 26) = (0 until n).map { i ->
            val angulo = azar(i, 4) * 2 * PI.toFloat()
            val v = 140 + azar(i, 5) * 220
            PiezaConfeti(
                COLORES_EXPLOSION[i % COLORES_EXPLOSION.size], round(6 + azar(i, 1) * 6), azar(i, 2) > 0.5f, azar(i, 3) > 0.6f,
                0f, 0f, cos(angulo) * v * 1.2f, sin(angulo) * v * 1.2f, 380 * 1.44f, (azar(i, 6) - 0.5f) * 900 * 1.2f,
            )
        }
    }
}

/** Las motas vuelan desde el origen del lienzo cuando `disparado` pasa a true.
 * El lienzo no recorta: las motas salen de él. */
@Composable
fun Confeti(piezas: List<PiezaConfeti>, disparado: Boolean, modifier: Modifier = Modifier, duracionMs: Int = 1100) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(disparado) {
        if (disparado) t.animateTo(1f, tween(duracionMs, easing = LinearEasing))
    }
    Canvas(modifier) {
        val p = t.value
        if (p <= 0f || p >= 1f) return@Canvas
        val opacidad = if (p < 0.65f) 1f else (1 - (p - 0.65f) / 0.35f).coerceIn(0f, 1f)
        piezas.forEach { m ->
            val ancho = m.ancho * density
            val alto = (if (m.plana) m.ancho else round(m.ancho * 0.45f)) * density
            val centro = Offset(
                (m.x + m.vx * p) * density,
                (m.y + m.vy * p + m.gravedad * p * p) * density,
            )
            rotate(m.giro * p, centro) {
                drawRoundRect(
                    color = m.color.copy(alpha = opacidad),
                    topLeft = Offset(centro.x - ancho / 2, centro.y - alto / 2),
                    size = Size(ancho, alto),
                    cornerRadius = CornerRadius(if (m.redonda) ancho / 2 else 2 * density),
                )
            }
        }
    }
}
