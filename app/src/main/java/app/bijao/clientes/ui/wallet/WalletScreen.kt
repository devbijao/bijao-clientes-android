package app.bijao.clientes.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.calculo.CalculadoraCuenta
import app.bijao.clientes.core.models.ActualWallet
import app.bijao.clientes.core.models.CategoriaTotal
import app.bijao.clientes.core.models.MovimientoWallet
import app.bijao.clientes.core.models.PeriodoResumen
import app.bijao.clientes.core.models.WalletResponse
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.ui.ColorExito
import kotlin.math.abs
import kotlinx.coroutines.launch

private val ETIQUETA_PERIODO = mapOf("dia" to "Día", "semana" to "Semana", "mes" to "Mes")
private val TITULO_PERIODO = mapOf("dia" to "Hoy", "semana" to "Esta semana", "mes" to "Este mes")

/**
 * Mi Wallet (W3) -- gastos automáticos en negocios Bijao + ingresos/gastos
 * manuales. Espejo de `frontend/app/(app-clientes)/wallet/page.tsx` y
 * `WalletView.swift`. El periodo lo elige el servidor según la actividad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(onAtras: () -> Unit) {
    var forzado by remember { mutableStateOf("auto") }
    // Periodo que se detalla; null = el actual.
    var ref by remember { mutableStateOf<String?>(null) }
    var datos by remember { mutableStateOf<WalletResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var actualizando by remember { mutableStateOf(false) }
    var agregando by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var borrandoId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun cargar() {
        try {
            datos = ApiClient.service.wallet(periodo = forzado, fecha = ref).datoOLanzar()
            error = null
        } catch (e: Exception) {
            error = "No pudimos cargar tu Wallet."
        }
        actualizando = false
    }

    LaunchedEffect(Unit) { cargar() }

    fun cambiarPeriodo(p: String) {
        if (p == forzado) return
        actualizando = true
        ref = null
        forzado = p
        scope.launch { cargar() }
    }

    fun irA(fecha: String?) {
        actualizando = true
        ref = fecha
        scope.launch { cargar() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Wallet") },
                navigationIcon = { IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } },
                actions = {
                    IconButton(onClick = { agregando = true }) { Icon(Icons.Filled.Add, contentDescription = "Agregar movimiento") }
                },
            )
        },
    ) { padding ->
        val d = datos
        when {
            d == null && error != null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.padding(top = 8.dp))
                    OutlinedButton(onClick = { scope.launch { cargar() } }) { Text("Reintentar") }
                }
            }
            d == null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> Contenido(
                datos = d,
                actualizando = actualizando,
                aviso = aviso,
                borrandoId = borrandoId,
                modifier = Modifier.padding(padding),
                onCambiarPeriodo = ::cambiarPeriodo,
                onIrA = ::irA,
                onBorrar = { m ->
                    scope.launch {
                        borrandoId = m.id
                        try {
                            ApiClient.service.eliminarMovimientoWallet(m.id).datoOLanzar()
                            datos = aplicarLocal(d, m, signo = -1)
                            cargar()
                        } catch (e: Exception) {
                            aviso = "No pudimos eliminarlo."
                        } finally {
                            borrandoId = null
                        }
                    }
                },
            )
        }
    }

    if (agregando) {
        datos?.let { actual ->
            AgregarMovimientoSheet(
                categorias = actual.categorias,
                onCerrar = { agregando = false },
                onGuardado = { nuevo ->
                    agregando = false
                    var conCategoria = actual
                    if (nuevo.tipo == "GASTO" && nuevo.categoria !in actual.categorias.gasto) {
                        val lista = actual.categorias.gasto.toMutableList()
                        lista.add(maxOf(0, lista.size - 1), nuevo.categoria)
                        conCategoria = conCategoria.copy(categorias = conCategoria.categorias.copy(gasto = lista))
                    } else if (nuevo.tipo == "INGRESO" && nuevo.categoria !in actual.categorias.ingreso) {
                        val lista = actual.categorias.ingreso.toMutableList()
                        lista.add(maxOf(0, lista.size - 1), nuevo.categoria)
                        conCategoria = conCategoria.copy(categorias = conCategoria.categorias.copy(ingreso = lista))
                    }
                    // Si la fecha cae en otro periodo, saltar a él -- si no, el
                    // movimiento queda guardado pero solo se nota en la gráfica.
                    if (nuevo.fecha < conCategoria.actual.desde || nuevo.fecha > conCategoria.actual.hasta) {
                        datos = conCategoria
                        ref = nuevo.fecha
                        actualizando = true
                        scope.launch { cargar() }
                    } else {
                        datos = aplicarLocal(conCategoria, nuevo, signo = 1)
                    }
                },
            )
        }
    }
}

@Composable
private fun Contenido(
    datos: WalletResponse,
    actualizando: Boolean,
    aviso: String?,
    borrandoId: String?,
    modifier: Modifier = Modifier,
    onCambiarPeriodo: (String) -> Unit,
    onIrA: (String?) -> Unit,
    onBorrar: (MovimientoWallet) -> Unit,
) {
    val p = datos.periodo
    val primero = datos.serie.firstOrNull()?.desde ?: datos.actual.desde
    val hayAnterior = datos.actual.desde > primero
    val hayPosterior = !datos.esActual
    val rotulo = if (datos.esActual) TITULO_PERIODO[p] ?: p
        else WalletFechas.rotuloPeriodo(datos.actual.desde, datos.actual.hasta, p)
    val totalGasto = maxOf(1, datos.porCategoria.sumOf { it.total })

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        aviso?.let { item { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

        item {
            Column {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("dia", "semana", "mes").forEachIndexed { i, v ->
                        SegmentedButton(selected = p == v, onClick = { onCambiarPeriodo(v) }, shape = SegmentedButtonDefaults.itemShape(i, 3)) {
                            Text(ETIQUETA_PERIODO[v] ?: v)
                        }
                    }
                }
                Spacer(Modifier.padding(top = 4.dp))
                if (datos.automatico) {
                    Text(
                        "Vista ${if (p == "dia") "diaria" else if (p == "semana") "semanal" else "mensual"} según tu actividad",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    TextButton(onClick = { onCambiarPeriodo("auto") }) { Text("Volver a la vista automática") }
                }
                Spacer(Modifier.padding(top = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onIrA(WalletFechas.sumarDias(datos.actual.desde, -1)) }, enabled = hayAnterior && !actualizando) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Periodo anterior")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(rotulo, style = MaterialTheme.typography.titleSmall)
                        if (!datos.esActual) TextButton(onClick = { onIrA(null) }) { Text("Volver a hoy", style = MaterialTheme.typography.labelSmall) }
                    }
                    IconButton(onClick = { onIrA(WalletFechas.sumarDias(datos.actual.hasta, 1)) }, enabled = hayPosterior && !actualizando) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Periodo siguiente")
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))), RoundedCornerShape(20.dp))
                    .padding(16.dp),
            ) {
                Text("Balance", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
                Text(
                    "${if (datos.actual.balance < 0) "−" else ""}${CalculadoraCuenta.formatoCOP(abs(datos.actual.balance))}",
                    color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.padding(top = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.padding(start = 4.dp))
                            Text("Ingresos", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                        }
                        Text(CalculadoraCuenta.formatoCOP(datos.actual.ingresos), color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.padding(start = 4.dp))
                            Text("Gastos", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                        }
                        Text(CalculadoraCuenta.formatoCOP(datos.actual.gastos), color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BarrasWallet(serie = datos.serie, etiqueta = { WalletFechas.etiquetaBarra(it.desde, p) }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.padding(top = 8.dp))
                    Row {
                        LeyendaPunto(ColorExito, "Ingresos")
                        Spacer(Modifier.padding(start = 16.dp))
                        LeyendaPunto(MaterialTheme.colorScheme.primary, "Gastos")
                    }
                }
            }
        }

        if (datos.porCategoria.isNotEmpty()) {
            item { Text("En qué gastaste", style = MaterialTheme.typography.titleSmall) }
            items(datos.porCategoria, key = { it.categoria }) { c ->
                CategoriaFila(c, totalGasto)
            }
            if (datos.porNegocio.isNotEmpty()) {
                item {
                    Text(
                        "En Bijao: " + datos.porNegocio.joinToString(" · ") { "${it.negocio} ${CalculadoraCuenta.formatoCOP(it.total)}" },
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { Text("Movimientos", style = MaterialTheme.typography.titleSmall) }
        if (datos.movimientos.isEmpty()) {
            item {
                Column {
                    Text("Sin movimientos en este periodo", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Tus compras en negocios Bijao aparecen solas. Agrega tus ingresos y otros gastos con el botón de arriba.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(datos.movimientos, key = { it.id }) { m ->
                FilaMovimiento(m, borrandoId == m.id, onBorrar)
            }
        }
    }
}

@Composable
private fun LeyendaPunto(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(50)))
        Spacer(Modifier.padding(start = 6.dp))
        Text(texto, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CategoriaFila(c: CategoriaTotal, totalGasto: Int) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(c.categoria, style = MaterialTheme.typography.bodyMedium)
            Text(CalculadoraCuenta.formatoCOP(c.total), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.padding(top = 4.dp))
        LinearProgressIndicator(progress = { c.total.toFloat() / totalGasto }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FilaMovimiento(m: MovimientoWallet, borrando: Boolean, onBorrar: (MovimientoWallet) -> Unit) {
    val colorIcono = if (m.tipo == "INGRESO") ColorExito else MaterialTheme.colorScheme.primary
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(32.dp).background(colorIcono.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (m.origen == "VENTA") Icons.Filled.Storefront else if (m.tipo == "INGRESO") Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                contentDescription = null, tint = colorIcono, modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.padding(start = 10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(m.negocio ?: m.nota ?: m.categoria, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "${WalletFechas.fecha(m.fecha)} · ${m.categoria}",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "${if (m.tipo == "INGRESO") "+" else "−"}${CalculadoraCuenta.formatoCOP(m.monto)}",
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            color = if (m.tipo == "INGRESO") ColorExito else MaterialTheme.colorScheme.onSurface,
        )
        if (m.origen == "MANUAL") {
            IconButton(onClick = { onBorrar(m) }, enabled = !borrando) {
                if (borrando) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Icon(Icons.Filled.Delete, contentDescription = "Eliminar movimiento")
            }
        }
    }
}

/**
 * Aplica (signo +1) o quita (signo −1) un movimiento sobre los datos ya en
 * pantalla, para que balance, barras y desglose cambien en el acto sin
 * esperar al servidor. Equivalente de `aplicarLocal` en `wallet/page.tsx` y
 * `WalletView.swift`.
 */
private fun aplicarLocal(w: WalletResponse, m: MovimientoWallet, signo: Int): WalletResponse {
    fun dentro(desde: String, hasta: String) = m.fecha >= desde && m.fecha <= hasta
    val delta = signo * m.monto

    val serie = w.serie.map { p ->
        if (!dentro(p.desde, p.hasta)) p
        else p.copy(
            ingresos = p.ingresos + if (m.tipo == "INGRESO") delta else 0,
            gastos = p.gastos + if (m.tipo == "GASTO") delta else 0,
        )
    }

    var actual = w.actual
    if (dentro(actual.desde, actual.hasta)) {
        val ing = actual.ingresos + if (m.tipo == "INGRESO") delta else 0
        val gas = actual.gastos + if (m.tipo == "GASTO") delta else 0
        actual = ActualWallet(actual.desde, actual.hasta, ing, gas, ing - gas)
    }

    var porCategoria = w.porCategoria
    if (m.tipo == "GASTO" && dentro(w.actual.desde, w.actual.hasta)) {
        porCategoria = if (porCategoria.any { it.categoria == m.categoria }) {
            porCategoria.map { if (it.categoria == m.categoria) it.copy(total = it.total + delta) else it }
        } else {
            porCategoria + CategoriaTotal(m.categoria, delta)
        }.filter { it.total > 0 }.sortedByDescending { it.total }
    }

    var movimientos = w.movimientos
    if (dentro(w.actual.desde, w.actual.hasta)) {
        movimientos = if (signo == 1) listOf(m) + movimientos else movimientos.filterNot { it.id == m.id }
    }

    return w.copy(actual = actual, serie = serie, porCategoria = porCategoria, movimientos = movimientos)
}
