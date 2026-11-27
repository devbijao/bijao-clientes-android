package app.bijao.clientes.core.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bijao.clientes.ui.theme.BijaoAzul
import coil.compose.AsyncImage
import java.io.ByteArrayOutputStream

/** Foto de perfil redonda con la inicial de respaldo. Equivalente de
 * `FotoPerfil` en iOS; la usan Perfil, Contactos y el perfil de otra persona. */
@Composable
fun FotoPerfil(url: String?, nombre: String?, tamano: Dp = 44.dp, aro: Boolean = false, modifier: Modifier = Modifier) {
    val fondo = MaterialTheme.colorScheme.background
    Box(
        modifier
            .size(tamano)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (aro) Modifier.border(4.dp, fondo, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            (nombre ?: "?").trim().take(1).uppercase(),
            fontSize = (tamano.value * 0.4f).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (url != null) {
            AsyncImage(url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
        }
    }
}

/** La portada del perfil (franja apaisada de arriba, como en WhatsApp). Sin
 * foto, un degradado con el azul de Bijao. */
@Composable
fun PortadaPerfil(url: String?, alto: Dp = 200.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(alto)
            .background(Brush.linearGradient(listOf(BijaoAzul.copy(alpha = 0.9f), Color(0xFF0D1A47)))),
    ) {
        if (url != null) {
            AsyncImage(url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Recorte centrado al cuadrado, a `lado` px, en JPEG: la foto de perfil. */
fun jpegCuadrado(bitmap: Bitmap, lado: Int = 512): ByteArray {
    val tam = minOf(bitmap.width, bitmap.height)
    val cuadrado = Bitmap.createBitmap(bitmap, (bitmap.width - tam) / 2, (bitmap.height - tam) / 2, tam, tam)
    return comprimir(Bitmap.createScaledBitmap(cuadrado, lado, lado, true))
}

/** Recorte centrado a 2:1, a `ancho` px, en JPEG: la portada. */
fun jpegPortada(bitmap: Bitmap, ancho: Int = 1600): ByteArray {
    val altoDeseado = bitmap.width / 2
    val (w, h) = if (bitmap.height >= altoDeseado) bitmap.width to altoDeseado else (bitmap.height * 2) to bitmap.height
    val recorte = Bitmap.createBitmap(bitmap, (bitmap.width - w) / 2, (bitmap.height - h) / 2, w, h)
    return comprimir(Bitmap.createScaledBitmap(recorte, ancho, ancho / 2, true))
}

private fun comprimir(b: Bitmap): ByteArray =
    ByteArrayOutputStream().apply { b.compress(Bitmap.CompressFormat.JPEG, 85, this) }.toByteArray()

/** Lee la imagen elegida respetando su orientación: `ImageDecoder` (API 28+)
 * aplica el EXIF; `BitmapFactory` no, y una foto vertical salía girada. */
fun decodificarImagen(context: android.content.Context, uri: android.net.Uri): Bitmap? = try {
    if (android.os.Build.VERSION.SDK_INT >= 28) {
        android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) }
    }
} catch (e: Exception) {
    null
}
