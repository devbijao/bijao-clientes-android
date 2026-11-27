package app.bijao.clientes.ui.beneficios

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.bijao.clientes.core.models.Bono
import app.bijao.clientes.core.models.Tarjeta
import kotlinx.coroutines.delay

/**
 * La tarjeta se completa y el premio llega a Bonos. Espejo de
 * `CeremoniaPremio.tsx` y de `CeremoniaPremioView` en iOS; se muestra una vez
 * por premio, la primera vez que la persona abre Tarjetas después de ganarlo:
 *
 *   1. cae el último sello (el regalo) y salta confeti;
 *   2. la tarjeta se voltea y del otro lado está el voucher;
 *   3. el voucher vuela hacia las pestañas;
 *   4. sube una hoja con el premio y el botón para verlo.
 *
 * Con las animaciones del sistema apagadas va directo a la hoja.
 */
@Composable
fun CeremoniaPremio(
    tarjeta: Tarjeta,
    bono: Bono,
    logoUrl: String?,
    onVer: () -> Unit,
    onCerrar: () -> Unit,
) {
    val quieto = movimientoReducido()
    var confeti by remember { mutableStateOf(false) }
    var hoja by remember { mutableStateOf(quieto) }
    val volteo = remember { Animatable(0f) }
    val vuelo = remember { Animatable(0f) }
    val subida = remember { Animatable(if (quieto) 1f else 0f) }
    val llena = remember(tarjeta) { tarjeta.copy(saldo = tarjeta.meta, faltan = 0, agotada = false) }

    LaunchedEffect(Unit) {
        if (quieto) return@LaunchedEffect
        delay(1_050)
        confeti = true
        delay(1_050)
        volteo.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
        delay(750)
        vuelo.animateTo(1f, tween(700, easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)))
        delay(200)
        hoja = true
        subida.animateTo(1f, tween(450, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
    }

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(AzulSombra.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (hoja) onCerrar() },
        ) {
            if (!hoja) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 140.dp, start = 16.dp, end = 16.dp).widthIn(max = 358.dp).fillMaxWidth()) {
                    TarjetaSellos(
                        llena, logoUrl = logoUrl, animarSellos = 1,
                        modifier = Modifier.graphicsLayer {
                            rotationY = 90f * volteo.value
                            alpha = if (volteo.value < 0.5f) 1f else 0f
                            cameraDistance = 12 * density
                        },
                    )
                    Voucher(
                        bono, desprendible = "Completaste ${tarjeta.nombre ?: "la tarjeta"}.",
                        modifier = Modifier.graphicsLayer {
                            rotationY = -90f * (1 - volteo.value)
                            alpha = if (volteo.value >= 0.5f) 1f - 0.4f * vuelo.value else 0f
                            val s = 1f - 0.9f * vuelo.value
                            scaleX = s; scaleY = s
                            translationY = -420.dp.toPx() * vuelo.value
                            cameraDistance = 12 * density
                        },
                    )
                    Confeti(PiezaConfeti.explosion(), confeti, Modifier.align(Alignment.Center).size(1.dp), duracionMs = 1200)
                }
            } else {
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(12.dp)
                        .graphicsLayer { translationY = 400.dp.toPx() * (1 - subida.value) }
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).background(Color(0xFFFFF8E6), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(26.dp))
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("¡Ganaste ${tarjeta.premio ?: bono.nombre ?: "tu premio"}!", fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold, lineHeight = 23.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Ya está en Bonos. Úsalo en ${tarjeta.negocio.nombre ?: "el negocio"} antes del ${FormatoBonos.dia(bono.venceEn)}.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onCerrar) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onVer, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) {
                        Text("Ver mi premio en Bonos", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
