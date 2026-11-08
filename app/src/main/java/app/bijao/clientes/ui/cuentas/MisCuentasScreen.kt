package app.bijao.clientes.ui.cuentas

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.AppConfig
import app.bijao.clientes.core.calculo.CalculadoraCuenta
import app.bijao.clientes.core.models.MiCuenta
import app.bijao.clientes.core.models.ParteMiCuenta
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.ui.ColorExito
import kotlinx.coroutines.launch

/**
 * "Mis cuentas" -- las cuentas que la persona creó al dividir. Espejo de
 * `frontend/app/(app-clientes)/cuentas/page.tsx` y `MisCuentasView.swift`:
 * marcar quién pagó, compartir el link de nuevo o retirar la cuenta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisCuentasScreen(onAtras: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    fun compartirTexto(texto: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, texto)
        }
        context.startActivity(android.content.Intent.createChooser(intent, null))
    }

    var cuentas by remember { mutableStateOf(listOf<MiCuenta>()) }
    var compartidas by remember { mutableStateOf(listOf<app.bijao.clientes.core.models.CuentaCompartida>()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var guardandoParte by remember { mutableStateOf<String?>(null) }
    var retirando by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun cargar() {
        cargando = true
        try {
            cuentas = ApiClient.service.misCuentas().datoOLanzar().data
            compartidas = try {
                ApiClient.service.cuentasCompartidas().datoOLanzar().data
            } catch (e: Exception) {
                emptyList()
            }
            error = null
        } catch (e: Exception) {
            error = "No pudimos cargar tus cuentas."
        }
        cargando = false
    }

    LaunchedEffect(Unit) { cargar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis cuentas") },
                navigationIcon = { IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } },
            )
        },
    ) { padding ->
        when {
            cargando -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.padding(top = 8.dp))
                    OutlinedButton(onClick = { scope.launch { cargar() } }) { Text("Reintentar") }
                }
            }
            cuentas.isEmpty() && compartidas.isEmpty() -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Aún no has dividido ninguna cuenta", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (compartidas.isNotEmpty()) {
                    item {
                        Text("Compartidas conmigo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(compartidas, key = { it.codigo }) { c ->
                        FilaCompartida(c) {
                            val url = "${AppConfig.CLIENTES_WEB_BASE_URL}/dividir/${c.codigo}"
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                        }
                    }
                }
                if (cuentas.isNotEmpty() && compartidas.isNotEmpty()) {
                    item {
                        Text("Las que armé", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(cuentas, key = { it.codigo }) { cuenta ->
                    CuentaCard(
                        cuenta = cuenta,
                        guardandoParte = guardandoParte,
                        retirando = retirando,
                        onAlternar = { parte ->
                            scope.launch {
                                guardandoParte = parte.id
                                val nuevo = if (parte.estado == "PAGADO") "PENDIENTE" else "PAGADO"
                                cuentas = cuentas.map { c ->
                                    if (c.codigo != cuenta.codigo) c
                                    else c.copy(partes = c.partes.map { if (it.id == parte.id) it.copy(estado = nuevo) else it })
                                }
                                try {
                                    ApiClient.service.marcarParteCuenta(cuenta.codigo, parte.id, app.bijao.clientes.core.models.EstadoParteInput(nuevo)).datoOLanzar()
                                } catch (e: Exception) {
                                    cargar()
                                } finally {
                                    guardandoParte = null
                                }
                            }
                        },
                        onCompartir = { compartirTexto("${AppConfig.CLIENTES_WEB_BASE_URL}/dividir/${cuenta.codigo}") },
                        onRetirar = {
                            scope.launch {
                                retirando = cuenta.codigo
                                try {
                                    ApiClient.service.eliminarCuentaDividida(cuenta.codigo).datoOLanzar()
                                    cuentas = cuentas.filterNot { it.codigo == cuenta.codigo }
                                } catch (e: Exception) {
                                    // Se queda en la lista; la persona puede reintentar.
                                } finally {
                                    retirando = null
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaCompartida(c: app.bijao.clientes.core.models.CuentaCompartida, onAbrir: () -> Unit) {
    Card(
        onClick = onAbrir,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    c.descripcion ?: c.negocio ?: "Cuenta de ${c.creador ?: "alguien"}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "De ${c.creador ?: "alguien"}${if (c.vencida) " · vencida" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(CalculadoraCuenta.formatoCOP(c.miMonto), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    if (c.miEstado == "PAGADO") "Pagado" else "Por pagar",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (c.miEstado == "PAGADO") ColorExito else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CuentaCard(
    cuenta: MiCuenta,
    guardandoParte: String?,
    retirando: String?,
    onAlternar: (ParteMiCuenta) -> Unit,
    onCompartir: () -> Unit,
    onRetirar: () -> Unit,
) {
    val pagadas = cuenta.partes.count { it.estado == "PAGADO" }
    val cobrado = cuenta.partes.filter { it.estado == "PAGADO" }.sumOf { it.monto }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(cuenta.descripcion ?: cuenta.negocio ?: "Cuenta dividida", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Han pagado $pagadas de ${cuenta.partes.size} · ${CalculadoraCuenta.formatoCOP(cobrado)} de ${CalculadoraCuenta.formatoCOP(cuenta.totalConPropina)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(CalculadoraCuenta.formatoCOP(cuenta.totalConPropina), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.padding(top = 10.dp))

            cuenta.partes.forEach { parte ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !cuenta.vencida) { onAlternar(parte) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (guardandoParte == parte.id) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(
                            if (parte.estado == "PAGADO") Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (parte.estado == "PAGADO") ColorExito else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.padding(start = 10.dp))
                    Text(
                        parte.nombre,
                        modifier = Modifier.weight(1f),
                        textDecoration = if (parte.estado == "PAGADO") TextDecoration.LineThrough else null,
                        color = if (parte.estado == "PAGADO") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(CalculadoraCuenta.formatoCOP(parte.monto), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!cuenta.vencida) {
                    OutlinedButton(onClick = onCompartir, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(Modifier.padding(start = 8.dp))
                        Text("Compartir link")
                    }
                } else {
                    Text("Link vencido", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                }
                IconButton(onClick = onRetirar, enabled = retirando == null) {
                    if (retirando == cuenta.codigo) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Icon(Icons.Filled.Delete, contentDescription = "Retirar cuenta")
                }
            }
        }
    }
}
