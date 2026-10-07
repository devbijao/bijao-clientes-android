package app.bijao.clientes.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response

/** Equivalente de `APIError` en bijao-clientes-ios -- `estado` es lo que
 * `RootScreen` necesita leer para distinguir un 428 real de un error normal. */
class ApiException(val estado: Int, mensaje: String) : Exception(mensaje)

private val json = Json { ignoreUnknownKeys = true }

/**
 * Equivalente de `APIClient.ejecutar()` en iOS: desenvuelve el body en éxito,
 * o lanza [ApiException] con el mensaje real del backend en error.
 */
fun <T> Response<T>.datoOLanzar(): T {
    if (isSuccessful) return body() ?: error("Respuesta vacía del servidor")
    throw ApiException(code(), extraerMensajeError(errorBody()?.string(), code()))
}

/** FastAPI manda `detail` como string para un `HTTPException` normal, pero
 * como lista de objetos `{msg, loc, type}` en un 422 de validación de
 * Pydantic -- mismo caso que ya resolvió `APIClient.swift`. */
private fun extraerMensajeError(cuerpo: String?, estado: Int): String {
    if (cuerpo == null) return "Error del servidor ($estado)"
    return try {
        val detail = json.parseToJsonElement(cuerpo).jsonObject["detail"] ?: return "Error del servidor ($estado)"
        detail.jsonPrimitive.contentOrNull
            ?: detail.jsonArray.firstOrNull()?.jsonObject?.get("msg")?.jsonPrimitive?.contentOrNull
            ?: "Error del servidor ($estado)"
    } catch (e: Exception) {
        "Error del servidor ($estado)"
    }
}
