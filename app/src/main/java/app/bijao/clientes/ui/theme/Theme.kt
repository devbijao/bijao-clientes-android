package app.bijao.clientes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** Azul Bijao (#007BFF) -- el mismo `AccentColor` que usa bijao-clientes-ios. */
val BijaoAzul = androidx.compose.ui.graphics.Color(0xFF007BFF)

private val EsquemaClaro = lightColorScheme(
    primary = BijaoAzul,
    secondary = BijaoAzul,
    // Sin esto, controles como SegmentedButton usan el lila por defecto de
    // Material 3 para su estado seleccionado -- la marca no se ve de verdad
    // con solo pisar `primary`/`secondary`.
    secondaryContainer = BijaoAzul.copy(alpha = 0.18f),
    onSecondaryContainer = BijaoAzul,
)

private val EsquemaOscuro = darkColorScheme(
    primary = BijaoAzul,
    secondary = BijaoAzul,
    secondaryContainer = BijaoAzul.copy(alpha = 0.3f),
    onSecondaryContainer = androidx.compose.ui.graphics.Color.White,
)

/**
 * Sin `dynamicColor` (Material You) a propósito: el azul de marca manda
 * siempre, no el wallpaper de quien lo instale -- misma decisión que iOS de
 * no dejar que el sistema le gane el color a la marca.
 */
@Composable
fun BijaoClientesTheme(content: @Composable () -> Unit) {
    val esquema = if (isSystemInDarkTheme()) EsquemaOscuro else EsquemaClaro
    MaterialTheme(colorScheme = esquema, content = content)
}
