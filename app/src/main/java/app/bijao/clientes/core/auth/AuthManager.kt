package app.bijao.clientes.core.auth

import android.content.Intent
import app.bijao.clientes.core.AppConfig
import app.bijao.clientes.core.models.PedirCodigoInput
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.flow.StateFlow

/**
 * Envuelve el [SupabaseClient] del proyecto AISLADO de Bijao Clientes y
 * expone el mismo flujo de entrada que ya está en producción en la web y en
 * iOS: OTP por SMS (Colombia) y código por correo. Equivalente directo de
 * `AuthManager.swift` en bijao-clientes-ios.
 *
 * `SettingsSessionManager()` persiste la sesión entre aperturas de la app
 * (SharedPreferences por debajo) -- el equivalente funcional del Keychain
 * que usa iOS, aunque sin cifrado adicional todavía; endurecer con
 * `EncryptedSharedPreferences` queda pendiente para una pasada de seguridad,
 * no bloquea el arranque.
 */
object AuthManager {
    val client = createSupabaseClient(AppConfig.SUPABASE_URL, AppConfig.SUPABASE_ANON_KEY) {
        install(Auth) {
            sessionManager = SettingsSessionManager()
            // `scheme`/`host` arman el redirect por defecto (`bijaoclientes://auth-callback`)
            // que usa cualquier `signInWith` de un OAuthProvider -- mismo valor que
            // `AppConfig.oauthRedirectURL` en iOS. PKCE (no el implícito por defecto
            // de la librería) es el que de verdad entrega un `code` en el deep link,
            // que es lo que espera `handleDeeplinks` más abajo.
            scheme = AppConfig.OAUTH_SCHEME
            host = AppConfig.OAUTH_HOST
            flowType = FlowType.PKCE
            // Custom Tabs en vez de saltar a una app de navegador aparte -- misma
            // sensación de "hoja dentro de la app" que da `ASWebAuthenticationSession`
            // en iOS, sin tener que agregar una dependencia nueva (ya la trae auth-kt).
            defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
        }
    }

    private val auth get() = client.auth

    /** Equivalente de `AuthManager.session` en iOS -- `Initializing` cubre el
     * `cargandoSesionInicial` que ahí era un booleano aparte. */
    val sessionStatus: StateFlow<SessionStatus> get() = auth.sessionStatus

    /** Token actual para llamar al backend FastAPI (`Authorization: Bearer ...`). */
    val accessToken: String? get() = auth.currentAccessTokenOrNull()

    /** Celular con el que se entró, si fue por SMS -- vacío si fue por correo.
     * Lo usa Onboarding para precargar/enviar el celular ya verificado, igual
     * que `auth.session?.user.phone` en iOS. */
    val celularSesion: String? get() = auth.currentUserOrNull()?.phone

    /** Nombre que trajo el proveedor (Google), para precargar «Tu nombre» en
     * el onboarding -- igual que `nombreDelProveedor` en iOS. Un «nombre» con
     * «@» es el correo que Google pone cuando la cuenta no tiene nombre: no
     * sirve, y el backend tampoco lo usa (alta del 25-nov). */
    val nombreDelProveedor: String?
        get() {
            val datos = auth.currentUserOrNull()?.userMetadata ?: return null
            return listOf("full_name", "name").firstNotNullOfOrNull { clave ->
                (datos[clave] as? kotlinx.serialization.json.JsonPrimitive)
                    ?.takeIf { it.isString }?.content?.trim()
                    ?.takeIf { it.isNotEmpty() && !it.contains("@") }
            }
        }

    // Pedir el código va por el backend (`POST /api/app/auth/codigo`), no por
    // `auth.signInWith(OTP)`: el proyecto de Auth de Clientes exige captcha
    // (Turnstile, para la web) y Supabase respondía `captcha_failed` a la app.
    // El backend lo pide con la llave de servicio y aplica sus propios topes.
    // Verificar el código sí sigue directo contra Supabase (no exige captcha).
    suspend fun enviarCodigoSMS(celular10Digitos: String) {
        ApiClient.service.pedirCodigo(PedirCodigoInput(celular = celular10Digitos)).datoOLanzar()
    }

    suspend fun verificarCodigoSMS(celular10Digitos: String, codigo: String) {
        val telefono = "${AppConfig.INDICATIVO_TELEFONO}$celular10Digitos"
        auth.verifyPhoneOtp(OtpType.Phone.SMS, telefono, codigo)
    }

    suspend fun enviarCodigoCorreo(email: String) {
        ApiClient.service.pedirCodigo(PedirCodigoInput(email = email)).datoOLanzar()
    }

    suspend fun verificarCodigoCorreo(email: String, codigo: String) {
        auth.verifyEmailOtp(OtpType.Email.EMAIL, email, codigo)
    }

    /** Equivalente de `iniciarSesionConGoogle()` en iOS. `signInWith(Google)`
     * abre el Custom Tab solo; la sesión queda importada cuando Android
     * entrega el deep link de vuelta -- ver [completarOAuthDesdeIntent]. */
    suspend fun iniciarSesionConGoogle() {
        auth.signInWith(Google)
    }

    /** Se llama desde `onCreate`/`onNewIntent` de `MainActivity` con el
     * intent que trae `bijaoclientes://auth-callback?code=...` -- la mitad
     * que en iOS resuelve solo `ASWebAuthenticationSession` y que acá hay
     * que capturar a mano porque Android no tiene ese callback integrado. */
    fun completarOAuthDesdeIntent(intent: Intent, onError: (Throwable) -> Unit) {
        client.handleDeeplinks(intent, onError = onError)
    }

    suspend fun salir() {
        auth.signOut()
    }

    /** Renueva el token (por un 401 o por «Reintentar»). Devuelve si funcionó; si
     * falla por red, la sesión guardada sigue ahí y Supabase lo reintenta solo. */
    suspend fun renovar(): Boolean = try {
        auth.refreshCurrentSession()
        true
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        false
    }
}
