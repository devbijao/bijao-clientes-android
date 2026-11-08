package app.bijao.clientes.ui.creditos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.calculo.CalculadoraCuenta
import app.bijao.clientes.core.models.AvisoPagoApp
import app.bijao.clientes.core.models.AvisoPagoInput
import app.bijao.clientes.core.models.MedioAviso
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import kotlinx.coroutines.launch

/**
 * "Ya pagué": la persona avisa que pagó por su banca. No mueve el saldo: el
 * negocio verifica el dinero y lo confirma (o lo rechaza con un motivo).
 * Mientras haya un aviso abierto solo se puede retirar. Espejo de
 * `YaPague.tsx` en la web.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YaPague(creditoId: String, saldo: Double, nombreNegocio: String, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    var avisos by remember { mutableStateOf<List<AvisoPagoApp>?>(null) }
    var abierto by remember { mutableStateOf(false) }
    var monto by remember { mutableStateOf("") }
    var medio by remember { mutableStateOf(MedioAviso.NEQUI) }
    var nota by remember { mutableStateOf("") }
    var trabajando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun cargar() {
        avisos = try {
            ApiClient.service.avisosPago(creditoId).datoOLanzar().data
        } catch (e: Exception) {
            emptyList()
        }
    }
    LaunchedEffect(creditoId) { cargar() }

    val listaAvisos = avisos ?: return
    val pendiente = listaAvisos.firstOrNull { it.estado == "PENDIENTE" }
    val ultimo = if (pendiente == null) listaAvisos.firstOrNull { it.estado == "RECHAZADO" || it.estado == "CONFIRMADO" } else null

    Column(modifier = modifier) {
        if (pendiente != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row {
                        Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            " Avisaste un pago de ${CalculadoraCuenta.formatoCOP(pendiente.monto)}",
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        "$nombreNegocio lo está revisando. Cuando lo confirme, tu saldo se actualiza aquí y te llega un aviso.",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    TextButton(
                        onClick = {
                            scope.launch {
                                trabajando = true
                                error = null
                                try {
                                    ApiClient.service.cancelarAvisoPago(pendiente.id, creditoId).datoOLanzar()
                                    cargar()
                                } catch (e: Exception) {
                                    error = "No pudimos retirar el aviso."
                                    cargar()
                                }
                                trabajando = false
                            }
                        },
                        enabled = !trabajando,
                    ) { Text("Retirar aviso") }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                }
            }
        } else {
            if (ultimo != null && ultimo.estado == "RECHAZADO") {
                EtiquetaUltimo(ultimo, nombreNegocio, exito = false)
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
            } else if (ultimo != null && ultimo.estado == "CONFIRMADO") {
                EtiquetaUltimo(ultimo, nombreNegocio, exito = true)
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
            }

            if (!abierto) {
                Button(
                    onClick = {
                        monto = Math.round(saldo).toString()
                        abierto = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Ya pagué") }
            } else {
                val valor = monto.toIntOrNull() ?: 0
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Cuéntale a $nombreNegocio cuánto pagaste. Él lo verifica y registra tu pago.",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = monto,
                            onValueChange = { monto = it.filter { c -> c.isDigit() } },
                            label = { Text("¿Cuánto pagaste?") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Column {
                            Text("¿Por dónde?", style = MaterialTheme.typography.labelMedium)
                            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 4.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MedioAviso.entries.forEach { m ->
                                    FilterChip(selected = medio == m, onClick = { medio = m }, label = { Text(m.texto) })
                                }
                            }
                        }
                        OutlinedTextField(
                            value = nota,
                            onValueChange = { if (it.length <= 300) nota = it },
                            label = { Text("Nota (opcional)") },
                            placeholder = { Text("Ej. a nombre de mi esposa") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { abierto = false; error = null }, enabled = !trabajando) { Text("Cancelar") }
                            Button(
                                onClick = {
                                    scope.launch {
                                        trabajando = true
                                        error = null
                                        try {
                                            ApiClient.service.avisarPago(
                                                AvisoPagoInput(creditoId, valor.toDouble(), medio.valor, nota.trim().ifBlank { null }),
                                            ).datoOLanzar()
                                            abierto = false
                                            nota = ""
                                            cargar()
                                        } catch (e: Exception) {
                                            error = "No pudimos enviar el aviso."
                                        }
                                        trabajando = false
                                    }
                                },
                                enabled = !trabajando && valor > 0 && valor <= Math.round(saldo),
                                modifier = Modifier.weight(1f),
                            ) {
                                if (trabajando) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Text("Enviar aviso")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EtiquetaUltimo(a: AvisoPagoApp, nombreNegocio: String, exito: Boolean) {
    val color = if (exito) app.bijao.clientes.core.ui.ColorExito else MaterialTheme.colorScheme.error
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row {
                Icon(if (exito) Icons.Filled.CheckCircle else Icons.Filled.Error, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Text(
                    if (exito) " Tu último aviso de ${CalculadoraCuenta.formatoCOP(a.monto)} fue confirmado."
                    else " $nombreNegocio no pudo confirmar tu aviso de ${CalculadoraCuenta.formatoCOP(a.monto)}.",
                    style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold,
                )
            }
            if (!exito) {
                a.motivoRechazo?.let {
                    Text("Motivo: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
