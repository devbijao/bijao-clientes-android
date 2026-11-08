package app.bijao.clientes.ui.creditos

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.PagosNegocio
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/**
 * Datos para pagarle a un negocio (Bre-B, Nequi, Bancolombia, QR) con botón
 * de copiar. Espejo de `BloquePago.tsx` en la web -- el negocio los
 * configura una sola vez en Ajustes y se muestran igual en Dividir cuenta y
 * en Mis créditos.
 */
@Composable
fun BloquePago(pagos: PagosNegocio, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        pagos.titular?.let {
            Text(
                buildAnnotatedStringTitular(it),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column {
                pagos.metodos.breb?.let { FilaPago(etiqueta = "Llave Bre-B", valor = it) }
                pagos.metodos.nequi?.let { FilaPago(etiqueta = "Nequi", valor = it) }
                pagos.metodos.bancolombia?.let { b ->
                    val tipo = b.tipo?.let { if (it == "CORRIENTE") "Corriente" else "Ahorros" }
                    FilaPago(etiqueta = "Bancolombia" + (tipo?.let { " · $it" } ?: ""), valor = b.numero)
                }
            }
        }

        pagos.qrUrl?.let { url ->
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = url,
                    contentDescription = "Código QR de pago",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(160.dp),
                )
            }
        }

        pagos.instrucciones?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FilaPago(etiqueta: String, valor: String) {
    val context = LocalContext.current
    var copiado by remember { mutableStateOf(false) }

    LaunchedEffect(copiado) {
        if (copiado) {
            delay(1500)
            copiado = false
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(onClick = {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard?.setPrimaryClip(ClipData.newPlainText(etiqueta, valor))
            copiado = true
        }) {
            Icon(
                if (copiado) Icons.Filled.Check else Icons.Filled.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(if (copiado) " Copiado" else " Copiar", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun buildAnnotatedStringTitular(nombre: String) =
    buildAnnotatedString {
        append("A nombre de ")
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(nombre) }
    }
