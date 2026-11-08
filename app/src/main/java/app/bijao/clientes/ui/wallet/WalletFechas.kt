package app.bijao.clientes.ui.wallet

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Utilidades de fecha del Wallet -- todo en `America/Bogota` y strings
 * "YYYY-MM-DD" (el mismo formato que manda/recibe el backend), para no
 * arrastrar el huso horario del dispositivo a un día equivocado. Equivalente
 * de `WalletFechas.swift`.
 */
object WalletFechas {
    val bogota: ZoneId = ZoneId.of("America/Bogota")
    private val localeEs = Locale.Builder().setLanguage("es").setRegion("CO").build()

    fun hoy(): String = LocalDate.now(bogota).toString()

    fun fecha(iso: String): LocalDate = LocalDate.parse(iso)

    fun sumarDias(iso: String, n: Int): String = fecha(iso).plusDays(n.toLong()).toString()

    fun etiquetaBarra(iso: String, periodo: String): String {
        val d = fecha(iso)
        return when (periodo) {
            "dia" -> d.dayOfMonth.toString()
            "semana" -> "${d.dayOfMonth}/${d.monthValue}"
            else -> d.month.getDisplayName(TextStyle.SHORT, localeEs).replace(".", "")
        }
    }

    /** "3–9 nov" / "octubre 2026" / "sáb, 2 nov": el periodo detallado cuando no es el actual. */
    fun rotuloPeriodo(desde: String, hasta: String, periodo: String): String {
        val a = fecha(desde)
        val b = fecha(hasta)
        if (periodo == "dia") {
            val dia = a.dayOfWeek.getDisplayName(TextStyle.SHORT, localeEs)
            val mes = a.month.getDisplayName(TextStyle.SHORT, localeEs).replace(".", "")
            return "$dia, ${a.dayOfMonth} $mes"
        }
        if (periodo == "mes") {
            val mes = a.month.getDisplayName(TextStyle.FULL, localeEs)
            return "$mes ${a.year}"
        }
        val mismoMes = a.monthValue == b.monthValue
        val mesA = a.month.getDisplayName(TextStyle.SHORT, localeEs).replace(".", "")
        val mesB = b.month.getDisplayName(TextStyle.SHORT, localeEs).replace(".", "")
        return "${a.dayOfMonth}${if (mismoMes) "" else " $mesA"} – ${b.dayOfMonth} $mesB"
    }
}
