package app.bijao.clientes.core.models

import app.bijao.clientes.core.network.ApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/** Espejo de `GET /api/app/beneficios` -- una fila por recompensa activa de
 * cada negocio vinculado. El "saldo" de sellos vive en `saldos_fidelidad`,
 * no se calcula en el cliente.
 *
 * Desde F5 de `MDs/plan_bonos.md` no hay «Canjear»: al completar la tarjeta
 * el premio se emite solo como bono (`origen: FIDELIDAD`) y la tarjeta vuelve
 * a cero. `puede_canjear` sigue llegando, pero la app ya no lo lee. */
@Serializable
data class NegocioResumen(
    val id: String,
    val nombre: String? = null,
    val slug: String? = null,
    @SerialName("tipo_negocio") val tipoNegocio: String? = null,
)

@Serializable
data class Tarjeta(
    @SerialName("recompensa_id") val recompensaId: String,
    val negocio: NegocioResumen,
    val nombre: String? = null,
    val descripcion: String? = null,
    @SerialName("imagen_url") val imagenUrl: String? = null,
    val tipo: String? = null,
    @SerialName("premio_texto") val premioTexto: String? = null,
    @SerialName("descuento_porcentaje") val descuentoPorcentaje: Double? = null,
    @SerialName("descuento_monto") val descuentoMonto: Double? = null,
    val saldo: Int,
    val meta: Int,
    val faltan: Int,
    val agotada: Boolean = false,
    @SerialName("aplica_cualquier_compra") val aplicaCualquierCompra: Boolean = false,
    /** Productos (o «productos de X») con que suma una tarjeta condicionada. */
    @SerialName("suma_con") val sumaCon: List<String>? = null,
    /** Cara de la tarjeta (hex); null = la de Bijao. */
    val color: String? = null,
    /** Uno de los 10 íconos fijos; null = sello genérico. */
    val icono: String? = null,
    /** El premio en pocas palabras: «15% en una compra». */
    val premio: String? = null,
    /** Días que dura el bono-premio al completar la tarjeta. */
    @SerialName("premio_dias") val premioDias: Int? = null,
) {
    /** Lo que se gana, con el mismo orden de preferencia que la web. */
    val textoPremio: String get() = premio ?: premioTexto ?: nombre ?: "Premio"
}

@Serializable
data class BeneficiosResponse(val data: List<Tarjeta>, val negocios: Int = 0)

@Serializable
data class MovimientoTarjeta(
    /** ACUMULACION | CANJE | REVERSION | EXPIRACION | AJUSTE */
    val tipo: String,
    /** «Compra», «Premio», «Compra anulada», «Sellos vencidos», «Ajuste del negocio» */
    val motivo: String = "",
    val sellos: Int = 0,
    val fecha: String,
    val monto: Double? = null,
    /** Cómo quedó la tarjeta después del movimiento. */
    val saldo: Int = 0,
)

@Serializable
private data class SoloHistorial(
    val historial: List<MovimientoTarjeta> = emptyList(),
    /** El backend puede crear la tarjeta en Google Wallet (F5). */
    @SerialName("google_wallet") val googleWallet: Boolean = false,
)

/** `GET /api/app/beneficios/{id}`: la tarjeta y sus últimos movimientos, en un
 * mismo objeto -- se decodifica dos veces del mismo JSON. */
data class TarjetaDetalle(
    val tarjeta: Tarjeta,
    val historial: List<MovimientoTarjeta>,
    val googleWallet: Boolean = false,
) {
    companion object {
        fun desde(obj: JsonObject): TarjetaDetalle {
            val extra = ApiClient.json.decodeFromJsonElement<SoloHistorial>(obj)
            return TarjetaDetalle(
                tarjeta = ApiClient.json.decodeFromJsonElement<Tarjeta>(obj),
                historial = extra.historial,
                googleWallet = extra.googleWallet,
            )
        }
    }
}

/** `GET /api/app/beneficios/{id}/google-wallet`: el enlace de «Agregar a Google Wallet». */
@Serializable
data class TarjetaGoogleWallet(val jwt: String, val url: String)

/** `GET /api/app/beneficios/{id}/google-wallet/estado`: si ya está en su Google Wallet. */
@Serializable
data class EstadoGoogleWallet(val guardada: Boolean = false)
