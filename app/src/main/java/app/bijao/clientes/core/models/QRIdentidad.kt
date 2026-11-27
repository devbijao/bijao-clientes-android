package app.bijao.clientes.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Espejo de `POST /api/app/qr` -- token rotatorio de un solo uso que
 * identifica al cliente en caja (TTL corto, no otorga sellos por sí mismo). */
@Serializable
data class QRIdentidad(
    val token: String,
    @SerialName("expira_at") val expiraAt: String,
    @SerialName("ttl_segundos") val ttlSegundos: Int,
    val nombre: String? = null,
)
