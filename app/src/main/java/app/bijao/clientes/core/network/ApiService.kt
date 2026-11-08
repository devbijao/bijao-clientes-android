package app.bijao.clientes.core.network

import app.bijao.clientes.core.models.AvatarResponse
import app.bijao.clientes.core.models.AvisoPagoInput
import app.bijao.clientes.core.models.AvisoPagoResponse
import app.bijao.clientes.core.models.AvisosPagoResponse
import app.bijao.clientes.core.models.BeneficiosResponse
import app.bijao.clientes.core.models.BloqueosResponse
import app.bijao.clientes.core.models.BusquedaResponse
import app.bijao.clientes.core.models.CanjeInput
import app.bijao.clientes.core.models.CanjeResponse
import app.bijao.clientes.core.models.CategoriasResponse
import app.bijao.clientes.core.models.CompraDetalle
import app.bijao.clientes.core.models.ContactosResponse
import app.bijao.clientes.core.models.CrearCuentaResponse
import app.bijao.clientes.core.models.CuentaInput
import app.bijao.clientes.core.models.CuentasCompartidasResponse
import app.bijao.clientes.core.models.DejarResenaInput
import app.bijao.clientes.core.models.DetalleCreditoResponse
import app.bijao.clientes.core.models.ListaCreditos
import app.bijao.clientes.core.models.EstadoSolicitudResponse
import app.bijao.clientes.core.models.DirectorioResponse
import app.bijao.clientes.core.models.Envelope
import app.bijao.clientes.core.models.EstadoParteInput
import app.bijao.clientes.core.models.EstadoParteResponse
import app.bijao.clientes.core.models.FavoritoResponse
import app.bijao.clientes.core.models.FavoritosResponse
import app.bijao.clientes.core.models.HistorialResponse
import app.bijao.clientes.core.models.MensajeResponse
import app.bijao.clientes.core.models.MiUsuarioResponse
import app.bijao.clientes.core.models.MisCuentasResponse
import app.bijao.clientes.core.models.MovimientoInput
import app.bijao.clientes.core.models.MovimientoResponse
import app.bijao.clientes.core.models.NegocioDetalle
import app.bijao.clientes.core.models.NotificacionesResponse
import app.bijao.clientes.core.models.OkResponse
import app.bijao.clientes.core.models.Perfil
import app.bijao.clientes.core.models.PreferenciaCobroInput
import app.bijao.clientes.core.models.PreferenciaCobroResponse
import app.bijao.clientes.core.models.PreferenciasCobroResponse
import app.bijao.clientes.core.models.RestriccionInput
import app.bijao.clientes.core.models.RestriccionResponse
import app.bijao.clientes.core.models.SinLeerResponse
import app.bijao.clientes.core.models.PerfilUpdateInput
import app.bijao.clientes.core.models.QRIdentidad
import app.bijao.clientes.core.models.RegistroInput
import app.bijao.clientes.core.models.RegistroResponse
import app.bijao.clientes.core.models.ResenasResponse
import app.bijao.clientes.core.models.SolicitudInput
import app.bijao.clientes.core.models.SolicitudesResponse
import app.bijao.clientes.core.models.UsuarioGuardadoResponse
import app.bijao.clientes.core.models.UsuarioInput
import app.bijao.clientes.core.models.ValesResponse
import app.bijao.clientes.core.models.WalletResponse
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

    @GET("api/app/notificaciones")
    suspend fun notificaciones(@Query("limite") limite: Int = 30): Response<NotificacionesResponse>

    @POST("api/app/notificaciones/leer")
    suspend fun marcarNotificacionesLeidas(): Response<SinLeerResponse>

    // ── Dividir cuenta (D5) ──────────────────────────────────────────────

    @POST("api/app/cuentas")
    suspend fun crearCuenta(@Body body: CuentaInput): Response<CrearCuentaResponse>

    @GET("api/app/cuentas")
    suspend fun misCuentas(): Response<MisCuentasResponse>

    @PUT("api/app/cuentas/{codigo}/partes/{parteId}")
    suspend fun marcarParteCuenta(
        @Path("codigo") codigo: String,
        @Path("parteId") parteId: String,
        @Body body: EstadoParteInput,
    ): Response<EstadoParteResponse>

    @DELETE("api/app/cuentas/{codigo}")
    suspend fun eliminarCuentaDividida(@Path("codigo") codigo: String): Response<OkResponse>

    @GET("api/app/cuentas-compartidas")
    suspend fun cuentasCompartidas(): Response<CuentasCompartidasResponse>

    // ── Wallet (W3) ──────────────────────────────────────────────────────

    @GET("api/app/wallet")
    suspend fun wallet(
        @Query("periodo") periodo: String = "auto",
        @Query("fecha") fecha: String? = null,
    ): Response<WalletResponse>

    @POST("api/app/wallet/movimientos")
    suspend fun agregarMovimientoWallet(@Body body: MovimientoInput): Response<MovimientoResponse>

    @DELETE("api/app/wallet/movimientos/{id}")
    suspend fun eliminarMovimientoWallet(@Path("id") id: String): Response<OkResponse>

    // ── Contactos (S1) ───────────────────────────────────────────────────

    @GET("api/app/contactos/mi-usuario")
    suspend fun miUsuario(): Response<MiUsuarioResponse>

    @PUT("api/app/contactos/mi-usuario")
    suspend fun elegirUsuario(@Body body: UsuarioInput): Response<UsuarioGuardadoResponse>

    @PUT("api/app/contactos/restriccion")
    suspend fun restringirPerfil(@Body body: RestriccionInput): Response<RestriccionResponse>

    @GET("api/app/contactos/buscar")
    suspend fun buscarPersonas(@Query("q") q: String): Response<BusquedaResponse>

    @GET("api/app/contactos")
    suspend fun misContactos(): Response<ContactosResponse>

    @GET("api/app/contactos/solicitudes")
    suspend fun misSolicitudes(): Response<SolicitudesResponse>

    @POST("api/app/contactos/solicitudes")
    suspend fun pedirConexion(@Body body: SolicitudInput): Response<EstadoSolicitudResponse>

    @POST("api/app/contactos/solicitudes/{id}/aceptar")
    suspend fun aceptarSolicitud(@Path("id") id: String): Response<EstadoSolicitudResponse>

    @DELETE("api/app/contactos/solicitudes/{id}")
    suspend fun quitarSolicitud(@Path("id") id: String): Response<OkResponse>

    @DELETE("api/app/contactos/{id}")
    suspend fun eliminarContacto(@Path("id") id: String): Response<OkResponse>

    @GET("api/app/contactos/bloqueos")
    suspend fun misBloqueos(): Response<BloqueosResponse>

    @POST("api/app/contactos/bloqueos")
    suspend fun bloquearPersona(@Body body: SolicitudInput): Response<OkResponse>

    @DELETE("api/app/contactos/bloqueos/{id}")
    suspend fun desbloquearPersona(@Path("id") id: String): Response<OkResponse>

    // ── Créditos (F7) ────────────────────────────────────────────────────

    @GET("api/app/creditos")
    suspend fun creditos(@Query("estado") estado: String = "PENDIENTE"): Response<ListaCreditos>

    @GET("api/app/creditos/{id}")
    suspend fun creditoDetalle(@Path("id") id: String): Response<DetalleCreditoResponse>

    @GET("api/app/avisos-pago")
    suspend fun avisosPago(@Query("credito_id") creditoId: String): Response<AvisosPagoResponse>

    @POST("api/app/avisos-pago")
    suspend fun avisarPago(@Body body: AvisoPagoInput): Response<AvisoPagoResponse>

    @DELETE("api/app/avisos-pago/{id}")
    suspend fun cancelarAvisoPago(@Path("id") id: String, @Query("credito_id") creditoId: String): Response<OkResponse>

    @GET("api/app/preferencias-credito")
    suspend fun preferenciasCobro(): Response<PreferenciasCobroResponse>

    @PUT("api/app/preferencias-credito/{negocioId}")
    suspend fun guardarPreferenciaCobro(@Path("negocioId") negocioId: String, @Body body: PreferenciaCobroInput): Response<PreferenciaCobroResponse>
}
