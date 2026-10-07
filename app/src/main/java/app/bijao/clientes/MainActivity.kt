package app.bijao.clientes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.bijao.clientes.core.auth.AuthManager
import app.bijao.clientes.ui.root.RootScreen
import app.bijao.clientes.ui.theme.BijaoClientesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        completarOAuthSiAplica(intent)
        setContent {
            BijaoClientesTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootScreen()
                }
            }
        }
    }

    // `launchMode="singleTask"` hace que el deep link de vuelta de Google
    // reuse esta misma Activity en vez de crear una nueva -- por eso el
    // intent llega acá y no a un `onCreate` nuevo.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        completarOAuthSiAplica(intent)
    }

    private fun completarOAuthSiAplica(intent: Intent) {
        AuthManager.completarOAuthDesdeIntent(intent) { error ->
            Log.e("OAuth", "No se pudo completar el login con Google", error)
        }
    }
}
