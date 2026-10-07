package app.bijao.clientes.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Espejo de `GET /api/app/vales` -- historial de vales emitidos al canjear. */
@Serializable
data class Vale(
    val codigo: String,
    val estado: String? = null,
    val origen: String? = null,
    val negocio: String? = null,
    val recompensa: String? = null,
    @SerialName("fecha_expiracion") val fechaExpiracion: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("canjeado_at") val canjeadoAt: String? = null,
)

@Serializable
data class ValesResponse(val data: List<Vale>)

/** Espejo de `POST /api/app/qr` -- token rotatorio de un solo uso que
 * identifica al cliente en caja (TTL corto, no otorga sellos por sí mismo). */
@Serializable
data class QRIdentidad(
    val token: String,
    @SerialName("expira_at") val expiraAt: String,
    @SerialName("ttl_segundos") val ttlSegundos: Int,
    val nombre: String? = null,
)
