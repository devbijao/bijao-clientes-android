package app.bijao.clientes.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Espejo de `Negocio.swift` -- fila del directorio (`GET /api/app/directorio`). */
@Serializable
data class Negocio(
    val id: String,
    val nombre: String? = null,
    val slug: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("portada_url") val portadaUrl: String? = null,
    val descripcion: String? = null,
    @SerialName("tipo_negocio") val tipoNegocio: String? = null,
    val ciudad: String? = null,
    // Ya formateada por el backend (ej. "2.3 km") -- solo viene si se mandó lat/lng.
    val distancia: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val rating: Double? = null,
    val resenas: Int = 0,
    val favorito: Boolean = false,
)

@Serializable
data class DirectorioResponse(
    val data: List<Negocio>,
    val total: Int,
    @SerialName("por_cercania") val porCercania: Boolean = false,
)

/** Solo trae categorías con negocios visibles de verdad, no la taxonomía completa. */
@Serializable
data class Categoria(
    val slug: String,
    val nombre: String,
    val icono: String? = null,
    val negocios: Int = 0,
)

@Serializable
data class CategoriasResponse(
    val data: List<Categoria>,
    val ciudades: List<String> = emptyList(),
)

@Serializable
data class FavoritoResponse(val favorito: Boolean)

@Serializable
data class MensajeResponse(val mensaje: String)
