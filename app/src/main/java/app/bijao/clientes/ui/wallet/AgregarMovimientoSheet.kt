package app.bijao.clientes.ui.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.calculo.CalculadoraCuenta
import app.bijao.clientes.core.models.CategoriasWallet
import app.bijao.clientes.core.models.MovimientoInput
import app.bijao.clientes.core.models.MovimientoWallet
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

/**
 * Hoja "Agregar movimiento" -- espejo de `FormularioMovimiento` en
 * `wallet/page.tsx` y `AgregarMovimientoView.swift`. Elegir la última
 * categoría de la lista ("Otros" / "Otros ingresos") pide la categoría
 * propia; el servidor la deja en la lista para la próxima vez.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgregarMovimientoSheet(categorias: CategoriasWallet, onCerrar: () -> Unit, onGuardado: (MovimientoWallet) -> Unit) {
    var tipo by remember { mutableStateOf("GASTO") }
    var montoTexto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(categorias.gasto.first()) }
    var nota by remember { mutableStateOf("") }
    var propia by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(WalletFechas.hoy()) }
    var mostrandoCalendario by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val listaCategorias = if (tipo == "GASTO") categorias.gasto else categorias.ingreso
    val libre = listaCategorias.last()
    val esLibre = categoria == libre
    val monto = CalculadoraCuenta.leerPesos(montoTexto)

    fun guardar() {
        guardando = true
        error = null
        scope.launch {
            val final = if (esLibre && propia.isNotBlank()) propia.trim() else categoria
            try {
                val r = ApiClient.service.agregarMovimientoWallet(
                    MovimientoInput(tipo = tipo, monto = monto, categoria = final, nota = nota.trim().ifBlank { null }, fecha = fecha),
                ).datoOLanzar()
                onGuardado(
                    MovimientoWallet(
                        id = r.id, origen = "MANUAL", tipo = tipo, monto = monto, fecha = fecha,
                        categoria = r.categoria, negocio = null, nota = nota.trim().ifBlank { null },
                    ),
                )
            } catch (e: Exception) {
                error = "No pudimos guardarlo. Inténtalo de nuevo."
            } finally {
                guardando = false
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Agregar movimiento", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.padding(top = 16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(selected = tipo == "GASTO", onClick = { tipo = "GASTO"; categoria = categorias.gasto.first(); propia = "" }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                    Text("Gasto")
                }
                SegmentedButton(selected = tipo == "INGRESO", onClick = { tipo = "INGRESO"; categoria = categorias.ingreso.first(); propia = "" }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                    Text("Ingreso")
                }
            }
            Spacer(Modifier.padding(top = 16.dp))

            OutlinedTextField(
                value = CalculadoraCuenta.formatoMiles(monto),
                onValueChange = { montoTexto = it },
                label = { Text("Monto") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                prefix = { Text("$") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.padding(top = 16.dp))

            Text("Categoría", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.padding(top = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listaCategorias.forEach { c ->
                    CategoriaChip(c, categoria == c) { categoria = c }
                }
            }

            if (esLibre) {
                Spacer(Modifier.padding(top = 12.dp))
                OutlinedTextField(
                    value = propia,
                    onValueChange = { propia = it.take(30) },
                    label = { Text("¿Cuál categoría? (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Se guarda y queda en la lista para la próxima vez.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.padding(top = 16.dp))
            Text("Fecha", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.padding(top = 8.dp))
            OutlinedButton(onClick = { mostrandoCalendario = true }, modifier = Modifier.fillMaxWidth()) {
                Text(WalletFechas.fecha(fecha).toString())
            }

            Spacer(Modifier.padding(top = 16.dp))
            OutlinedTextField(
                value = nota,
                onValueChange = { nota = it.take(80) },
                label = { Text("Nota (opcional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let {
                Spacer(Modifier.padding(top = 10.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.padding(top = 20.dp))
            Button(onClick = ::guardar, enabled = monto > 0 && !guardando, modifier = Modifier.fillMaxWidth()) {
                if (guardando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.padding(start = 8.dp))
                }
                Text("Guardar")
            }
            Spacer(Modifier.padding(bottom = 12.dp))
        }
    }

    if (mostrandoCalendario) {
        val hoyMillis = remember { WalletFechas.fecha(WalletFechas.hoy()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = WalletFechas.fecha(fecha).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            // Sin fechas futuras -- misma regla que el backend (`MovimientoInput`).
            selectableDates = remember {
                object : androidx.compose.material3.SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= hoyMillis
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { mostrandoCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { millis ->
                        fecha = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    mostrandoCalendario = false
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { mostrandoCalendario = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = estado)
        }
    }
}

@Composable
private fun CategoriaChip(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.FilterChip(selected = seleccionado, onClick = onClick, label = { Text(texto) })
}
