package app.bijao.clientes.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Espejo de `GET /api/app/historial` y `/historial/{id}` -- compras de la
 * persona en TODOS sus negocios, una sola línea de tiempo. Sin margen, costo
 * ni cajero: eso es del negocio, no del comprador. */
@Serializable
data class NegocioHistorial(
    val id: String,
    val nombre: String? = null,
    @SerialName("tipo_negocio") val tipoNegocio: String? = null,
)

@Serializable
data class Compra(
    val id: String,
    val negocio: NegocioHistorial,
    @SerialName("numero_factura") val numeroFactura: String? = null,
    val total: Double,
    val estado: String? = null,
    val fecha: String? = null,
)

@Serializable
data class HistorialResponse(val data: List<Compra>, val total: Int = 0)

@Serializable
data class ItemCompra(
    val nombre: String? = null,
    val cantidad: Double,
    @SerialName("precio_unitario") val precioUnitario: Double,
    val descuento: Double = 0.0,
)

@Serializable
data class CompraDetalle(
    val id: String,
    val negocio: String? = null,
    @SerialName("negocio_slug") val negocioSlug: String? = null,
    @SerialName("numero_factura") val numeroFactura: String? = null,
    val fecha: String? = null,
    val estado: String? = null,
    val subtotal: Double,
    val descuento: Double,
    val total: Double,
    val items: List<ItemCompra> = emptyList(),
)
