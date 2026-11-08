package app.bijao.clientes.ui.creditos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.PreferenciaCobro
import app.bijao.clientes.core.models.PreferenciaCobroInput
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import kotlinx.coroutines.launch

private val NOMBRE = mapOf("APP" to "En esta app", "EMAIL" to "Por correo", "SMS" to "Por SMS")
private val ORDEN = listOf("APP", "EMAIL", "SMS")

/**
 * "Cómo te avisamos": la persona decide por qué canales cada negocio puede
 * recordarle sus pagos (app, correo, SMS), o pide no ser contactada.
 * Contrapeso de los recordatorios (Ley 2300 de 2023, Art. 2): lo que elige
 * aquí manda sobre lo que el negocio haya declarado. Espejo de
 * `PreferenciasCobro.tsx`. Si la lista llega vacía no se pinta nada.
 */
@Composable
fun PreferenciasCobro(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var filas by remember { mutableStateOf<List<PreferenciaCobro>?>(null) }
    var canalesConEnvio by remember { mutableStateOf(listOf("APP")) }
    var trabajando by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val r = ApiClient.service.preferenciasCobro().datoOLanzar()
            filas = r.data
            canalesConEnvio = r.canalesConEnvio
        } catch (e: Exception) {
            filas = emptyList()
        }
    }

    suspend fun guardar(f: PreferenciaCobro, canales: List<String>, noContactar: Boolean) {
        trabajando = f.negocio.id
        error = null
        try {
            val r = ApiClient.service.guardarPreferenciaCobro(f.negocio.id, PreferenciaCobroInput(canales, noContactar)).datoOLanzar()
            filas = filas?.map { if (it.negocio.id == f.negocio.id) r.data else it }
        } catch (e: Exception) {
            error = "No pudimos guardar el cambio."
        }
        trabajando = null
    }

    val lista = filas
    if (lista.isNullOrEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(" Cómo te avisamos", style = MaterialTheme.typography.titleSmall)
        }
        Text(
            "Cada negocio puede recordarte tus pagos, solo de lunes a sábado en horario diurno y como máximo una vez al día. Tú eliges por dónde.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column {
                lista.forEachIndexed { i, f ->
                    FilaPreferencia(
                        f = f, canalesConEnvio = canalesConEnvio, ocupado = trabajando == f.negocio.id,
                        onGuardar = { canales, noContactar -> scope.launch { guardar(f, canales, noContactar) } },
                    )
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun FilaPreferencia(
    f: PreferenciaCobro,
    canalesConEnvio: List<String>,
    ocupado: Boolean,
    onGuardar: (List<String>, Boolean) -> Unit,
) {
    val nombreNegocio = f.negocio.nombre ?: "Negocio"
    val activos = ORDEN.filter { f.canales.contains(it) }

    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(nombreNegocio, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (ocupado) CircularProgressIndicator(modifier = Modifier.size(16.dp))
        }
        Text(
            when {
                f.noContactar == "NEGOCIO" -> "Este negocio decidió no enviarte avisos."
                f.noContactar != null -> "Pediste no recibir avisos."
                activos.isEmpty() -> "No recibirás avisos de pago."
                else -> "Te recordaremos tus pagos por los canales que elegiste."
            },
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (f.noContactar == null) {
            ORDEN.filter { canalesConEnvio.contains(it) }.forEach { canal ->
                val on = f.canales.contains(canal)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(NOMBRE[canal] ?: canal, style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = on,
                        onCheckedChange = { nuevo -> onGuardar(if (nuevo) f.canales + canal else f.canales - canal, false) },
                        enabled = !ocupado,
                    )
                }
            }
        }

        if (f.noContactar == "CLIENTE_APP") {
            TextButton(onClick = { onGuardar(listOf("APP"), false) }, enabled = !ocupado) { Text("Volver a recibir avisos") }
        }
        if (f.noContactar == null) {
            TextButton(onClick = { onGuardar(emptyList(), true) }, enabled = !ocupado) {
                Text("No quiero que este negocio me contacte", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
