package app.bijao.clientes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Azul Bijao (#007BFF) -- el mismo `AccentColor` que usa bijao-clientes-ios
 * (sin variante oscura allá tampoco) y el mismo `--chart-1` de la web. */
val BijaoAzul = Color(0xFF007BFF)

private val Blanco = Color(0xFFFFFFFF)

// Claro: estilo "agrupado" de iOS -- fondo gris suave y tarjetas blancas
// encima. Antes fondo y tarjetas eran casi el mismo blanco y una persona en
// la prueba interna dijo que "irrita los ojos".
private val FondoClaro = Color(0xFFF2F4F7)
private val BordeClaro = Color(0xFFE2E8F0)
private val TextoSecundarioClaro = Color(0xFF64748B)
private val TextoClaro = Color(0xFF171717)
private val RojoErrorClaro = Color(0xFFD92D20)

// Oscuro: mismos tonos que el sistema de iOS (systemBackground,
// secondarySystemBackground, separator) y el rojo de `ColorError` en oscuro.
private val FondoOscuro = Color(0xFF000000)
private val TarjetaOscura = Color(0xFF1C1C1E)
private val TarjetaOscuraAlta = Color(0xFF2C2C2E)
private val BordeOscuro = Color(0xFF38383A)
private val TextoSecundarioOscuro = Color(0xFF98989F)
private val TextoOscuro = Color(0xFFF5F5F7)
private val RojoErrorOscuro = Color(0xFFFF6961)

private val EsquemaClaro = lightColorScheme(
    primary = BijaoAzul,
    onPrimary = Blanco,
    secondary = BijaoAzul,
    // Sin esto, controles como SegmentedButton usan el lila por defecto de
    // Material 3 para su estado seleccionado.
    secondaryContainer = BijaoAzul.copy(alpha = 0.18f),
    onSecondaryContainer = BijaoAzul,
    background = FondoClaro,
    onBackground = TextoClaro,
    surface = FondoClaro,
    onSurface = TextoClaro,
    // Las tarjetas de lista usan `surfaceVariant` como contenedor.
    surfaceVariant = Blanco,
    onSurfaceVariant = TextoSecundarioClaro,
    surfaceContainerLowest = Blanco,
    surfaceContainerLow = Blanco,
    surfaceContainer = Blanco,
    surfaceContainerHigh = Blanco,
    surfaceContainerHighest = BordeClaro,
    outline = BordeClaro,
    outlineVariant = BordeClaro,
    error = RojoErrorClaro,
    onError = Blanco,
    errorContainer = RojoErrorClaro.copy(alpha = 0.12f),
    onErrorContainer = RojoErrorClaro,
)

private val EsquemaOscuro = darkColorScheme(
    primary = BijaoAzul,
    onPrimary = Blanco,
    secondary = BijaoAzul,
    secondaryContainer = BijaoAzul.copy(alpha = 0.30f),
    onSecondaryContainer = Blanco,
    background = FondoOscuro,
    onBackground = TextoOscuro,
    surface = FondoOscuro,
    onSurface = TextoOscuro,
    surfaceVariant = TarjetaOscura,
    onSurfaceVariant = TextoSecundarioOscuro,
    surfaceContainerLowest = FondoOscuro,
    surfaceContainerLow = TarjetaOscura,
    surfaceContainer = TarjetaOscura,
    surfaceContainerHigh = TarjetaOscuraAlta,
    surfaceContainerHighest = TarjetaOscuraAlta,
    outline = BordeOscuro,
    outlineVariant = BordeOscuro,
    error = RojoErrorOscuro,
    onError = FondoOscuro,
    errorContainer = RojoErrorOscuro.copy(alpha = 0.18f),
    onErrorContainer = RojoErrorOscuro,
)

/**
 * Sigue el modo del sistema, como la app de iOS. Sin `dynamicColor`
 * (Material You) a propósito: el azul de marca manda siempre, no el
 * wallpaper de quien instale la app.
 */
@Composable
fun BijaoClientesTheme(oscuro: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        typography = BijaoTypography,
        content = content,
    )
}
