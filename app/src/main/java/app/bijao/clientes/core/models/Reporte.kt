package app.bijao.clientes.core.models

import kotlinx.serialization.Serializable

/** `POST /api/app/reportes`: reportar una persona (por su @) o una reseña (por su `ref`). */
@Serializable
data class ReporteInput(
    val tipo: String,
    val motivo: String,
    val detalle: String? = null,
    val usuario: String? = null,
    val negocio: String? = null,
    val resena: String? = null,
)

@Serializable
data class ReporteRespuesta(val ok: Boolean = true, val mensaje: String = "")
