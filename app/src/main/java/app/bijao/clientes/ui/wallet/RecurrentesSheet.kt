package app.bijao.clientes.ui.wallet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.calculo.CalculadoraCuenta
import app.bijao.clientes.core.models.RecurrenteCambioInput
import app.bijao.clientes.core.models.RecurrenteWallet
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.ui.ColorExito
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Los ingresos y gastos que se repiten (F3): pausar, reanudar o dejar de
 * repetir, conservando o borrando lo ya generado. Equivalente de
 * `RecurrentesView.swift`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrentesSheet(recurrentes: List<RecurrenteWallet>, onCerrar: () -> Unit, onCambio: (List<RecurrenteWallet>) -> Unit) {
    val scope = rememberCoroutineScope()
    var lista by remember { mutableStateOf(recurrentes) }
    var aQuitar by remember { mutableStateOf<RecurrenteWallet?>(null) }
    var trabajando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun ejecutar(fallo: String, accion: suspend () -> List<RecurrenteWallet>) {
        if (trabajando) return
        scope.launch {
            trabajando = true
            try {
                lista = accion()
                onCambio(lista)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                error = fallo
            } finally {
                trabajando = false
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Recurrentes", style = MaterialTheme.typography.titleLarge)
            Text("Pausar no agrega después lo que tocaba mientras estuvo en pausa.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            LazyColumn {
                items(lista, key = { it.id }) { r ->
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.nota ?: r.categoria, fontWeight = FontWeight.SemiBold)
                                Text("${r.textoFrecuencia} · ${r.categoria}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${if (r.tipo == "INGRESO") "+" else "−"}${CalculadoraCuenta.formatoCOP(r.monto)}",
                                fontWeight = FontWeight.SemiBold,
                                color = if (r.tipo == "INGRESO") ColorExito else MaterialTheme.colorScheme.onSurface)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when {
                                    !r.activo -> "En pausa"
                                    r.proxima != null -> "Próximo: ${WalletFechas.fecha(r.proxima)}"
                                    else -> "Terminó"
                                },
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(checked = r.activo, enabled = !trabajando, onCheckedChange = { activo ->
                                ejecutar("No pudimos cambiarlo.") {
                                    val nuevo = ApiClient.service.cambiarRecurrente(r.id, RecurrenteCambioInput(activo)).datoOLanzar().data
                                    lista.map { if (it.id == r.id) nuevo else it }
                                }
                            })
                            IconButton(onClick = { aQuitar = r }, enabled = !trabajando) {
                                Icon(Icons.Filled.Delete, contentDescription = "Quitar")
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
            Text("", modifier = Modifier.padding(bottom = 24.dp))
        }
    }

    aQuitar?.let { r ->
        AlertDialog(
            onDismissRequest = { aQuitar = null },
            title = { Text("¿Dejar de repetir «${r.nota ?: r.categoria}»?") },
            text = {
                Column {
                    Text("Lo que ya se agregó a tu Wallet se puede conservar o borrar.")
                    TextButton(onClick = {
                        aQuitar = null
                        ejecutar("No pudimos quitarlo.") {
                            ApiClient.service.eliminarRecurrente(r.id, borrarMovimientos = true).datoOLanzar()
                            lista.filterNot { it.id == r.id }
                        }
                    }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Dejar de repetir y borrar sus movimientos", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    aQuitar = null
                    ejecutar("No pudimos quitarlo.") {
                        ApiClient.service.eliminarRecurrente(r.id, borrarMovimientos = false).datoOLanzar()
                        lista.filterNot { it.id == r.id }
                    }
                }) { Text("Dejar de repetir") }
            },
            dismissButton = { TextButton(onClick = { aQuitar = null }) { Text("Cancelar") } },
        )
    }
}
