package app.bijao.clientes.core.network

import app.bijao.clientes.core.models.AvatarResponse
import app.bijao.clientes.core.models.BeneficiosResponse
import app.bijao.clientes.core.models.CanjeInput
import app.bijao.clientes.core.models.CanjeResponse
import app.bijao.clientes.core.models.CategoriasResponse
import app.bijao.clientes.core.models.CompraDetalle
import app.bijao.clientes.core.models.DejarResenaInput
import app.bijao.clientes.core.models.DirectorioResponse
import app.bijao.clientes.core.models.Envelope
import app.bijao.clientes.core.models.FavoritoResponse
import app.bijao.clientes.core.models.FavoritosResponse
import app.bijao.clientes.core.models.HistorialResponse
import app.bijao.clientes.core.models.MensajeResponse
import app.bijao.clientes.core.models.NegocioDetalle
import app.bijao.clientes.core.models.Perfil
import app.bijao.clientes.core.models.PerfilUpdateInput
import app.bijao.clientes.core.models.QRIdentidad
import app.bijao.clientes.core.models.RegistroInput
import app.bijao.clientes.core.models.RegistroResponse
import app.bijao.clientes.core.models.ResenasResponse
import app.bijao.clientes.core.models.ValesResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Llamadas bajo `/api/app` -- equivalente de `AppClientesAPI.swift` en
 * bijao-clientes-ios. Se usa `Response<T>` (no el valor desenvuelto) porque
 * necesitamos leer el código HTTP a mano: un 428 significa "hay que
 * registrarse", no es un error de verdad (ver `RootScreen`).
 */
interface ApiService {
    @GET("api/app/perfil")
    suspend fun perfil(): Response<Envelope<Perfil>>

    @POST("api/app/registro")
    suspend fun registrar(@Body body: RegistroInput): Response<RegistroResponse>

    @GET("api/app/directorio")
    suspend fun directorio(
        @Query("q") q: String? = null,
        @Query("categoria") categoria: String? = null,
        @Query("ciudad") ciudad: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("desde") desde: Int = 0,
        @Query("limite") limite: Int = 30,
    ): Response<DirectorioResponse>

    @GET("api/app/categorias")
    suspend fun categorias(): Response<CategoriasResponse>

    @POST("api/app/favoritos/{negocioId}")
    suspend fun marcarFavorito(@Path("negocioId") negocioId: String): Response<FavoritoResponse>

    @DELETE("api/app/favoritos/{negocioId}")
    suspend fun quitarFavorito(@Path("negocioId") negocioId: String): Response<FavoritoResponse>

    @GET("api/app/favoritos")
    suspend fun favoritos(): Response<FavoritosResponse>

    @GET("api/app/negocios/{id}")
    suspend fun negocioDetalle(@Path("id") id: String): Response<Envelope<NegocioDetalle>>

    @GET("api/app/negocios/{id}/resenas")
    suspend fun resenas(@Path("id") id: String): Response<ResenasResponse>

    @PUT("api/app/negocios/{id}/resenas")
    suspend fun dejarResena(@Path("id") id: String, @Body body: DejarResenaInput): Response<MensajeResponse>

    @GET("api/app/beneficios")
    suspend fun beneficios(): Response<BeneficiosResponse>

    @POST("api/app/canjear")
    suspend fun canjear(@Body body: CanjeInput): Response<CanjeResponse>

    @GET("api/app/vales")
    suspend fun vales(@Query("estado") estado: String? = null): Response<ValesResponse>

    @POST("api/app/qr")
    suspend fun generarQR(): Response<Envelope<QRIdentidad>>

    @PUT("api/app/perfil")
    suspend fun actualizarPerfil(@Body body: PerfilUpdateInput): Response<RegistroResponse>

    @Multipart
    @POST("api/app/perfil/avatar")
    suspend fun subirAvatar(@Part file: MultipartBody.Part): Response<AvatarResponse>

    @DELETE("api/app/perfil/avatar")
    suspend fun quitarAvatar(): Response<AvatarResponse>

    @GET("api/app/historial")
    suspend fun historial(
        @Query("desde") desde: Int = 0,
        @Query("limite") limite: Int = 25,
    ): Response<HistorialResponse>

    @GET("api/app/historial/{id}")
    suspend fun compraDetalle(@Path("id") id: String): Response<Envelope<CompraDetalle>>
}
