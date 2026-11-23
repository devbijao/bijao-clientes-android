package app.bijao.clientes.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Bijao Comunidad -- espejo de `backend/routers/app_comunidad.py` y de la
// sección «Comunidad» de `frontend/lib/app-clientes/api.ts` en el monorepo
// (y de `Comunidad.swift` en iOS). Plan: MDs/plan_comunidad.md (D15–D19).
// Todo lo opcional lleva valor por defecto: una pregunta oculta solo trae
// `orden` y `estado`, y kotlinx falla si falta un campo sin default.

@Serializable
data class EventoComunidad(
    val slug: String,
    val titulo: String,
    val descripcion: String? = null,
    val organizador: String? = null,
    @SerialName("fecha_inicio") val fechaInicio: String,
    val estado: String,
    @SerialName("minimo_premio") val minimoPremio: Int,
    @SerialName("n_preguntas") val nPreguntas: Int = 0,
    @SerialName("premio_titulo") val premioTitulo: String? = null,
    @SerialName("imagen_url") val imagenUrl: String? = null,
) {
    val enVivo: Boolean get() = estado == "en_vivo"
}

/** Fila de `GET /api/app/comunidad/eventos` (lista sin sobre `data`). */
@Serializable
data class MiEventoComunidad(
    val slug: String,
    val titulo: String,
    val organizador: String? = null,
    @SerialName("fecha_inicio") val fechaInicio: String,
    val estado: String,
    @SerialName("imagen_url") val imagenUrl: String? = null,
    val aprobadas: Int = 0,
    @SerialName("minimo_premio") val minimoPremio: Int,
    val cumple: Boolean = false,
    val participe: Boolean = false,
)

@Serializable
data class AvanceComunidad(val aprobadas: Int, val minimo: Int, val cumple: Boolean)

@Serializable
data class PremioComunidad(
    val titulo: String? = null,
    val descripcion: String? = null,
    @SerialName("como_reclamar") val comoReclamar: String? = null,
)

@Serializable
data class MensajeHiloComunidad(
    val id: String,
    val nombre: String,
    /** "pendiente" | "aprobada" | "rechazada" */
    val estado: String,
    val mia: Boolean = false,
    /** `null` en una rechazada ajena: el rechazo existe para lo que no debe quedar en pantalla. */
    val texto: String? = null,
    /** Solo llega en las propias. */
    val motivo: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class PreguntaComunidad(
    val orden: Int,
    /** "oculta" | "abierta" | "cerrada" */
    val estado: String,
    val id: String? = null,
    val texto: String? = null,
    val hilo: List<MensajeHiloComunidad> = emptyList(),
    @SerialName("total_hilo") val totalHilo: Int = 0,
    @SerialName("aprobadas_hilo") val aprobadasHilo: Int = 0,
    @SerialName("mis_envios") val misEnvios: Int = 0,
    @SerialName("envios_restantes") val enviosRestantes: Int = 0,
    @SerialName("ya_suma") val yaSuma: Boolean = false,
) {
    /** La oculta no tiene `id`: se identifica por su orden. */
    val clave: String get() = id ?: "orden-$orden"
}

/** `GET /api/app/comunidad/{slug}/estado?version=N`: o `{sin_cambios: true, version}`
 * o el estado completo. */
@Serializable
data class EstadoComunidad(
    @SerialName("sin_cambios") val sinCambios: Boolean = false,
    val version: Int? = null,
    val evento: EventoComunidad? = null,
    val avance: AvanceComunidad? = null,
    val premio: PremioComunidad? = null,
    val preguntas: List<PreguntaComunidad> = emptyList(),
)

@Serializable
data class ResponderComunidadInput(val texto: String)

@Serializable
data class RespuestaComunidadCreada(val id: String, val estado: String)
