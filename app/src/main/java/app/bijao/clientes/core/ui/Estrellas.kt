package app.bijao.clientes.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Equivalente de `Estrellas.swift`. `tamano` en dp en vez de una `Font`
 * porque acá el ícono (no texto) es lo que cambia de tamaño. */
@Composable
fun Estrellas(valor: Double, tamano: Dp = 16.dp, modifier: Modifier = Modifier) {
    val llenas = valor.roundToInt().coerceIn(0, 5)
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "${"%.1f".format(valor)} de 5 estrellas"
        },
    ) {
        repeat(5) { i ->
            Icon(
                imageVector = if (i < llenas) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(tamano),
            )
        }
    }
}
