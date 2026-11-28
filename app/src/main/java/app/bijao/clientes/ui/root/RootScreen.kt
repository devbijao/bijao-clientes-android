package app.bijao.clientes.ui.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.auth.AuthManager
import app.bijao.clientes.core.models.Perfil
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.ui.login.LoginScreen
import app.bijao.clientes.ui.onboarding.OnboardingScreen
import app.bijao.clientes.ui.principal.PrincipalScreen
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class Pantalla { CARGANDO, LOGIN, ONBOARDING, PRINCIPAL, SIN_CONEXION }

/**
 * Equivalente de `RootView.swift` en bijao-clientes-ios: decide Login vs.
 * Onboarding vs. Principal según la sesión de Supabase y la respuesta de
 * `GET /api/app/perfil` (428 = "hay que registrarse").
 */
@Composable
fun RootScreen() {
    val sessionStatus by AuthManager.sessionStatus.collectAsState()
    var pantalla by remember { mutableStateOf(Pantalla.CARGANDO) }
    var perfil by remember { mutableStateOf<Perfil?>(null) }
    var negociosVinculados by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    var intento by remember { mutableStateOf(0) }

    /** Regla (pedido de Harold, 2026-11-28): **nunca mandar a entrar por un error**.
     * Antes, `RefreshFailure` (renovar el token sin red) y cualquier falla al pedir
     * el perfil (red caída, Render despertando, un 500) mostraban el login aunque
     * la sesión seguía guardada, y el estado cambia cada vez que el token se
     * renueva (cada hora): a la gente «se le cerraba la sesión» sola. Ahora, si ya
     * estaba adentro se queda; si no, ve «Sin conexión» con Reintentar. */
    fun falla() {
        if (pantalla == Pantalla.PRINCIPAL && perfil != null) return
        pantalla = Pantalla.SIN_CONEXION
    }

    LaunchedEffect(sessionStatus, intento) {
        when (sessionStatus) {
            is SessionStatus.Initializing -> if (pantalla != Pantalla.PRINCIPAL) pantalla = Pantalla.CARGANDO
            is SessionStatus.NotAuthenticated -> pantalla = Pantalla.LOGIN
            // Sin red al renovar: la sesión sigue guardada y Supabase reintenta solo.
            is SessionStatus.RefreshFailure -> falla()
            is SessionStatus.Authenticated -> {
                try {
                    var respuesta = ApiClient.service.perfil()
                    if (respuesta.code() == 401 && AuthManager.renovar()) respuesta = ApiClient.service.perfil()
                    when {
                        respuesta.code() == 428 -> pantalla = Pantalla.ONBOARDING
                        respuesta.isSuccessful -> {
                            perfil = respuesta.body()?.data
                            pantalla = Pantalla.PRINCIPAL
                        }
                        else -> falla()
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    falla()
                }
            }
        }
    }

    when (pantalla) {
        Pantalla.CARGANDO -> Centrado { CircularProgressIndicator() }
        Pantalla.LOGIN -> LoginScreen()
        Pantalla.SIN_CONEXION -> SinConexion(
            onReintentar = { pantalla = Pantalla.CARGANDO; intento++ },
            onCerrarSesion = { scope.launch { AuthManager.salir() } },
        )
        Pantalla.ONBOARDING -> OnboardingScreen(celularSesion = AuthManager.celularSesion ?: "") { nuevoPerfil, vinculados ->
            perfil = nuevoPerfil
            negociosVinculados = vinculados
            pantalla = Pantalla.PRINCIPAL
        }
        Pantalla.PRINCIPAL -> perfil?.let { perfilActual ->
            PrincipalScreen(
                perfil = perfilActual,
                negociosVinculadosAlRegistrarse = negociosVinculados,
                onToastVinculadosMostrado = { negociosVinculados = 0 },
                onPerfilActualizado = { perfil = it },
            )
        }
    }
}

@Composable
private fun Centrado(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        content()
    }
}

/** Hay sesión pero no se pudo cargar la cuenta (sin red, servidor caído). No se
 * cierra la sesión: se ofrece reintentar, y cerrar sesión solo si la persona quiere. */
@Composable
private fun SinConexion(onReintentar: () -> Unit, onCerrarSesion: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.WifiOff, contentDescription = null, modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("No pudimos conectarnos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp))
        Text("Revisa tu conexión a internet. Tu sesión sigue abierta.", textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = onReintentar, modifier = Modifier.fillMaxWidth().padding(top = 24.dp).heightIn(min = 50.dp)) {
            Text("Reintentar", fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onCerrarSesion, modifier = Modifier.padding(top = 24.dp)) {
            Text("Cerrar sesión", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
