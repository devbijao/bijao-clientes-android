package app.bijao.clientes.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Espejo de `backend/routers/app_contactos.py` -- Contactos (Fase S1): @usuario,
 * solicitudes con aceptación y bloqueos. Ver MDs/handoff_apps_nativas_contactos.md.
 * De otra persona nunca se ve más que nombre, @ y foto.
 */
@Serializable
data class MiUsuarioResponse(val usuario: String?, val restringido: Boolean)

@Serializable
data class UsuarioInput(val usuario: String)

@Serializable
data class UsuarioGuardadoResponse(val usuario: String)

@Serializable
data class RestriccionInput(val restringido: Boolean)

@Serializable
data class RestriccionResponse(val restringido: Boolean)

/** Fila de `GET /contactos/buscar`. `relacion`: NINGUNA | ENVIADA | RECIBIDA | CONTACTO. */
@Serializable
data class PersonaBusqueda(
    val nombre: String?,
    val usuario: String?,
    @SerialName("avatar_url") val avatarUrl: String?,
    val relacion: String,
)

@Serializable
data class BusquedaResponse(val data: List<PersonaBusqueda>)

/** Fila de "Mis contactos" (`GET /contactos`) y de "Bloqueados" (`GET /contactos/bloqueos`). */
@Serializable
data class ContactoItem(
    val id: String,
    val nombre: String?,
    val usuario: String?,
    @SerialName("avatar_url") val avatarUrl: String?,
    val desde: String,
)

@Serializable
data class ContactosResponse(val data: List<ContactoItem>)

@Serializable
data class BloqueosResponse(val data: List<ContactoItem>)

/** Fila de "Solicitudes" -- mismo shape que [ContactoItem] pero con `creadaEn` en vez de `desde`. */
@Serializable
data class SolicitudItem(
    val id: String,
    val nombre: String?,
    val usuario: String?,
    @SerialName("avatar_url") val avatarUrl: String?,
    @SerialName("creada_en") val creadaEn: String,
)

@Serializable
data class SolicitudesResponse(val recibidas: List<SolicitudItem>, val enviadas: List<SolicitudItem>)

/** Body de `POST /contactos/solicitudes` y `POST /contactos/bloqueos` -- el @ de la otra persona. */
@Serializable
data class SolicitudInput(val usuario: String)

/** "ENVIADA" | "ACEPTADA" */
@Serializable
data class EstadoSolicitudResponse(val estado: String)
