package app.bijao.clientes.ui.beneficios

import android.graphics.Bitmap
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Pantalla con TopAppBar -- se abre desde "Mostrar mi código" en Beneficios.
 * Inicio (Fase A7) usa el contenido [CodigoQR] directo, con su propio
 * encabezado, en vez de este wrapper. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodigoQRScreen(onAtras: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi código") },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { padding ->
        CodigoQR(modifier = Modifier.padding(padding))
    }
}

/**
 * QR de identidad que se muestra en caja -- equivalente de `CodigoQRView.swift`.
 * `POST /api/app/qr` entrega un token rotatorio de un solo uso (TTL corto);
 * se renueva solo al expirar.
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
            bitmap = withContext(Dispatchers.Default) { generarBitmapQR(qr.token, 480) }
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

private fun generarBitmapQR(texto: String, tamano: Int): Bitmap {
    val matriz = QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, tamano, tamano)
    val bitmap = Bitmap.createBitmap(tamano, tamano, Bitmap.Config.RGB_565)
    for (x in 0 until tamano) {
        for (y in 0 until tamano) {
            bitmap.setPixel(x, y, if (matriz[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}
