package app.bijao.clientes.ui.wallet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.PeriodoResumen
import app.bijao.clientes.core.ui.ColorExito

/**
 * Barras de ingresos/gastos del Wallet con `Canvas` -- no hay librería de
 * gráficas en esta app todavía, y una sola barra agrupada no la justifica.
 * Equivalente de `BarrasWalletView.swift` (Swift Charts) y de
 * `BarrasWallet.tsx` (Bklit) en la web.
 */
@Composable
fun BarrasWallet(serie: List<PeriodoResumen>, etiqueta: (PeriodoResumen) -> String, modifier: Modifier = Modifier) {
    val maxValor = (serie.maxOfOrNull { maxOf(it.ingresos, it.gastos) } ?: 0).coerceAtLeast(1)
    val colorGasto = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val n = serie.size
            if (n == 0) return@Canvas
            val anchoGrupo = size.width / n
            val anchoBarra = anchoGrupo * 0.3f
            val radio = CornerRadius(3f, 3f)

            serie.forEachIndexed { i, p ->
                val xGrupo = anchoGrupo * i
                val hIngreso = (p.ingresos.toFloat() / maxValor) * size.height
                val hGasto = (p.gastos.toFloat() / maxValor) * size.height

                drawRoundRect(
                    color = ColorExito,
                    topLeft = Offset(xGrupo + anchoGrupo * 0.15f, size.height - hIngreso),
                    size = Size(anchoBarra, hIngreso),
                    cornerRadius = radio,
                )
                drawRoundRect(
                    color = colorGasto,
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
