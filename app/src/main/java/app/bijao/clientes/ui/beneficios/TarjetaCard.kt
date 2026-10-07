package app.bijao.clientes.ui.beneficios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.Tarjeta
import coil.compose.AsyncImage

private enum class EstadoTarjeta { AGOTADA, PUEDE_CANJEAR, FALTA_UNO, MITAD, PROGRESO }

private fun estadoDe(tarjeta: Tarjeta): EstadoTarjeta = when {
    tarjeta.agotada -> EstadoTarjeta.AGOTADA
    tarjeta.puedeCanjear -> EstadoTarjeta.PUEDE_CANJEAR
    tarjeta.faltan == 1 -> EstadoTarjeta.FALTA_UNO
    tarjeta.meta > 0 && tarjeta.saldo * 2 >= tarjeta.meta -> EstadoTarjeta.MITAD
    else -> EstadoTarjeta.PROGRESO
}

private fun colorAcento(estado: EstadoTarjeta): Color = when (estado) {
    EstadoTarjeta.AGOTADA -> Color(0xFF9E9E9E)
    EstadoTarjeta.PUEDE_CANJEAR -> Color(0xFF2E7D32)
    EstadoTarjeta.FALTA_UNO -> Color(0xFFF57C00)
    EstadoTarjeta.MITAD, EstadoTarjeta.PROGRESO -> Color(0xFF007BFF)
}

/**
 * Tarjeta de fidelización estilo carné -- mismo lenguaje visual al que llegó
 * iOS tras varias rondas (degradado por estado, sellos con profundidad,
 * recuadro de logo + franja inferior de datos), construido de entrada acá
 * porque Compose ya trae los bloques (`Brush`, `shadow` con color) sin tener
 * que redescubrirlos. Sin la inclinación 3D al arrastrar ni el reflejo en
 * bucle de iOS -- esos son pulido de interacción, no lenguaje visual, y
 * quedan para la Fase A8 si vale la pena traerlos.
 */
@Composable
fun TarjetaCard(tarjeta: Tarjeta, canjeando: Boolean, logoUrl: String?, onCanjear: () -> Unit) {
    val estado = estadoDe(tarjeta)
    val acento = colorAcento(estado)
    val oscuro = if (estado == EstadoTarjeta.AGOTADA) Color(0xFF333333) else Color(0xFF0D1A48)
    val fondo = Brush.linearGradient(listOf(acento, acento.copy(alpha = 0.75f), oscuro))
    val puedeMostrarSellos = tarjeta.meta in 1..12

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(22.dp), ambientColor = acento, spotColor = acento)
            .clip(RoundedCornerShape(22.dp))
            .background(fondo),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                RecuadroLogo(logoUrl = logoUrl, inicial = (tarjeta.negocio.nombre ?: "B").take(1).uppercase())
                Spacer(Modifier.padding(start = 12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tarjeta.negocio.nombre ?: "Negocio",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                    Text(
                        tarjeta.nombre ?: "Tarjeta de sellos",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    tarjeta.premioTexto?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                    }
                }
                Insignia(estado)
            }

            Spacer(Modifier.padding(top = 14.dp))

            if (puedeMostrarSellos) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    repeat(maxOf(tarjeta.meta, 1)) { indice -> SelloOrbe(lleno = indice < tarjeta.saldo, acento = acento) }
                }
            } else {
                LinearProgressIndicator(
                    progress = { tarjeta.saldo.toFloat() / maxOf(tarjeta.meta, 1).toFloat() },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.18f)).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${tarjeta.saldo}/${tarjeta.meta} sellos",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f),
            )
            when {
                tarjeta.puedeCanjear -> Button(
                    onClick = onCanjear,
                    enabled = !canjeando,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF2E7D32)),
                ) {
                    if (canjeando) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF2E7D32))
                    else Text("Canjear", style = MaterialTheme.typography.labelMedium)
                }
                !tarjeta.agotada -> Text(
                    "Faltan ${tarjeta.faltan}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun RecuadroLogo(logoUrl: String?, inicial: String) {
    Box(
        modifier = Modifier
            .size(48.dp, 60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        if (logoUrl != null) {
            AsyncImage(model = logoUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth())
        } else {
            Text(inicial, style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
    }
}

@Composable
private fun Insignia(estado: EstadoTarjeta) {
    when (estado) {
        EstadoTarjeta.AGOTADA -> Text("Agotada", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
        EstadoTarjeta.PUEDE_CANJEAR -> Box(
            modifier = Modifier.background(Color.White, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
        ) { Text("¡Lista!", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32)) }
        EstadoTarjeta.FALTA_UNO -> Box(
            modifier = Modifier.background(Color(0xFFF57C00), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
        ) { Text("¡Te falta 1!", style = MaterialTheme.typography.labelSmall, color = Color.White) }
        EstadoTarjeta.MITAD -> Text("Vas a la mitad", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
        EstadoTarjeta.PROGRESO -> {}
    }
}

/** Esfera con profundidad (degradado radial + punto especular) en vez de un
 * círculo plano -- simula un botón de vidrio incrustado. */
@Composable
private fun SelloOrbe(lleno: Boolean, acento: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = if (lleno) listOf(Color.White, Color.White.copy(alpha = 0.88f), acento.copy(alpha = 0.45f))
                    else listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.05f), Color.Transparent),
                ),
            ),
    ) {
        if (lleno) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .align(Alignment.TopStart)
                    .padding(start = 3.dp, top = 3.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f)),
            )
        }
    }
}
