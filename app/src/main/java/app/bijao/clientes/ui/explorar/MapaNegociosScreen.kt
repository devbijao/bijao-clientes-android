package app.bijao.clientes.ui.explorar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.Negocio
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/** Mapa de los negocios con coordenadas -- equivalente de
 * `MapaNegociosView.swift`. Solo pines + una tarjeta flotante al tocar uno;
 * el resto de controles (zoom, mi ubicación) son los nativos del SDK. */
@Composable
fun MapaNegociosScreen(negocios: List<Negocio>, onFavorito: (Negocio) -> Unit, onNegocioClick: (String) -> Unit) {
    val negociosConCoords = remember(negocios) { negocios.filter { it.lat != null && it.lng != null } }
    var seleccionado by remember { mutableStateOf<Negocio?>(null) }

    if (negociosConCoords.isEmpty()) {
        EstadoVacio(
            icono = Icons.Filled.Map,
            titulo = "Sin ubicaciones en el mapa",
            descripcion = "Ninguno de estos negocios tiene coordenadas todavía.",
        )
        return
    }

    val primero = negociosConCoords.first()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(primero.lat!!, primero.lng!!), 12f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
        ) {
            negociosConCoords.forEach { negocio ->
                Marker(
                    state = MarkerState(position = LatLng(negocio.lat!!, negocio.lng!!)),
                    title = negocio.nombre ?: "Negocio",
                    onClick = {
                        seleccionado = negocio
                        false
                    },
                )
            }
        }

        seleccionado?.let { negocio ->
            NegocioFloatingCard(
                negocio = negocio,
                onFavorito = { onFavorito(negocio) },
                onClick = { onNegocioClick(negocio.id) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}
