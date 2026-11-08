package app.bijao.clientes.ui.beneficios

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.bijao.clientes.R
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
private const val MODULOS_FINDER = 7

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
    val context = LocalContext.current
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
            bitmap = withContext(Dispatchers.Default) { generarBitmapQR(context, qr.token, 600) }
            segundosRestantes = qr.ttlSegundos
            tokenTexto = qr.token
        } catch (e: Exception) {
            error = "Inténtalo de nuevo en un momento."
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

    // Respiración suave del resplandor -- "luminoso", no un halo fijo.
    val transicion = rememberInfiniteTransition(label = "brilloQR")
    val brillo by transicion.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "brilloQR",
    )

    // Sin `fillMaxSize()` -- este composable ya no es el contenido único de
    // una pantalla propia (ese wrapper se quitó junto con el atajo de
    // Beneficios), así que no debe forzar ocupar todo el espacio disponible
    // cuando convive con otros elementos, como el encabezado de Inicio.
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Resplandor: un halo radial azul detrás de la tarjeta blanca,
            // más grande que la tarjeta -- equivalente del `shadow` de color
            // que usa `CodigoQRView.swift`. `Modifier.blur` de Compose solo
            // suaviza de verdad en API 31+; por debajo es un no-op silencioso,
            // así que el degradado radial por sí solo ya tiene que leerse bien.
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .scale(brillo)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color(0xFF007BFF).copy(alpha = 0.55f),
                                androidx.compose.ui.graphics.Color(0xFF007BFF).copy(alpha = 0f),
                            ),
                        ),
                    ),
            )

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(20.dp)),
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
 * de negro, esquinas (finder patterns) como marco redondeado + círculo
 * interno, y el logo real de la marca en el centro. Equivalente de
 * `GeneradorQR.swift` (EFQRCode en iOS, Swift-only, sin versión para
 * Android); acá se arma a mano sobre `Encoder` de ZXing -- la API pública de
 * alto nivel (`QRCodeWriter.encode`) ya entrega la matriz escalada a píxeles,
 * sin acceso a los módulos individuales que hacen falta para dibujarlos así.
 *
 * Nivel de corrección `H` (el más alto) a propósito: al tapar el centro con
 * el logo, un nivel más bajo puede volverlo ilegible para el escáner de caja.
 */
private fun generarBitmapQR(context: Context, texto: String, tamano: Int): Bitmap {
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

    // Las 3 esquinas (finder patterns) son siempre 7x7 módulos en estas
    // posiciones fijas, sea cual sea la versión del QR -- se dibujan aparte,
    // con su propio estilo, y se saltan en el barrido módulo a módulo de abajo.
    val esquinas = listOf(
        0 to 0,
        (numModulos - MODULOS_FINDER) to 0,
        0 to (numModulos - MODULOS_FINDER),
    )
    fun dentroDeEsquina(x: Int, y: Int) = esquinas.any { (ex, ey) ->
        x >= ex && x < ex + MODULOS_FINDER && y >= ey && y < ey + MODULOS_FINDER
    }

    val paintModulo = Paint().apply { color = AZUL_BIJAO_QR; isAntiAlias = true }
    val radioModulo = anchoModulo * 0.28f
    for (y in 0 until numModulos) {
        for (x in 0 until numModulos) {
            if (matriz.get(x, y).toInt() == 1 && !dentroDeEsquina(x, y)) {
                val left = x * anchoModulo
                val top = y * anchoModulo
                canvas.drawRoundRect(
                    RectF(left, top, left + anchoModulo, top + anchoModulo),
                    radioModulo, radioModulo, paintModulo,
                )
            }
        }
    }

    val paintMarco = Paint().apply {
        color = AZUL_BIJAO_QR
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = anchoModulo * 1.3f
    }
    val paintCirculo = Paint().apply { color = AZUL_BIJAO_QR; isAntiAlias = true }
    for ((ex, ey) in esquinas) {
        val ladoFinder = MODULOS_FINDER * anchoModulo
        val margen = paintMarco.strokeWidth / 2
        val rectMarco = RectF(
            ex * anchoModulo + margen, ey * anchoModulo + margen,
            ex * anchoModulo + ladoFinder - margen, ey * anchoModulo + ladoFinder - margen,
        )
        canvas.drawRoundRect(rectMarco, ladoFinder * 0.28f, ladoFinder * 0.28f, paintMarco)

        val cx = ex * anchoModulo + ladoFinder / 2
        val cy = ey * anchoModulo + ladoFinder / 2
        canvas.drawCircle(cx, cy, ladoFinder * 0.18f, paintCirculo)
    }

    // Logo real de Bijao en el centro, con fondo blanco -- tapa los módulos
    // de en medio, por eso la corrección H de arriba.
    val ladoLogoFondo = tamano * 0.22f
    val cx = tamano / 2f
    val cy = tamano / 2f
    val rectFondo = RectF(cx - ladoLogoFondo / 2, cy - ladoLogoFondo / 2, cx + ladoLogoFondo / 2, cy + ladoLogoFondo / 2)
    canvas.drawRoundRect(rectFondo, ladoLogoFondo * 0.22f, ladoLogoFondo * 0.22f, Paint().apply { color = Color.WHITE; isAntiAlias = true })

    val ladoLogo = (tamano * 0.16f).toInt()
    val logo = BitmapFactory.decodeResource(context.resources, R.drawable.logo_qr)
    val logoEscalado = Bitmap.createScaledBitmap(logo, ladoLogo, ladoLogo, true)
    canvas.drawBitmap(logoEscalado, cx - ladoLogo / 2f, cy - ladoLogo / 2f, Paint().apply { isAntiAlias = true })

    return bitmap
}
