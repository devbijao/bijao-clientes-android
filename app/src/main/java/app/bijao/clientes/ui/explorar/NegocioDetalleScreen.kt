package app.bijao.clientes.ui.explorar

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.NegocioDetalle
import app.bijao.clientes.core.models.ResenasResponse
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.ui.BotonFavorito
import app.bijao.clientes.core.ui.Estrellas
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/** Ficha de un negocio del directorio -- `GET /api/app/negocios/{id}`,
 * equivalente de `NegocioDetalleView.swift`. Sin la sección de tarjetas de
 * sellos todavía (Fase A4). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NegocioDetalleScreen(negocioId: String, onAtras: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var detalle by remember { mutableStateOf<NegocioDetalle?>(null) }
    var resenasResp by remember { mutableStateOf<ResenasResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var mostrandoTodas by remember { mutableStateOf(false) }

    LaunchedEffect(negocioId) {
        error = null
        try {
            detalle = ApiClient.service.negocioDetalle(negocioId).datoOLanzar().data
            try {
                resenasResp = ApiClient.service.resenas(negocioId).datoOLanzar()
            } catch (e: Exception) {
                // Si falla el fetch de reseñas, la ficha sigue siendo útil sin ellas.
            }
        } catch (e: Exception) {
            error = e.message ?: "No pudimos cargar el negocio."
        }
    }

    if (mostrandoTodas && resenasResp != null) {
        TodasResenasScreen(negocioId, resenasResp!!, onAtras = { mostrandoTodas = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detalle?.nombre ?: "") },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { padding ->
        when {
            detalle != null -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                detalle!!.portadaUrl?.let { portada ->
                    item {
                        AsyncImage(
                            model = portada,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                        )
                    }
                }
                item {
                    Encabezado(
                        detalle = detalle!!,
                        onFavorito = {
                            scope.launch { alternarFavorito(negocioId, detalle!!) { nuevo -> detalle = nuevo } }
                        },
                    )
                }
                resenasResp?.let { r ->
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ResumenResenas(r, onVerTodas = { mostrandoTodas = true })
                            if (r.puedeResenar) {
                                Spacer(Modifier.padding(top = 12.dp))
                                FormularioResena(negocioId) {
                                    scope.launch {
                                        try {
                                            resenasResp = ApiClient.service.resenas(negocioId).datoOLanzar()
                                        } catch (e: Exception) { /* se queda con lo que había */ }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.padding(bottom = 24.dp)) }
            }
            error != null -> EstadoVacio(
                icono = Icons.Filled.WifiOff,
                titulo = "No pudimos cargar el negocio",
                descripcion = error ?: "",
            )
            else -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

private suspend fun alternarFavorito(negocioId: String, actual: NegocioDetalle, onCambio: (NegocioDetalle) -> Unit) {
    val nuevoValor = !actual.favorito
    onCambio(actual.copy(favorito = nuevoValor))
    try {
        if (nuevoValor) ApiClient.service.marcarFavorito(negocioId).datoOLanzar()
        else ApiClient.service.quitarFavorito(negocioId).datoOLanzar()
    } catch (e: Exception) {
        onCambio(actual.copy(favorito = !nuevoValor))
    }
}

@Composable
private fun Encabezado(detalle: NegocioDetalle, onFavorito: () -> Unit) {
    val context = LocalContext.current

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = detalle.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)),
            )
            Spacer(Modifier.padding(start = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(detalle.nombre ?: "Negocio", style = MaterialTheme.typography.titleLarge)
                Text(
                    listOfNotNull(detalle.tipoNegocio, detalle.ciudad).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                detalle.rating?.let { Estrellas(valor = it, tamano = 14.dp) }
            }
            BotonFavorito(favorito = detalle.favorito, onClick = onFavorito)
        }

        detalle.descripcion?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.padding(top = 10.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        detalle.direccion?.let {
            Spacer(Modifier.padding(top = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        detalle.whatsapp?.let { whatsapp ->
            Spacer(Modifier.padding(top = 12.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$whatsapp"))
                    context.startActivity(intent)
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF25D366)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Escribir por WhatsApp")
            }
        }

        detalle.tiendaUrl?.let { tienda ->
            Spacer(Modifier.padding(top = 8.dp))
            OutlinedButton(
                onClick = {
                    val url = if (tienda.startsWith("http")) tienda else "https://bijao.app$tienda"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.padding(start = 6.dp))
                Text("Visitar página web")
            }
        }
    }
}
