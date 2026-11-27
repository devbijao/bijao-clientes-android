package app.bijao.clientes.core.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Puntos de mil mientras se escribe un monto ("12500" se ve "12.500"). El
 * estado guarda solo dígitos y los puntos se pintan encima, así el cursor no
 * salta -- reescribir el texto con `formatoMiles` en cada tecla lo dejaba
 * antes del último dígito. Equivalente de `CampoPesos` en iOS.
 */
object MilesTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitos = text.text
        if (digitos.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val conPuntos = buildString {
            digitos.forEachIndexed { i, c ->
                if (i > 0 && (digitos.length - i) % 3 == 0) append('.')
                append(c)
            }
        }
        val mapeo = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val puntosAntes = (0 until offset).count { i -> i > 0 && (digitos.length - i) % 3 == 0 }
                return offset + puntosAntes
            }

            override fun transformedToOriginal(offset: Int): Int =
                conPuntos.take(offset.coerceAtMost(conPuntos.length)).count { it != '.' }
        }
        return TransformedText(AnnotatedString(conPuntos), mapeo)
    }
}

/** Lo que se guarda al escribir un monto: solo dígitos, sin ceros a la izquierda. */
fun soloDigitos(texto: String): String = texto.filter { it.isDigit() }.trimStart('0').take(12)
