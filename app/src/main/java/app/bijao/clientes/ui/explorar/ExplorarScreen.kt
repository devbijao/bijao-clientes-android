package app.bijao.clientes.ui.explorar

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.bijao.clientes.core.location.obtenerUbicacionActual
import app.bijao.clientes.core.models.Categoria
import app.bijao.clientes.core.models.Negocio
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Directorio de negocios -- equivalente de `ExplorarView.swift`. Solo
 * entran negocios que pidieron visibilidad y un admin aprobó. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ExplorarScreen(onNegocioClick: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val paginaTamano = 30

    var negocios by remember { mutableStateOf<List<Negocio>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var categorias by remember { mutableStateOf<List<Categoria>>(emptyList()) }
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }
    var busqueda by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var cargandoMas by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var modoMapa by remember { mutableStateOf(false) }
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }
    var pidiendoUbicacion by remember { mutableStateOf(false) }
    var mostrarMenuCategoria by remember { mutableStateOf(false) }

    suspend fun cargarDirectorio() {
        cargando = true
        error = null
        try {
            val respuesta = ApiClient.service.directorio(
                q = busqueda.ifBlank { null },
                categoria = categoriaSeleccionada,
                lat = lat,
                lng = lng,
                limite = paginaTamano,
            ).datoOLanzar()
            negocios = respuesta.data
            total = respuesta.total
        } catch (e: Exception) {
            error = e.message ?: "No pudimos cargar el directorio."
        } finally {
            cargando = false
        }
    }

    suspend fun cargarMas() {
        cargandoMas = true
        try {
            val respuesta = ApiClient.service.directorio(
                q = busqueda.ifBlank { null },
                categoria = categoriaSeleccionada,
                lat = lat,
                lng = lng,
                desde = negocios.size,
                limite = paginaTamano,
            ).datoOLanzar()
            negocios = negocios + respuesta.data
            total = respuesta.total
        } catch (e: Exception) {
            // Si falla "cargar más" se queda con lo que ya tenía.
        } finally {
            cargandoMas = false
        }
    }

    fun alternarFavorito(negocio: Negocio) {
        val nuevoValor = !negocio.favorito
        negocios = negocios.map { if (it.id == negocio.id) it.copy(favorito = nuevoValor) else it }
        scope.launch {
            try {
                if (nuevoValor) ApiClient.service.marcarFavorito(negocio.id).datoOLanzar()
                else ApiClient.service.quitarFavorito(negocio.id).datoOLanzar()
            } catch (e: Exception) {
                negocios = negocios.map { if (it.id == negocio.id) it.copy(favorito = !nuevoValor) else it }
            }
        }
    }

    val permisoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            scope.launch {
                pidiendoUbicacion = true
                obtenerUbicacionActual(context)?.let { lat = it.latitude; lng = it.longitude }
                pidiendoUbicacion = false
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            categorias = ApiClient.service.categorias().datoOLanzar().data
        } catch (e: Exception) {
            // Sin categorías la pantalla sigue siendo útil (lista sin filtros).
        }
        cargarDirectorio()
    }

    LaunchedEffect(categoriaSeleccionada, lat, lng) {
        cargarDirectorio()
    }

    LaunchedEffect(busqueda) {
        delay(400)
        cargarDirectorio()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Busca un negocio") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                val concedido = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                if (concedido) {
                    scope.launch {
                        pidiendoUbicacion = true
                        obtenerUbicacionActual(context)?.let { lat = it.latitude; lng = it.longitude }
                        pidiendoUbicacion = false
                    }
                } else {
                    permisoLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            }) {
                if (pidiendoUbicacion) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Icon(Icons.Filled.LocationOn, contentDescription = "Usar mi ubicación actual")
            }
            IconButton(onClick = { modoMapa = !modoMapa }) {
                Icon(
                    if (modoMapa) Icons.AutoMirrored.Filled.List else Icons.Filled.Map,
                    contentDescription = if (modoMapa) "Ver como lista" else "Ver como mapa",
                )
            }
        }

        if (categorias.isNotEmpty()) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                val nombreActual = categorias.firstOrNull { it.slug == categoriaSeleccionada }?.nombre ?: "Todas"
                SuggestionChip(
                    onClick = { mostrarMenuCategoria = true },
                    icon = { Icon(Icons.Filled.Apps, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(nombreActual) },
                )
                DropdownMenu(expanded = mostrarMenuCategoria, onDismissRequest = { mostrarMenuCategoria = false }) {
                    DropdownMenuItem(text = { Text("Todas") }, onClick = {
                        categoriaSeleccionada = null
                        mostrarMenuCategoria = false
                    })
                    categorias.forEach { categoria ->
                        DropdownMenuItem(text = { Text(categoria.nombre) }, onClick = {
                            categoriaSeleccionada = categoria.slug
                            mostrarMenuCategoria = false
                        })
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                cargando && negocios.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null && negocios.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.WifiOff,
                    titulo = "No pudimos cargar el directorio",
                    descripcion = error ?: "",
                )
                negocios.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.Search,
                    titulo = "Sin resultados",
                    descripcion = "Prueba con otra búsqueda o categoría.",
                )
                modoMapa -> MapaNegociosScreen(
                    negocios = negocios,
                    onFavorito = ::alternarFavorito,
                    onNegocioClick = onNegocioClick,
                )
                else -> PullToRefreshBox(
                    isRefreshing = cargando,
                    onRefresh = { scope.launch { cargarDirectorio() } },
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(negocios, key = { it.id }) { negocio ->
                            NegocioCard(
                                negocio = negocio,
                                onFavorito = { alternarFavorito(negocio) },
                                onClick = { onNegocioClick(negocio.id) },
                            )
                        }
                        if (negocios.size < total) {
                            item {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    if (cargandoMas) {
                                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                                    } else {
                                        Button(onClick = { scope.launch { cargarMas() } }) { Text("Cargar más") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EstadoVacio(icono: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, descripcion: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icono, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.padding(top = 12.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.padding(top = 4.dp))
        Text(descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
