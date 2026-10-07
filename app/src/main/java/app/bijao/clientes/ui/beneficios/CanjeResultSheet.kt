package app.bijao.clientes.ui.beneficios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.CanjeData

/** Vale PENDIENTE recién emitido -- se confirma en caja, no acá. Equivalente
 * de `CanjeResultSheet.swift`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanjeResultSheet(resultado: CanjeData, onCerrar: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                resultado.recompensa ?: "¡Canje realizado!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            resultado.premioTexto?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            resultado.codigo?.let { codigo ->
                Text(
                    codigo,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Text(
                resultado.instrucciones ?: "Muestra este código en caja para reclamar tu premio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Button(onClick = onCerrar, modifier = Modifier.fillMaxWidth()) { Text("Listo") }
        }
    }
}
