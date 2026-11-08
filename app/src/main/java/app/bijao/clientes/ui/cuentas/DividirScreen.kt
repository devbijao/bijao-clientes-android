package app.bijao.clientes.ui.cuentas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.calculo.CalculadoraCuenta
import app.bijao.clientes.core.models.ContactoItem
import app.bijao.clientes.core.models.CrearCuentaResponse
import app.bijao.clientes.core.models.CuentaInput
import app.bijao.clientes.core.models.Negocio
import app.bijao.clientes.core.models.ParteCuentaInput
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ModoReparto { IGUAL, PERSONALIZADO }
private enum class ModoPropina { SIN, SUGERIDA, OTRA }

/**
 * "Dividir la cuenta" (D5) -- calculadora + creación del link. Espejo de
 * `frontend/app/(app-clientes)/dividir/page.tsx` y `DividirView.swift`. El
 * link público se abre en la web (sin pantalla nativa para *recibir*, ver el
 * handoff); esta pantalla solo calcula, crea la cuenta y la comparte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DividirScreen(onAtras: () -> Unit, onVerMisCuentas: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    fun compartirTexto(texto: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, texto)
        }
        context.startActivity(android.content.Intent.createChooser(intent, null))
    }

    var totalTexto by remember { mutableStateOf("") }
    var personas by remember { mutableStateOf(CalculadoraCuenta.MIN_PERSONAS) }
    var propina by remember { mutableStateOf(ModoPropina.SIN) }
    var otraPctTexto by remember { mutableStateOf("") }
    var modo by remember { mutableStateOf(ModoReparto.IGUAL) }
    var montos by remember { mutableStateOf(listOf("", "")) }
    var nombres by remember { mutableStateOf(listOf("", "")) }
    var contactoDe by remember { mutableStateOf(listOf<String?>(null, null)) }
    var contactos by remember { mutableStateOf(listOf<ContactoItem>()) }
    var menuAbiertoPara by remember { mutableStateOf<Int?>(null) }

    var negocio by remember { mutableStateOf<Negocio?>(null) }
    var buscandoNegocio by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    var resultadosNegocio by remember { mutableStateOf(listOf<Negocio>()) }
    var cargandoNegocios by remember { mutableStateOf(false) }

    var creando by remember { mutableStateOf(false) }
    var errorLink by remember { mutableStateOf<String?>(null) }
    var cuentaCreada by remember { mutableStateOf<CrearCuentaResponse?>(null) }

    val scope = rememberCoroutineScope()

    val total = CalculadoraCuenta.leerPesos(totalTexto)
    val pct = when (propina) {
        ModoPropina.SIN -> 0
        ModoPropina.SUGERIDA -> CalculadoraCuenta.PROPINA_SUGERIDA
        ModoPropina.OTRA -> minOf(100, otraPctTexto.toIntOrNull() ?: 0)
    }
    val aPagar = CalculadoraCuenta.totalConPropina(total, pct)
    val partesIguales = CalculadoraCuenta.repartirIgual(aPagar, personas)
    val partesPersonalizadas = montos.map(CalculadoraCuenta::leerPesos)
    val partes = if (modo == ModoReparto.IGUAL) partesIguales else partesPersonalizadas
    val falta = CalculadoraCuenta.restante(aPagar, partesPersonalizadas)
    val cuadra = modo == ModoReparto.IGUAL || falta == 0

    fun cambiarPersonas(n: Int) {
        val nuevo = n.coerceIn(CalculadoraCuenta.MIN_PERSONAS, CalculadoraCuenta.MAX_PERSONAS)
        personas = nuevo
        montos = (0 until nuevo).map { montos.getOrElse(it) { "" } }
        nombres = (0 until nuevo).map { nombres.getOrElse(it) { "" } }
        contactoDe = (0 until nuevo).map { contactoDe.getOrElse(it) { null } }
    }

    fun nombreDe(i: Int): String = nombres.getOrNull(i)?.trim()?.takeIf { it.isNotEmpty() } ?: "Persona ${i + 1}"

    fun elegirContacto(i: Int, contacto: ContactoItem?) {
        contactoDe = contactoDe.toMutableList().also { it[i] = contacto?.id }
        if (contacto != null && nombres.getOrElse(i) { "" }.trim().isEmpty()) {
            val primerNombre = (contacto.nombre ?: contacto.usuario ?: "").split(" ").firstOrNull().orEmpty()
            nombres = nombres.toMutableList().also { it[i] = primerNombre.take(30) }
        }
    }

    LaunchedEffect(Unit) {
        contactos = try {
            ApiClient.service.misContactos().datoOLanzar().data
        } catch (e: Exception) {
            emptyList()
        }
    }

    LaunchedEffect(buscandoNegocio, busqueda) {
        if (!buscandoNegocio) return@LaunchedEffect
        delay(300)
        cargandoNegocios = true
        resultadosNegocio = try {
            ApiClient.service.directorio(q = busqueda.ifBlank { null }, limite = 8).datoOLanzar().data
        } catch (e: Exception) {
            emptyList()
        }
        cargandoNegocios = false
    }

    if (cuentaCreada != null) {
        CuentaCreadaDialog(
            codigo = cuentaCreada!!.codigo,
            onCompartir = { codigo -> compartirTexto("${app.bijao.clientes.core.AppConfig.CLIENTES_WEB_BASE_URL}/dividir/$codigo") },
            onCerrar = { cuentaCreada = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dividir la cuenta") },
                navigationIcon = {
                    IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                },
                actions = {
                    TextButton(onClick = onVerMisCuentas) { Text("Mis cuentas") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Column {
                    Text("¿Dónde pagaron? (opcional)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.padding(top = 8.dp))
                    NegocioSelector(
                        negocio = negocio,
                        buscando = buscandoNegocio,
                        busqueda = busqueda,
                        resultados = resultadosNegocio,
                        cargando = cargandoNegocios,
                        onBuscandoChange = { buscandoNegocio = it; if (!it) busqueda = "" },
                        onBusquedaChange = { busqueda = it },
                        onElegir = { negocio = it; buscandoNegocio = false; busqueda = "" },
                        onQuitar = { negocio = null },
                    )
                }
            }

            item {
                Column {
                    Text("Total de la cuenta", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.padding(top = 8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("$", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.padding(start = 6.dp))
                            OutlinedTextField(
                                value = CalculadoraCuenta.formatoMiles(total),
                                onValueChange = { totalTexto = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                placeholder = { Text("0") },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            item {
                Column {
                    Text("Propina", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.padding(top = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Chip("Sin propina", propina == ModoPropina.SIN) { propina = ModoPropina.SIN }
                        Spacer(Modifier.padding(start = 8.dp))
                        Chip("${CalculadoraCuenta.PROPINA_SUGERIDA}%", propina == ModoPropina.SUGERIDA) { propina = ModoPropina.SUGERIDA }
                        Spacer(Modifier.padding(start = 8.dp))
                        Chip("Otra", propina == ModoPropina.OTRA) { propina = ModoPropina.OTRA }
                        if (propina == ModoPropina.OTRA) {
                            Spacer(Modifier.padding(start = 8.dp))
                            OutlinedTextField(
                                value = otraPctTexto,
                                onValueChange = { nuevo -> if (nuevo.length <= 3 && nuevo.all { it.isDigit() }) otraPctTexto = nuevo },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(70.dp),
                                suffix = { Text("%") },
                            )
                        }
                    }
                }
            }

            item {
                Column {
                    Text("¿Entre cuántos?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.padding(top = 8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { cambiarPersonas(personas - 1) }, enabled = personas > CalculadoraCuenta.MIN_PERSONAS) {
                                Icon(Icons.Filled.Remove, contentDescription = "Una persona menos")
                            }
                            Text("$personas personas", style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { cambiarPersonas(personas + 1) }, enabled = personas < CalculadoraCuenta.MAX_PERSONAS) {
                                Icon(Icons.Filled.Add, contentDescription = "Una persona más")
                            }
                        }
                    }
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = modo == ModoReparto.IGUAL, onClick = { modo = ModoReparto.IGUAL }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                        Text("Partes iguales")
                    }
                    SegmentedButton(selected = modo == ModoReparto.PERSONALIZADO, onClick = { modo = ModoReparto.PERSONALIZADO }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                        Text("Personalizado")
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))), RoundedCornerShape(20.dp))
                        .padding(18.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total a pagar" + if (pct > 0) " (con $pct% de propina)" else "", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
                        Text(CalculadoraCuenta.formatoCOP(aPagar), color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
                    }
                    if (modo == ModoReparto.IGUAL) {
                        Text("Cada uno paga", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
                        Text(CalculadoraCuenta.formatoCOP(partesIguales.lastOrNull() ?: 0), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        if (partesIguales.firstOrNull() != partesIguales.lastOrNull()) {
                            Text("Algunos ponen \$1 más para que la suma cuadre exacto.", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        val texto = when {
                            aPagar == 0 -> "Escribe el total para empezar"
                            falta == 0 -> "Cuadra perfecto"
                            falta > 0 -> "Faltan ${CalculadoraCuenta.formatoCOP(falta)} por asignar"
                            else -> "Te pasaste por ${CalculadoraCuenta.formatoCOP(-falta)}"
                        }
                        Text(texto, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }

            if (aPagar > 0) {
                items(partes.indices.toList()) { i ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = nombres.getOrElse(i) { "" },
                                onValueChange = { nuevo -> nombres = nombres.toMutableList().also { it[i] = nuevo.take(30) } },
                                placeholder = { Text("Persona ${i + 1}") },
                                modifier = Modifier.weight(1f),
                            )
                            if (contactos.isNotEmpty()) {
                                Spacer(Modifier.padding(start = 8.dp))
                                Box {
                                    val elegido = contactos.firstOrNull { it.id == contactoDe.getOrNull(i) }
                                    TextButton(onClick = { menuAbiertoPara = i }) {
                                        Text(
                                            elegido?.let { it.nombre ?: "@${it.usuario}" } ?: "Contacto…",
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                    DropdownMenu(expanded = menuAbiertoPara == i, onDismissRequest = { menuAbiertoPara = null }) {
                                        DropdownMenuItem(text = { Text("Sin contacto") }, onClick = { elegirContacto(i, null); menuAbiertoPara = null })
                                        contactos.filter { it.id == contactoDe.getOrNull(i) || it.id !in contactoDe }.forEach { c ->
                                            DropdownMenuItem(
                                                text = { Text(c.nombre ?: "@${c.usuario}") },
                                                onClick = { elegirContacto(i, c); menuAbiertoPara = null },
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.padding(start = 10.dp))
                            if (modo == ModoReparto.IGUAL) {
                                Text(CalculadoraCuenta.formatoCOP(partes[i]), style = MaterialTheme.typography.titleSmall)
                            } else {
                                OutlinedTextField(
                                    value = montos.getOrElse(i) { "" },
                                    onValueChange = { nuevo -> montos = montos.toMutableList().also { it[i] = nuevo } },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    prefix = { Text("$") },
                                    modifier = Modifier.width(120.dp),
                                )
                            }
                        }
                    }
                }
            }

            errorLink?.let { msg ->
                item { Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            creando = true
                            errorLink = null
                            scope.launch {
                                try {
                                    val datos = CuentaInput(
                                        total = total,
                                        propinaPct = pct,
                                        descripcion = null,
                                        negocioId = negocio?.id,
                                        partes = partes.indices.map { i ->
                                            ParteCuentaInput(nombres.getOrElse(i) { "" }.trim(), partes[i], contactoDe.getOrNull(i))
                                        },
                                    )
                                    cuentaCreada = ApiClient.service.crearCuenta(datos).datoOLanzar()
                                } catch (e: Exception) {
                                    errorLink = "No pudimos crear el link. Inténtalo de nuevo."
                                } finally {
                                    creando = false
                                }
                            }
                        },
                        enabled = aPagar > 0 && cuadra && !creando,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (creando) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Filled.Link, contentDescription = null)
                            Spacer(Modifier.padding(start = 8.dp))
                            Text("Crear link para compartir")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val lineas = partes.indices.map { i -> "${nombreDe(i)}: ${CalculadoraCuenta.formatoCOP(partes[i])}" }
                            val propinaTexto = if (pct > 0) " (con $pct% de propina)" else ""
                            compartirTexto("Cuenta: ${CalculadoraCuenta.formatoCOP(aPagar)}$propinaTexto\n${lineas.joinToString("\n")}")
                        },
                        enabled = aPagar > 0 && cuadra,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(Modifier.padding(start = 8.dp))
                        Text("Compartir solo el texto")
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            texto,
            color = if (seleccionado) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun NegocioSelector(
    negocio: Negocio?,
    buscando: Boolean,
    busqueda: String,
    resultados: List<Negocio>,
    cargando: Boolean,
    onBuscandoChange: (Boolean) -> Unit,
    onBusquedaChange: (String) -> Unit,
    onElegir: (Negocio) -> Unit,
    onQuitar: () -> Unit,
) {
    when {
        negocio != null -> {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    NegocioIcono(negocio.logoUrl)
                    Spacer(Modifier.padding(start = 10.dp))
                    Text(negocio.nombre ?: "Negocio", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = onQuitar) { Icon(Icons.Filled.Close, contentDescription = "Quitar negocio") }
                }
            }
        }
        buscando -> {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.padding(start = 8.dp))
                        OutlinedTextField(value = busqueda, onValueChange = onBusquedaChange, placeholder = { Text("Busca un negocio") }, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onBuscandoChange(false) }) { Icon(Icons.Filled.Close, contentDescription = "Cerrar búsqueda") }
                    }
                    if (cargando) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp).size(20.dp))
                    } else if (resultados.isEmpty()) {
                        Text("No encontramos negocios.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        resultados.forEach { n ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).clickable { onElegir(n) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                NegocioIcono(n.logoUrl)
                                Spacer(Modifier.padding(start = 10.dp))
                                Column {
                                    Text(n.nombre ?: "Negocio", style = MaterialTheme.typography.bodyMedium)
                                    n.ciudad?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                            }
                        }
                    }
                }
            }
        }
        else -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable { onBuscandoChange(true) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.padding(start = 10.dp))
                Text("Elegir un negocio", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NegocioIcono(url: String?) {
    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))) {
        if (url != null) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CuentaCreadaDialog(codigo: String, onCompartir: (String) -> Unit, onCerrar: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Link listo para compartir") },
        text = { Text("${app.bijao.clientes.core.AppConfig.CLIENTES_WEB_BASE_URL}/dividir/$codigo") },
        confirmButton = {
            Button(onClick = { onCompartir(codigo) }) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.padding(start = 8.dp))
                Text("Compartir link")
            }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Listo") } },
    )
}
