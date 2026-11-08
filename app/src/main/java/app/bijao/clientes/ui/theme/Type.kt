package app.bijao.clientes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

/**
 * Pisa el peso de los estilos que cargan jerarquía visual (títulos, botones,
 * tabs) a Semibold/Bold -- el default de Material 3 es Normal/Medium, que se
 * ve plano al lado de la web (Manrope `font-extrabold`/`font-semibold` en
 * `ShellClientes.tsx`) e iOS (`.bold`/`.semibold` en las vistas nativas). El
 * resto de los estilos (cuerpo, labels pequeños) se deja en el default de
 * Material -- no todo necesita más peso.
 */
val BijaoTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}
