package app.bijao.clientes.core.seguridad

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Bloqueo opcional de la app con huella o rostro (F4 de
 * `MDs/plan_apps_nativas_previo_revision.md`), con el PIN o patrón del teléfono
 * de respaldo. La sesión de Supabase sigue guardada: esto solo decide si se
 * muestra la app. Equivalente de `BloqueoApp.swift`.
 *
 * Se bloquea al abrir y al volver después de [GRACIA_MS] en segundo plano.
 */
object BloqueoApp {
    /** Volver antes de un minuto (contestar un mensaje, copiar un código) no pide otra vez. */
    private const val GRACIA_MS = 60_000L
    private const val PREFS = "bijao_clientes"
    private const val CLAVE = "bloqueo_biometrico"

    // BIOMETRIC_WEAK | DEVICE_CREDENTIAL es la combinación que funciona en todas
    // las versiones de Android (STRONG | DEVICE_CREDENTIAL no existe antes de la 30).
    private const val AUTENTICADORES = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    private val _activado = MutableStateFlow(false)
    val activado: StateFlow<Boolean> = _activado
    private val _bloqueada = MutableStateFlow(false)
    val bloqueada: StateFlow<Boolean> = _bloqueada
    private var enFondoDesde: Long? = null
    private var iniciado = false

    /** Se llama una vez al crear la Activity: si está activado, arranca bloqueada. */
    fun iniciar(context: Context) {
        if (iniciado) return
        iniciado = true
        _activado.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(CLAVE, false)
        _bloqueada.value = _activado.value
    }

    fun disponible(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTENTICADORES) == BiometricManager.BIOMETRIC_SUCCESS

    fun alIrAFondo() {
        if (enFondoDesde == null) enFondoDesde = System.currentTimeMillis()
    }

    fun alVolver() {
        val desde = enFondoDesde
        if (_activado.value && desde != null && System.currentTimeMillis() - desde > GRACIA_MS) _bloqueada.value = true
        enFondoDesde = null
    }

    /** Sin sesión no hay nada que proteger: quien acaba de entrar no desbloquea otra vez. */
    fun sinSesion() {
        _bloqueada.value = false
    }

    suspend fun desbloquear(activity: FragmentActivity): Boolean {
        val ok = autenticar(activity, "Desbloquea Bijao", "Para proteger tus datos")
        if (ok) _bloqueada.value = false
        return ok
    }

    /** Activar pide autenticarse primero: así se comprueba que funciona. */
    suspend fun activar(activity: FragmentActivity): Boolean {
        if (!autenticar(activity, "Activa el bloqueo de Bijao", null)) return false
        guardar(activity, true)
        return true
    }

    fun desactivar(context: Context) {
        guardar(context, false)
        _bloqueada.value = false
    }

    private fun guardar(context: Context, valor: Boolean) {
        _activado.value = valor
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(CLAVE, valor).apply()
    }

    private suspend fun autenticar(activity: FragmentActivity, titulo: String, subtitulo: String?): Boolean =
        suspendCancellableCoroutine { cont ->
            val prompt = BiometricPrompt(
                activity, ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (cont.isActive) cont.resume(false)
                    }
                    // onAuthenticationFailed = un intento que no coincidió; el diálogo sigue abierto.
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(titulo)
                .apply { subtitulo?.let { setSubtitle(it) } }
                .setAllowedAuthenticators(AUTENTICADORES)
                .build()
            prompt.authenticate(info)
            cont.invokeOnCancellation { prompt.cancelAuthentication() }
        }
}
