package app.bijao.clientes.ui.favoritos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.FavoritoNegocio
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.ui.explorar.EstadoVacio
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/** Negocios guardados -- equivalente de `FavoritosView.swift`. Quitar un
 * favorito es un swipe-to-delete, no un corazón repetido: la lista entera
 * ya es la señal de "esto es favorito". */
@Composable
fun FavoritosScreen(onNegocioClick: (String) -> Unit) {
    var favoritos by remember { mutableStateOf<List<FavoritoNegocio>>(emptyList()) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun cargar() {
        cargando = true
        error = null
        try {
            favoritos = ApiClient.service.favoritos().datoOLanzar().data
        } catch (e: Exception) {
            error = e.message ?: "No pudimos cargar tus favoritos."
        } finally {
            cargando = false
        }
    }

    LaunchedEffect(Unit) { cargar() }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            cargando && favoritos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null && favoritos.isEmpty() -> EstadoVacio(
                icono = Icons.Filled.WifiOff,
                titulo = "No pudimos cargar tus favoritos",
                descripcion = error ?: "",
            )
            favoritos.isEmpty() -> EstadoVacio(
                icono = Icons.Filled.Favorite,
                titulo = "Sin favoritos todavía",
                descripcion = "Guarda negocios desde Explorar tocando el corazón.",
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(favoritos, key = { it.id }) { negocio ->
                    var visible by remember(negocio.id) { mutableStateOf(true) }
                    val scope = rememberCoroutineScope()

                    AnimatedVisibility(visible = visible, exit = shrinkVertically(tween(200))) {
                        FilaFavoritoDeslizable(
                            negocio = negocio,
                            onClick = { onNegocioClick(negocio.id) },
                            onQuitar = {
                                visible = false
                                favoritos = favoritos.filterNot { it.id == negocio.id }
                                scope.launch {
                                    try {
                                        ApiClient.service.quitarFavorito(negocio.id).datoOLanzar()
                                    } catch (e: Exception) {
                                        // Si falla, se queda quitado localmente -- el próximo
                                        // refresh de la lista lo corrige si de verdad no pasó.
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilaFavoritoDeslizable(negocio: FavoritoNegocio, onClick: () -> Unit, onQuitar: () -> Unit) {
    val estado = rememberSwipeToDismissBoxState(
        confirmValueChange = { valor ->
            if (valor == SwipeToDismissBoxValue.EndToStart || valor == SwipeToDismissBoxValue.StartToEnd) {
                onQuitar()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = estado,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Quitar de favoritos", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        FilaFavorito(negocio = negocio, onClick = onClick)
    }
}

@Composable
private fun FilaFavorito(negocio: FavoritoNegocio, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = negocio.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
            )
            Column {
                Text(
                    negocio.nombre ?: "Negocio",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                negocio.tipoNegocio?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                negocio.ciudad?.let { ciudad ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(ciudad, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
