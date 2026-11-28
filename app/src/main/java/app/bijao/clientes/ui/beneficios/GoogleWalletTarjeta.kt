package app.bijao.clientes.ui.beneficios

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.bijao.clientes.R
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * «Agregar a Google Wallet» en el detalle de una tarjeta de sellos (F5 de
 * `MDs/plan_apps_nativas_previo_revision.md`), gemelo de `AppleWalletTarjeta` en iOS.
 *
 * El backend crea o actualiza la tarjeta en Google (mismo diseño y QR que el pase de
 * Apple) y devuelve un enlace `pay.google.com/gp/v/save/…`. Al abrirlo, Android lo
 * entrega a Google Wallet (o al navegador si no está instalada). La tarjeta se
 * actualiza sola al ganar sellos.
 *
 * El botón es el oficial de Google (recursos Android XML de su guía de marca),
 * con «Preparando tu tarjeta…» encima mientras el servidor la prepara.
 */
@Composable
fun BotonGoogleWallet(recompensaId: String) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun agregar() {
        if (cargando) return
        scope.launch {
            cargando = true
            error = null
            try {
                val url = ApiClient.service.tarjetaGoogleWallet(recompensaId).datoOLanzar().data.url
                try {
                    contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: ActivityNotFoundException) {
                    error = "No encontramos Google Wallet ni un navegador en este teléfono."
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                error = e.message ?: "No pudimos preparar tu tarjeta. Inténtalo de nuevo."
            } finally {
                cargando = false
            }
        }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().height(48.dp)) {
            // El botón oficial de Google (res/layout/boton_google_wallet.xml): las guías de
            // marca piden usar su arte tal cual, no dibujar uno propio.
            AndroidView(
                factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.boton_google_wallet, null, false) },
                update = { vista ->
                    // Mientras carga se oculta solo el arte del botón: el fondo oficial
                    // queda igual y encima va «Preparando tu tarjeta…», así la forma no
                    // cambia. No se deshabilita: el estado deshabilitado del paquete de
                    // Google pinta una capa blanca rectangular. `agregar()` ya ignora
                    // toques repetidos.
                    vista.findViewById<View>(R.id.contenido_boton_google_wallet).visibility =
                        if (cargando) View.INVISIBLE else View.VISIBLE
                    vista.setOnClickListener { agregar() }
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (cargando) {
                // Mientras el servidor la prepara (unos segundos): sin esto parecía que el
                // botón no hacía nada (lo vio Harold en iOS).
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Text("Preparando tu tarjeta…", color = Color.White, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
        Text(
            error ?: "Tus sellos en Google Wallet, se actualizan solos.",
            style = MaterialTheme.typography.bodySmall,
            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
