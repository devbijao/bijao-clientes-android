package app.bijao.clientes.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Espejo de `GET /api/app/favoritos`. Forma más chica que [Negocio] -- el
 * directorio trae rating/distancia/favorito, esto no -- así que es un modelo
 * aparte en vez de forzar campos opcionales sobre [Negocio]. */
@Serializable
data class FavoritoNegocio(
    val id: String,
    val nombre: String? = null,
    val slug: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("tipo_negocio") val tipoNegocio: String? = null,
    val ciudad: String? = null,
)

@Serializable
data class FavoritosResponse(val data: List<FavoritoNegocio>)
