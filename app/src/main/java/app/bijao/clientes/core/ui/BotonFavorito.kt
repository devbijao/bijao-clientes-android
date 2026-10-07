package app.bijao.clientes.core.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/** Corazón con un "pop" elástico al marcar/desmarcar -- equivalente de
 * `BotonFavorito.swift`, reusado en `NegocioCard`, `NegocioDetalleScreen` y
 * `NegocioFloatingCard`. */
@Composable
fun BotonFavorito(favorito: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var agrandado by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val escala by animateFloatAsState(
        targetValue = if (agrandado) 1.4f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "escalaFavorito",
    )

    IconButton(
        onClick = {
            onClick()
            agrandado = true
            scope.launch {
                delay(180)
                agrandado = false
            }
        },
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (favorito) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (favorito) "Quitar de favoritos" else "Agregar a favoritos",
            tint = if (favorito) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp).scale(escala),
        )
    }
}
