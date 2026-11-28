package app.bijao.clientes

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import app.bijao.clientes.core.seguridad.BloqueoApp
import app.bijao.clientes.core.seguridad.PantallaBloqueo
import io.github.jan.supabase.auth.status.SessionStatus
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.bijao.clientes.core.auth.AuthManager
import app.bijao.clientes.ui.root.RootScreen
import app.bijao.clientes.ui.theme.BijaoClientesTheme

/** `FragmentActivity` (que es una `ComponentActivity`) porque `BiometricPrompt`
 * la necesita para el bloqueo con huella (F4). */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        completarOAuthSiAplica(intent)
        // Solo en el arranque "de verdad": al recrearse la Activity (rotar,
        // modo oscuro) llega el mismo intent y reabriría el evento.
        if (savedInstanceState == null) EnlacesEntrantes.abrir(intent)
        BloqueoApp.iniciar(this)
        setContent {
            BijaoClientesTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val bloqueada by BloqueoApp.bloqueada.collectAsState()
                    val activado by BloqueoApp.activado.collectAsState()
                    val sesion by AuthManager.sessionStatus.collectAsState()
                    val conSesion = sesion is SessionStatus.Authenticated
                    LaunchedEffect(conSesion) { if (!conSesion && sesion !is SessionStatus.Initializing) BloqueoApp.sinSesion() }
                    // Con el bloqueo activo, la miniatura del selector de apps no
                    // muestra saldos ni contactos (Android 13+).
                    LaunchedEffect(activado) {
                        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(!activado)
                    }
                    Box(Modifier.fillMaxSize()) {
                        RootScreen()
                        if (conSesion && bloqueada) PantallaBloqueo()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        BloqueoApp.alVolver()
    }

    override fun onStop() {
        super.onStop()
        BloqueoApp.alIrAFondo()
    }

    // `launchMode="singleTask"` hace que el deep link de vuelta de Google (y
    // el App Link de un evento de Comunidad con la app ya abierta) reuse esta
    // misma Activity en vez de crear una nueva -- por eso el intent llega acá
    // y no a un `onCreate` nuevo.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        completarOAuthSiAplica(intent)
        EnlacesEntrantes.abrir(intent)
    }

    private fun completarOAuthSiAplica(intent: Intent) {
        AuthManager.completarOAuthDesdeIntent(intent) { error ->
            Log.e("OAuth", "No se pudo completar el login con Google", error)
        }
    }
}
