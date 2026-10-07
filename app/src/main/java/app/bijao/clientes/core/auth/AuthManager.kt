package app.bijao.clientes.core.auth

import app.bijao.clientes.core.AppConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
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

    suspend fun salir() {
        auth.signOut()
    }
}
