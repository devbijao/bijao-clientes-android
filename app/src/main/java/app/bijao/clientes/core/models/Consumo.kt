package app.bijao.clientes.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Espejo de `GET /api/app/consumos` y `/consumos/{id}` -- lo que la persona
 * ya usó de su suscripción (p. ej. un corte de un plan de 4). Va aparte de
 * `/historial`: un consumo cubierto al 100% por el plan no genera venta, así
 * que no está ahí y no cuenta como gasto. Nunca viajan precio, costo ni
 * margen -- eso es del negocio, no de quien compra. */
@Serializable
data class ConsumoApp(
    val id: String,
    /** Número del comprobante, `CONS-00012`. */
    val numero: String? = null,
    val negocio: NegocioHistorial,
    val fecha: String? = null,
    val producto: String? = null,
    val cantidad: Double,
    val plan: String? = null,
    @SerialName("atendido_por") val atendidoPor: String? = null,
)

@Serializable
data class ConsumosResponse(val data: List<ConsumoApp>, val total: Int = 0)

@Serializable
data class SaldoConsumo(
    /** `null` si el plan ya no incluye el servicio. */
    val incluido: Double? = null,
    val consumido: Double,
    val restante: Double? = null,
)

@Serializable
data class DetalleConsumo(
    val id: String,
    val numero: String? = null,
    val negocio: String? = null,
    @SerialName("negocio_slug") val negocioSlug: String? = null,
    val fecha: String? = null,
    val producto: String? = null,
    val cantidad: Double,
    val plan: String? = null,
    @SerialName("atendido_por") val atendidoPor: String? = null,
    @SerialName("periodo_inicio") val periodoInicio: String? = null,
    @SerialName("periodo_fin") val periodoFin: String? = null,
    /** Factura de la venta en la que se registró, si el consumo salió de una venta en caja. */
    @SerialName("numero_factura") val numeroFactura: String? = null,
    val saldo: SaldoConsumo,
)
