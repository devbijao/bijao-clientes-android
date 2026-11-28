package app.bijao.clientes.ui.beneficios

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
 * Mientras el servidor la prepara (unos segundos), el botón dice «Preparando tu
 * tarjeta…»: sin eso parecía que no hacía nada (lo vio Harold en iOS).
 */
@Composable
fun BotonGoogleWallet(recompensaId: String) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(
            onClick = {
                if (cargando) return@Button
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
            },
            enabled = !cargando,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1F1F1F), contentColor = Color.White,
                disabledContainerColor = Color(0xFF1F1F1F), disabledContentColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cargando) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Text("Preparando tu tarjeta…", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp))
                } else {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Agregar a Google Wallet", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp))
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
