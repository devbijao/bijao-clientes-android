package app.bijao.clientes.ui.beneficios

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bijao.clientes.core.models.Bono

private val BARRAS = listOf(2, 1, 3, 1, 2, 1, 1, 3, 2, 1, 2, 3, 1, 2)
private val ANCHO_TALON = 92.dp

/**
 * El voucher de un bono: desprendible a la izquierda (texto y código de
 * barras), línea perforada con muescas y, a la derecha, el valor en grande con
 * el ícono de regalo. Espejo de `components/bonos/Voucher.tsx` y de
 * `VoucherView.swift`.
 *
 * Las muescas son un recorte de verdad ([FormaVoucher]), no círculos del color
 * del fondo: se ve lo que haya detrás, en modo claro y oscuro.
 */
@Composable
fun Voucher(
    color: String?,
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    donde: String? = null,
    desprendible: String? = null,
    codigo: String? = null,
) {
    val fondo = colorDeHex(color) ?: ColorBonoPorDefecto
    val tinta = tintaSobre(color)
    val forma = FormaVoucher(ANCHO_TALON + 1.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .height(IntrinsicSize.Min)
            .shadow(10.dp, forma, ambientColor = AzulSombra.copy(alpha = 0.16f), spotColor = AzulSombra.copy(alpha = 0.16f))
            .clip(forma)
            .background(fondo)
            .semantics { contentDescription = listOfNotNull(etiqueta, valor, detalle, donde).joinToString(", ") },
    ) {
        // Desprendible.
        Column(
            modifier = Modifier.width(ANCHO_TALON).fillMaxHeight().padding(start = 12.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(desprendible ?: "", color = tinta, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, lineHeight = 13.sp)
            Column {
                CodigoDeBarras(tinta, Modifier.height(26.dp))
                if (codigo != null) {
                    Text(codigo, color = tinta, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp,
                        maxLines = 1, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        // Perforación.
        Canvas(Modifier.width(2.dp).fillMaxHeight().padding(vertical = 12.dp)) {
            drawLine(
                color = tinta.copy(alpha = 0.55f),
                start = Offset(size.width / 2, 0f), end = Offset(size.width / 2, size.height),
                strokeWidth = size.width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
            )
        }
        // Cuerpo.
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Column(
                modifier = Modifier.fillMaxHeight().padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(etiqueta.uppercase(), color = tinta, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp, modifier = Modifier.padding(end = 40.dp))
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text(valor, color = tinta, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, lineHeight = 36.sp)
                    if (detalle != null) {
                        Text(detalle, color = tinta, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
                Text(donde ?: "", color = tinta, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp)
            }
            Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = tinta,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp).size(32.dp))
        }
    }
}

/** Un voucher para la billetera, a partir del bono. */
@Composable
fun Voucher(bono: Bono, modifier: Modifier = Modifier, desprendible: String? = null) {
    val (valor, detalle) = FormatoBonos.textos(bono)
    Voucher(
        color = bono.color, etiqueta = bono.etiqueta, valor = valor, detalle = detalle, donde = bono.donde,
        desprendible = desprendible ?: bono.desprendible, codigo = bono.codigo, modifier = modifier,
    )
}

@Composable
internal fun CodigoDeBarras(tinta: Color, modifier: Modifier = Modifier, barras: List<Int> = BARRAS) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        barras.forEach { ancho -> Spacer(Modifier.width(ancho.dp).fillMaxHeight().background(tinta)) }
    }
}

/** Rectángulo redondeado con dos muescas en la línea perforada. */
class FormaVoucher(
    private val xMuesca: androidx.compose.ui.unit.Dp,
    private val radioMuesca: androidx.compose.ui.unit.Dp = 9.dp,
    private val radioEsquina: androidx.compose.ui.unit.Dp = 18.dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        with(density) {
            val esquina = radioEsquina.toPx()
            val base = Path().apply {
                addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(esquina, esquina)))
            }
            val r = radioMuesca.toPx()
            val x = xMuesca.toPx()
            val muescas = Path().apply {
                addOval(Rect(Offset(x, 0f), r))
                addOval(Rect(Offset(x, size.height), r))
            }
            return Outline.Generic(Path().apply { op(base, muescas, PathOperation.Difference) })
        }
    }
}
