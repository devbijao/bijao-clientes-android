package app.bijao.clientes.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Espejo de `NegocioDetalle.swift` -- `GET /api/app/negocios/{id}`. Sin
 * `tarjetas` todavía: las tarjetas de sellos llegan con la Fase A4. */
@Serializable
data class NegocioDetalle(
    val id: String,
    val nombre: String? = null,
    val slug: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("portada_url") val portadaUrl: String? = null,
    val descripcion: String? = null,
    val about: String? = null,
    @SerialName("tipo_negocio") val tipoNegocio: String? = null,
    val ciudad: String? = null,
    val direccion: String? = null,
    val whatsapp: String? = null,
    @SerialName("tienda_url") val tiendaUrl: String? = null,
    val rating: Double? = null,
    val resenas: Int = 0,
    val favorito: Boolean = false,
    @SerialName("en_directorio") val enDirectorio: Boolean = false,
    val vinculado: Boolean = false,
)

@Serializable
data class Resena(
    val autor: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val calificacion: Int,
    val comentario: String? = null,
    val respuesta: String? = null,
    @SerialName("respuesta_at") val respuestaAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("es_mia") val esMia: Boolean = false,
)

@Serializable
data class ResenasResponse(
    val data: List<Resena>,
    val promedio: Double? = null,
    val conteo: Int = 0,
    @SerialName("puede_resenar") val puedeResenar: Boolean = false,
    @SerialName("mi_resena") val miResena: Resena? = null,
    val disponible: Boolean = true,
)

@Serializable
data class DejarResenaInput(val calificacion: Int, val comentario: String? = null)
