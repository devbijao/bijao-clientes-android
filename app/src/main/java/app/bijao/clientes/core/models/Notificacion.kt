package app.bijao.clientes.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notificacion(
    val id: String,
    val tipo: String? = null,
    val titulo: String? = null,
    val cuerpo: String? = null,
    val enlace: String? = null,
    val leida: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
) {
    /** Si el enlace apunta a una compra del historial (tipo `COMPRA`,
     * "Compra en {negocio} por $X" -- ver `avisar_compra` en el backend), el
     * id para abrir `CompraDetalleScreen` directo. El resto de los tipos
     * (SELLO, CASI, etc.) no tienen pantalla propia todavía, así que su fila
     * no navega a ningún lado. */
    val compraId: String?
        get() = enlace?.takeIf { it.startsWith("/historial/") }?.removePrefix("/historial/")
}

@Serializable
data class NotificacionesResponse(
    val data: List<Notificacion>,
    @SerialName("sin_leer") val sinLeer: Int = 0,
    val disponible: Boolean = true,
)

@Serializable
data class SinLeerResponse(@SerialName("sin_leer") val sinLeer: Int = 0)
