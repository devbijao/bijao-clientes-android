package app.bijao.clientes.ui.beneficios

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.ui.explorar.EstadoVacio
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val AZUL_BIJAO_QR = Color.rgb(0, 123, 255)

/**
 * QR de identidad que se muestra en caja -- equivalente de `CodigoQRView.swift`
 * + `GeneradorQR.swift`. `POST /api/app/qr` entrega un token rotatorio de un
 * solo uso (TTL corto); se renueva solo al expirar. Único contenido (sin
 * TopAppBar): Inicio (Fase A7) lo usa directo con su propio encabezado; ya no
 * hay un wrapper con su propia pantalla -- el atajo de Beneficios se quitó
 * cuando Inicio empezó a cubrir esto, igual que en iOS.
 */
@Composable
fun CodigoQR(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var tokenTexto by remember { mutableStateOf<String?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var segundosRestantes by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var reintentando by remember { mutableStateOf(false) }

    suspend fun generar() {
        error = null
        reintentando = true
        try {
            val qr = ApiClient.service.generarQR().datoOLanzar().data
            bitmap = withContext(Dispatchers.Default) { generarBitmapQR(qr.token, 600) }
            segundosRestantes = qr.ttlSegundos
            tokenTexto = qr.token
        } catch (e: Exception) {
            error = e.message ?: "No pudimos generar tu código."
        } finally {
            reintentando = false
        }
    }

    LaunchedEffect(Unit) { generar() }

    // Se vuelve a armar solo cuando cambia el token -- es decir, cada vez
    // que `generar()` entrega uno nuevo, lo que también cubre el primer
    // disparo y cada renovación siguiente sin lógica aparte.
    LaunchedEffect(tokenTexto) {
        if (tokenTexto == null) return@LaunchedEffect
        while (segundosRestantes > 0) {
            delay(1000)
            segundosRestantes -= 1
        }
        generar()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            when {
                bitmap != null -> Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Código QR",
                    modifier = Modifier.size(200.dp),
                )
                error != null -> EstadoVacio(
                    icono = Icons.Filled.QrCode,
                    titulo = "¡Ups! No pudimos generar tu código",
                    descripcion = "Inténtalo de nuevo en un momento.",
                )
                else -> CircularProgressIndicator()
            }
        }

        if (error != null) {
            Button(onClick = { scope.launch { generar() } }, enabled = !reintentando) {
                if (reintentando) CircularProgressIndicator(modifier = Modifier.size(16.dp)) else Text("Cargar de nuevo")
            }
        }

        tokenTexto?.let { token ->
            Text(
                if (segundosRestantes > 0) "Se renueva en ${segundosRestantes}s" else "Renovando…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "O dicta este código",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                formatearParaDictar(token),
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
            )
        }
    }
}

private fun formatearParaDictar(token: String): String {
    val mitad = token.length / 2
    return "${token.take(mitad)} ${token.drop(mitad)}"
}

/**
 * QR con la identidad visual de Bijao -- azul de marca en los módulos en vez
 * de negro, módulos redondeados, con una marca en el centro. Equivalente de
 * `GeneradorQR.swift` (EFQRCode en iOS, Swift-only, sin versión para
 * Android); acá se arma a mano sobre `Encoder` de ZXing -- la API pública de
 * alto nivel (`QRCodeWriter.encode`) ya entrega la matriz escalada a píxeles,
 * sin acceso a los módulos individuales que hacen falta para redondearlos.
 *
 * Nivel de corrección `H` (el más alto) a propósito: al tapar el centro con
 * una marca, un nivel más bajo puede volverlo ilegible para el escáner de caja.
 *
 * Sin el logo real de iOS ("LogoQR") todavía -- cae a un monograma con la
 * inicial, mismo patrón de `TarjetaCard.RecuadroLogo` para cuando no hay un
 * asset de marca disponible.
 */
private fun generarBitmapQR(texto: String, tamano: Int): Bitmap {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 1,
    )
    val qrCode = Encoder.encode(texto, ErrorCorrectionLevel.H, hints)
    val matriz = qrCode.matrix ?: error("No se pudo generar la matriz del QR")
    val numModulos = matriz.width
    val anchoModulo = tamano.toFloat() / numModulos

    val bitmap = Bitmap.createBitmap(tamano, tamano, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)

    val paintModulo = Paint().apply { color = AZUL_BIJAO_QR; isAntiAlias = true }
    val radioModulo = anchoModulo * 0.28f
    for (y in 0 until numModulos) {
        for (x in 0 until numModulos) {
            if (matriz.get(x, y).toInt() == 1) {
                val left = x * anchoModulo
                val top = y * anchoModulo
                canvas.drawRoundRect(
                    RectF(left, top, left + anchoModulo, top + anchoModulo),
                    radioModulo, radioModulo, paintModulo,
                )
            }
        }
    }

    // Monograma central con fondo blanco -- tapa los módulos de en medio,
    // por eso la corrección H de arriba.
    val ladoLogo = tamano * 0.2f
    val cx = tamano / 2f
    val cy = tamano / 2f
    val rectLogo = RectF(cx - ladoLogo / 2, cy - ladoLogo / 2, cx + ladoLogo / 2, cy + ladoLogo / 2)
    canvas.drawRoundRect(rectLogo, ladoLogo * 0.22f, ladoLogo * 0.22f, Paint().apply { color = Color.WHITE; isAntiAlias = true })

    val paintTexto = Paint().apply {
        color = AZUL_BIJAO_QR
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = ladoLogo * 0.55f
        typeface = Typeface.DEFAULT_BOLD
    }
    val metricas = paintTexto.fontMetrics
    canvas.drawText("B", cx, cy - (metricas.ascent + metricas.descent) / 2, paintTexto)

    return bitmap
}
