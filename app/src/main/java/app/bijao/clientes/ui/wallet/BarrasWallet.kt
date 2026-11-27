package app.bijao.clientes.ui.wallet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.calculo.CalculadoraCuenta
import app.bijao.clientes.core.models.PeriodoResumen
import app.bijao.clientes.core.ui.ColorExito
import androidx.compose.ui.platform.LocalContext

/**
 * Barras de ingresos/gastos del Wallet con `Canvas`. Equivalente de
 * `BarrasWalletView.swift` (Swift Charts) y de `BarrasWallet.tsx` (Bklit).
 *
 * Gráfica interactiva: tocar o arrastrar sobre un periodo lo resalta y arriba
 * aparecen sus ingresos y gastos; las demás barras se atenúan. Las barras
 * crecen desde cero al aparecer o cambiar de periodo (quietas si las
 * animaciones del sistema están apagadas).
 */
@Composable
fun BarrasWallet(serie: List<PeriodoResumen>, etiqueta: (PeriodoResumen) -> String, modifier: Modifier = Modifier) {
    val maxValor = (serie.maxOfOrNull { maxOf(it.ingresos, it.gastos) } ?: 0).coerceAtLeast(1)
    val colorGasto = MaterialTheme.colorScheme.primary
    val haptico = LocalHapticFeedback.current
    // «Quitar animaciones» de Android = escala de animaciones en 0.
    val context = LocalContext.current
    val quieto = remember {
        android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    var seleccion by remember(serie) { mutableStateOf<Int?>(null) }
    val crecimiento = remember(serie) { Animatable(if (quieto) 1f else 0f) }
    LaunchedEffect(serie) {
        if (!quieto) crecimiento.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
    }

    fun elegir(x: Float, ancho: Int) {
        if (serie.isEmpty() || ancho <= 0) return
        val i = ((x / ancho) * serie.size).toInt().coerceIn(0, serie.size - 1)
        if (i != seleccion) {
            seleccion = i
            haptico.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Column(modifier = modifier) {
        val elegido = seleccion?.let { serie.getOrNull(it) }
        if (elegido != null) {
            Row(Modifier.padding(bottom = 8.dp)) {
                Text(etiqueta(elegido), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("↓ ${CalculadoraCuenta.formatoCOP(elegido.ingresos)}", style = MaterialTheme.typography.labelMedium,
                    color = ColorExito, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 14.dp))
                Text("↑ ${CalculadoraCuenta.formatoCOP(elegido.gastos)}", style = MaterialTheme.typography.labelMedium,
                    color = colorGasto, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 14.dp))
            }
        } else {
            Text("Toca una barra para ver el detalle", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .pointerInput(serie) {
                    detectTapGestures { elegir(it.x, size.width) }
                }
                .pointerInput(serie) {
                    detectDragGestures(onDragStart = { elegir(it.x, size.width) }) { cambio, _ -> elegir(cambio.position.x, size.width) }
                },
        ) {
            val n = serie.size
            if (n == 0) return@Canvas
            val anchoGrupo = size.width / n
            val anchoBarra = anchoGrupo * 0.3f
            val radio = CornerRadius(3f, 3f)
            val k = crecimiento.value

            serie.forEachIndexed { i, p ->
                val xGrupo = anchoGrupo * i
                val hIngreso = (p.ingresos.toFloat() / maxValor) * size.height * k
                val hGasto = (p.gastos.toFloat() / maxValor) * size.height * k
                val alfa = if (seleccion == null || seleccion == i) 1f else 0.3f

                drawRoundRect(
                    color = ColorExito.copy(alpha = alfa),
                    topLeft = Offset(xGrupo + anchoGrupo * 0.15f, size.height - hIngreso),
                    size = Size(anchoBarra, hIngreso),
                    cornerRadius = radio,
                )
                drawRoundRect(
                    color = colorGasto.copy(alpha = alfa),
                    topLeft = Offset(xGrupo + anchoGrupo * 0.55f, size.height - hGasto),
                    size = Size(anchoBarra, hGasto),
                    cornerRadius = radio,
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            // Con muchos puntos (14 días, 12 meses) se saltan etiquetas de por
            // medio -- si no, se amontonan y quedan ilegibles.
            serie.forEachIndexed { i, p ->
                Text(
                    if (serie.size <= 8 || i % 2 == 0) etiqueta(p) else "",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
