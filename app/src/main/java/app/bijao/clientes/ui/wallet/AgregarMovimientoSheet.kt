package app.bijao.clientes.ui.wallet

import app.bijao.clientes.core.ui.MilesTransformation
import app.bijao.clientes.core.ui.soloDigitos
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
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Switch
import app.bijao.clientes.core.models.RecurrenteInput
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
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
 *
 * «Se repite» (F3) lo guarda como recurrente: diario, semanal, mensual o cada N
 * días, desde la fecha elegida y, si se quiere, hasta otra. Ahí `onGuardado`
 * recibe `null` y el Wallet se recarga (el backend ya generó lo que tocaba).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgregarMovimientoSheet(categorias: CategoriasWallet, onCerrar: () -> Unit, onGuardado: (MovimientoWallet?) -> Unit) {
    var tipo by remember { mutableStateOf("GASTO") }
    var montoTexto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(categorias.gasto.first()) }
    var nota by remember { mutableStateOf("") }
    var propia by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(WalletFechas.hoy()) }
    var mostrandoCalendario by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var repetir by remember { mutableStateOf(false) }
    var frecuencia by remember { mutableStateOf("MES") }
    var cadaN by remember { mutableStateOf(15) }
    var hasta by remember { mutableStateOf<String?>(null) }
    var eligiendoFin by remember { mutableStateOf(false) }
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
            if (repetir) {
                try {
                    ApiClient.service.crearRecurrente(
                        RecurrenteInput(
                            tipo = tipo, monto = monto, categoria = final, nota = nota.trim().ifBlank { null },
                            frecuencia = frecuencia, cadaN = if (frecuencia == "CADA_N_DIAS") cadaN else 1,
                            desde = fecha, hasta = hasta,
                        ),
                    ).datoOLanzar()
                    onGuardado(null)
                } catch (e: Exception) {
                    error = e.message ?: "No pudimos guardarlo. Inténtalo de nuevo."
                } finally {
                    guardando = false
                }
                return@launch
            }
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
                value = montoTexto,
                onValueChange = { montoTexto = soloDigitos(it) },
                visualTransformation = MilesTransformation,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Se repite", modifier = Modifier.weight(1f).padding(start = 10.dp))
                Switch(checked = repetir, onCheckedChange = { repetir = it; if (!it && fecha > WalletFechas.hoy()) fecha = WalletFechas.hoy() })
            }
            if (repetir) {
                Spacer(Modifier.padding(top = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("DIA" to "Cada día", "SEMANA" to "Cada semana", "MES" to "Cada mes", "CADA_N_DIAS" to "Personalizado")
                        .forEach { (valor, texto) -> CategoriaChip(texto, frecuencia == valor) { frecuencia = valor } }
                }
                if (frecuencia == "CADA_N_DIAS") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cada $cadaN días", modifier = Modifier.weight(1f))
                        TextButton(onClick = { cadaN = maxOf(2, cadaN - 1) }, enabled = cadaN > 2) { Text("−") }
                        TextButton(onClick = { cadaN = minOf(365, cadaN + 1) }, enabled = cadaN < 365) { Text("+") }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Termina", modifier = Modifier.weight(1f))
                    if (hasta != null) {
                        TextButton(onClick = { eligiendoFin = true }) { Text(WalletFechas.fecha(hasta!!).toString()) }
                        TextButton(onClick = { hasta = null }) { Text("Quitar") }
                    } else {
                        TextButton(onClick = { eligiendoFin = true }) { Text("Nunca") }
                    }
                }
                Text(
                    "Se agrega solo " + when (frecuencia) {
                        "DIA" -> "todos los días"
                        "SEMANA" -> "cada semana, el mismo día"
                        "MES" -> "cada mes, el día ${WalletFechas.fecha(fecha).dayOfMonth}"
                        else -> "cada $cadaN días"
                    } + ". Lo puedes pausar o quitar desde «Recurrentes» en tu Wallet.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.padding(top = 16.dp))
            Text(if (repetir) "Empieza" else "Fecha", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    if (eligiendoFin) {
        SelectorFecha(inicial = hasta ?: fecha, minima = fecha, onElegida = { hasta = it }, onCerrar = { eligiendoFin = false })
    }

    if (mostrandoCalendario) {
        val hoyMillis = remember { WalletFechas.fecha(WalletFechas.hoy()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = WalletFechas.fecha(fecha).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            // Suelto: sin fechas futuras (regla de `MovimientoInput`). Recurrente: puede
            // empezar hasta un año adelante (el backend acepta ±366 días).
            selectableDates = remember(repetir) {
                object : androidx.compose.material3.SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) =
                        utcTimeMillis <= hoyMillis + (if (repetir) 366L * 86_400_000L else 0L)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorFecha(inicial: String, minima: String, onElegida: (String) -> Unit, onCerrar: () -> Unit) {
    val minMillis = remember(minima) { WalletFechas.fecha(minima).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = WalletFechas.fecha(inicial).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = remember(minMillis) {
            object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= minMillis
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(onClick = {
                estado.selectedDateMillis?.let { onElegida(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                onCerrar()
            }) { Text("Listo") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    ) { DatePicker(state = estado) }
}

@Composable
private fun CategoriaChip(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.FilterChip(selected = seleccionado, onClick = onClick, label = { Text(texto) })
}
