package app.bijao.clientes.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Espejo de `GET /api/app/beneficios` -- una fila por recompensa activa de
 * cada negocio vinculado. El "saldo" de sellos vive en `saldos_fidelidad`,
 * no se calcula en el cliente. */
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
    @SerialName("puede_canjear") val puedeCanjear: Boolean,
    val agotada: Boolean,
    @SerialName("aplica_cualquier_compra") val aplicaCualquierCompra: Boolean = false,
)

@Serializable
data class BeneficiosResponse(val data: List<Tarjeta>, val negocios: Int = 0)

@Serializable
data class CanjeInput(@SerialName("negocio_id") val negocioId: String, @SerialName("recompensa_id") val recompensaId: String)

@Serializable
data class CanjeData(
    val codigo: String? = null,
    val estado: String? = null,
    @SerialName("fecha_expiracion") val fechaExpiracion: String? = null,
    val recompensa: String? = null,
    @SerialName("premio_texto") val premioTexto: String? = null,
    @SerialName("negocio_id") val negocioId: String? = null,
    @SerialName("saldo_restante") val saldoRestante: Int? = null,
    val instrucciones: String? = null,
)

@Serializable
data class CanjeResponse(val mensaje: String, val data: CanjeData)
