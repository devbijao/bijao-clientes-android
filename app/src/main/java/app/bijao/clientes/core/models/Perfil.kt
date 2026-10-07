package app.bijao.clientes.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Espejo de `_perfil_publico()` en `backend/routers/app_clientes.py` (monorepo
 * principal) -- mismo shape que `Perfil.swift` en bijao-clientes-ios.
 * Mantener los tres en sync si cambia el backend.
 */
@Serializable
data class Perfil(
    val id: String,
    val email: String? = null,
    val nombre: String? = null,
    val celular: String? = null,
    @SerialName("celular_verificado") val celularVerificado: Boolean = false,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val intereses: List<String> = emptyList(),
    val ciudad: String? = null,
    @SerialName("consentimiento_datos") val consentimientoDatos: Boolean = false,
)

@Serializable
data class Envelope<T>(val data: T)

@Serializable
data class RegistroInput(
    val nombre: String? = null,
    val celular: String? = null,
    val intereses: List<String> = emptyList(),
    val ciudad: String? = null,
    @SerialName("consentimiento_datos") val consentimientoDatos: Boolean,
)

@Serializable
data class RegistroResponse(
    val data: Perfil,
    @SerialName("negocios_vinculados") val negociosVinculados: Int? = null,
)

/** El celular se queda afuera a propósito: cambiarlo tiene reglas especiales
 * (invalida la verificación por SMS) que esta pantalla no necesita cubrir. */
@Serializable
data class PerfilUpdateInput(
    val nombre: String? = null,
    val intereses: List<String>? = null,
    val ciudad: String? = null,
)

@Serializable
data class AvatarResponse(@SerialName("avatar_url") val avatarUrl: String? = null)
