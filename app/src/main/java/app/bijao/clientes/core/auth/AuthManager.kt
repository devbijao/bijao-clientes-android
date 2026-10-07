package app.bijao.clientes.core.auth

import android.content.Intent
import app.bijao.clientes.core.AppConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.OTP
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

    suspend fun enviarCodigoSMS(celular10Digitos: String) {
        val telefono = "${AppConfig.INDICATIVO_TELEFONO}$celular10Digitos"
        auth.signInWith(OTP) { phone = telefono }
    }

    suspend fun verificarCodigoSMS(celular10Digitos: String, codigo: String) {
        val telefono = "${AppConfig.INDICATIVO_TELEFONO}$celular10Digitos"
        auth.verifyPhoneOtp(OtpType.Phone.SMS, telefono, codigo)
    }

    suspend fun enviarCodigoCorreo(email: String) {
        auth.signInWith(OTP) { this.email = email }
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
}
