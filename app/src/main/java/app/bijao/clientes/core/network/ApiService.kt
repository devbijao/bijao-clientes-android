package app.bijao.clientes.core.network

import app.bijao.clientes.core.models.Envelope
import app.bijao.clientes.core.models.Perfil
import retrofit2.Response
import retrofit2.http.GET

/**
 * Llamadas bajo `/api/app` -- equivalente de `AppClientesAPI.swift` en
 * bijao-clientes-ios. Se usa `Response<T>` (no el valor desenvuelto) porque
 * necesitamos leer el código HTTP a mano: un 428 significa "hay que
 * registrarse", no es un error de verdad (ver `RootScreen`).
 */
interface ApiService {
    @GET("api/app/perfil")
    suspend fun perfil(): Response<Envelope<Perfil>>
}
