package app.bijao.clientes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Azul Bijao (#007BFF) -- el mismo `AccentColor` que usa bijao-clientes-ios
 * y el mismo `--chart-1` de la web. */
val BijaoAzul = Color(0xFF007BFF)

// Mismos tokens neutros que `globals.css` de la web (--muted, --muted-foreground,
// --border) y el mismo rojo que `ColorError` en iOS -- sin esto, los roles que
// `lightColorScheme()` no pisa (surface, outline, error...) caen en el lila de
// Material por defecto, que es lo que hacía que la app se viera "de Android"
// en vez de Bijao.
private val SuperficieBlanca = Color(0xFFFFFFFF)
private val GrisSuave = Color(0xFFF1F5F9)
private val GrisBorde = Color(0xFFE2E8F0)
private val GrisTexto = Color(0xFF64748B)
private val TextoOscuro = Color(0xFF171717)
private val RojoError = Color(0xFFD92D20)

private val EsquemaClaro = lightColorScheme(
    primary = BijaoAzul,
    onPrimary = SuperficieBlanca,
    secondary = BijaoAzul,
    // Sin esto, controles como SegmentedButton usan el lila por defecto de
    // Material 3 para su estado seleccionado -- la marca no se ve de verdad
    // con solo pisar `primary`/`secondary`.
    secondaryContainer = BijaoAzul.copy(alpha = 0.18f),
    onSecondaryContainer = BijaoAzul,
    // Fondo de pantalla blanco (como `systemBackground`/`bg-white` en iOS y
    // la web), no gris -- el gris queda para `surfaceVariant`, que es lo que
    // usan las tarjetas de lista para destacarse del fondo.
    background = SuperficieBlanca,
    onBackground = TextoOscuro,
    surface = SuperficieBlanca,
    onSurface = TextoOscuro,
    surfaceVariant = GrisSuave,
    onSurfaceVariant = GrisTexto,
    surfaceContainer = SuperficieBlanca,
    surfaceContainerLow = SuperficieBlanca,
    surfaceContainerLowest = SuperficieBlanca,
    surfaceContainerHigh = GrisSuave,
    surfaceContainerHighest = GrisSuave,
    outline = GrisBorde,
    outlineVariant = GrisSuave,
    error = RojoError,
    onError = SuperficieBlanca,
    errorContainer = RojoError.copy(alpha = 0.12f),
    onErrorContainer = RojoError,
)

/**
 * Sin modo oscuro ni `dynamicColor` (Material You) a propósito: Clientes es
 * solo modo claro (misma decisión que ya tomó la web/iOS en la paridad), y
 * el azul de marca manda siempre, no el wallpaper de quien instale la app.
 */
@Composable
fun BijaoClientesTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EsquemaClaro, typography = BijaoTypography, content = content)
}
