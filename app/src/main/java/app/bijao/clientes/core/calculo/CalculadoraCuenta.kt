package app.bijao.clientes.core.calculo

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Aritmética de "Dividir la cuenta", en pesos enteros -- debe dar EXACTAMENTE
 * lo mismo que el backend (`backend/routers/app_cuentas.py: total_con_propina`),
 * la web (`frontend/lib/app-clientes/dividir.ts`) e iOS (`CalculadoraCuenta.swift`):
 * con `Double`, repartir $100.000 entre 3 personas no suma $100.000 exacto.
 */
object CalculadoraCuenta {
    const val MIN_PERSONAS = 2
    const val MAX_PERSONAS = 30
    const val PROPINA_SUGERIDA = 10

    /** Total + propina, redondeado al peso (round-half-up, sin flotantes). */
    fun totalConPropina(total: Int, propinaPct: Int): Int {
        if (total <= 0) return 0
        val pct = min(100, max(0, propinaPct))
        return (total.toLong() * (100 + pct) * 2 + 100).toInt() / 200
    }

    /** Reparte [monto] entre [personas] en pesos enteros. El residuo se
     * reparte de a $1 entre las primeras personas, así las partes difieren
     * como máximo en $1 y suman exactamente [monto]. */
    fun repartirIgual(monto: Int, personas: Int): List<Int> {
        val n = max(1, personas)
        val total = max(0, monto)
        val base = total / n
        val resto = total - base * n
        return (0 until n).map { base + if (it < resto) 1 else 0 }
    }

    /** Lo que falta (positivo) o sobra (negativo) por asignar en el modo personalizado. */
    fun restante(monto: Int, partes: List<Int>): Int = monto - partes.sum()

    /** "12.500" -> 12500. Ignora todo lo que no sea dígito. */
    fun leerPesos(texto: String): Int {
        val digitos = texto.filter { it.isDigit() }.take(12)
        return digitos.toIntOrNull() ?: 0
    }

    fun formatoCOP(n: Int): String {
        val f = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("es").setRegion("CO").build())
        f.maximumFractionDigits = 0
        return f.format(n)
    }

    /** Mismo formato, para montos que llegan en `Double` (créditos, con
     * intereses) -- el backend nunca manda centavos, así que redondea igual
     * que la versión en `Int`. */
    fun formatoCOP(n: Double): String = formatoCOP(Math.round(n).toInt())

    fun formatoMiles(n: Int): String {
        if (n <= 0) return ""
        val f = NumberFormat.getNumberInstance(Locale.Builder().setLanguage("es").setRegion("CO").build())
        f.maximumFractionDigits = 0
        return f.format(n)
    }
}
