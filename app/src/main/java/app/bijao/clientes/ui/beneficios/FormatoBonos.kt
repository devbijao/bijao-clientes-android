package app.bijao.clientes.ui.beneficios

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.bijao.clientes.core.models.Bono
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Textos, colores e íconos compartidos por bonos y tarjetas de sellos. Espejo
 * de `lib/app-clientes/bonos.ts` y `TarjetaSellos.tsx` en la web y de
 * `FormatoBonos.swift` en iOS, para que el mismo bono se lea igual en todas.
 */
object FormatoBonos {
    private val bogota: ZoneId = ZoneId.of("America/Bogota")
    private val meses = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    private val formatoPesos = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO"))

    fun pesos(valor: Double?): String = "$" + formatoPesos.format((valor ?: 0.0).roundToLong())

    /** Una fecha sola («2027-02-22») es ese día; un ISO con hora se pasa a Bogotá. */
    fun fecha(iso: String?): LocalDate? {
        if (iso.isNullOrBlank()) return null
        return try {
            if (iso.length == 10) LocalDate.parse(iso)
            else OffsetDateTime.parse(iso).atZoneSameInstant(bogota).toLocalDate()
        } catch (e: Exception) {
            null
        }
    }

    fun instante(iso: String?): Instant? = try {
        iso?.let { OffsetDateTime.parse(it).toInstant() }
    } catch (e: Exception) {
        null
    }

    /** «22 feb 2027». Armado a mano como en la web: el formato largo no cabe. */
    fun dia(iso: String?): String = fecha(iso)?.let { "${it.dayOfMonth} ${meses[it.monthValue - 1]} ${it.year}" } ?: ""

    /** «24 nov» para el historial de sellos. */
    fun diaCorto(iso: String?): String = fecha(iso)?.let { "${it.dayOfMonth} ${meses[it.monthValue - 1]}" } ?: ""

    /** «vence 22 feb 2027» con espacios duros: si no cabe, la fecha baja entera. */
    fun vence(iso: String): String = "vence ${dia(iso)}".replace(' ', ' ')

    /** El valor grande del voucher y la línea de abajo, según el tipo de bono. */
    fun textos(b: Bono): Pair<String, String> {
        val valor = when (b.tipo) {
            "VALOR" -> pesos(if (b.uso == "SALDO") b.saldo else b.valorInicial)
            "PORCENTAJE" -> "${(b.valor ?: 0.0).roundToLong()}%"
            else -> "1 gratis"
        }
        val detalle = when {
            b.estaPausado -> "En pausa por ahora"
            b.esDeSaldo -> "Saldo · ${vence(b.venceEn)}"
            b.tipo == "PRODUCTO" -> "${b.producto ?: "Producto o servicio"} · ${vence(b.venceEn)}"
            else -> "En una compra · ${vence(b.venceEn)}"
        }
        return valor to detalle
    }

    /** «A», «A y B», «A, B y C». */
    fun enumerar(xs: List<String>): String =
        if (xs.size <= 1) xs.firstOrNull() ?: "" else xs.dropLast(1).joinToString(", ") + " y " + xs.last()

    /** «AB3K9F2M» → «AB3K 9F2M»: una pausa a la mitad para dictarlo. */
    fun paraDictar(token: String): String = "${token.take(token.length / 2)} ${token.drop(token.length / 2)}"
}

// ── Colores ──────────────────────────────────────────────────────────────────

/** El rojo de los bonos de Bijao, cuando el bono no trae color. */
val ColorBonoPorDefecto = Color(0xFFB8342A)
/** El azul de Bijao, cuando la tarjeta no trae color. */
val ColorTarjetaPorDefecto = Color(0xFF1A5FC4)
/** El amarillo del regalo en el último espacio de la tarjeta. */
val AmarilloPremio = Color(0xFFF2C94C)
/** El azul marino de las sombras de la web (#0d1a47). */
val AzulSombra = Color(0xFF0D1A47)

fun colorDeHex(hex: String?): Color? {
    val texto = hex?.trim()?.removePrefix("#") ?: return null
    if (texto.length != 6) return null
    return texto.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
}

/** Texto oscuro sobre colores claros (el amarillo), blanco sobre el resto --
 * la misma luminancia que usa la web. */
fun tintaSobre(hex: String?): Color {
    val c = colorDeHex(hex) ?: ColorBonoPorDefecto
    val luz = 0.299 * c.red * 255 + 0.587 * c.green * 255 + 0.114 * c.blue * 255
    return if (luz > 160) Color(0xFF1D1A12) else Color.White
}

/** Este color mezclado con negro: `color-mix(in srgb, c 70%, #000)` con 0.3. */
fun Color.conNegro(proporcion: Float): Color =
    Color(red * (1 - proporcion), green * (1 - proporcion), blue * (1 - proporcion), alpha)

/** Este color mezclado con blanco: `color-mix(in srgb, c 12%, #fff)` con 0.12. */
fun Color.aclarado(proporcionColor: Float): Color = Color(
    red * proporcionColor + (1 - proporcionColor),
    green * proporcionColor + (1 - proporcionColor),
    blue * proporcionColor + (1 - proporcionColor),
)

// ── Íconos del sello ─────────────────────────────────────────────────────────

/** Los 10 íconos fijos que puede elegir el negocio. La lista se repite en el
 * backend (`fidelidad.ICONOS_SELLO`) y en la CHECK de la migración; uno que la
 * app no conozca cae al sello genérico. */
fun iconoSello(valor: String?): ImageVector = when (valor) {
    "corazon" -> Icons.Filled.Favorite
    "estrella" -> Icons.Filled.Star
    "taza" -> Icons.Filled.LocalCafe
    "hoja" -> Icons.Filled.Eco
    "tijera" -> Icons.Filled.ContentCut
    "flor" -> Icons.Filled.LocalFlorist
    "pata" -> Icons.Filled.Pets
    "helado" -> Icons.Filled.Icecream
    "diamante" -> Icons.Filled.Diamond
    else -> Icons.Filled.Verified
}

// ── Lo ya visto en este teléfono ─────────────────────────────────────────────

/** Saldos de la última visita, premios ya celebrados y franjas cerradas. Lo
 * mismo que la web guarda en el navegador: sin esto no se anima nada, y
 * borrarlo solo vuelve a animar. */
class RecuerdosBeneficios(context: Context) {
    private val prefs = context.getSharedPreferences("bijao.beneficios", Context.MODE_PRIVATE)

    var saldos: Map<String, Int>?
        get() = prefs.getString("saldos", null)?.let { texto ->
            texto.split('|').filter { it.contains('=') }.associate {
                val (id, n) = it.split('=', limit = 2); id to (n.toIntOrNull() ?: 0)
            }
        }
        set(valor) {
            prefs.edit().putString("saldos", valor?.entries?.joinToString("|") { "${it.key}=${it.value}" }).apply()
        }

    var premiosVistos: List<String>
        get() = prefs.getString("vistos", "")!!.split('|').filter { it.isNotEmpty() }
        set(valor) { prefs.edit().putString("vistos", valor.distinct().takeLast(100).joinToString("|")).apply() }

    var avisosCerrados: List<String>
        get() = prefs.getString("cerrados", "")!!.split('|').filter { it.isNotEmpty() }
        set(valor) { prefs.edit().putString("cerrados", valor.distinct().takeLast(100).joinToString("|")).apply() }
}
