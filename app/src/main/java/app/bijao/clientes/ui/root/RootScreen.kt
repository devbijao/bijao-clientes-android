package app.bijao.clientes.ui.root

import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.auth.AuthManager
import app.bijao.clientes.core.models.Perfil
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.ui.login.LoginScreen
import app.bijao.clientes.ui.onboarding.OnboardingScreen
import app.bijao.clientes.ui.principal.PrincipalScreen
import io.github.jan.supabase.auth.status.SessionStatus

private enum class Pantalla { CARGANDO, LOGIN, ONBOARDING, PRINCIPAL }

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

    LaunchedEffect(sessionStatus) {
        when (sessionStatus) {
            is SessionStatus.Initializing -> pantalla = Pantalla.CARGANDO
            is SessionStatus.NotAuthenticated -> pantalla = Pantalla.LOGIN
            is SessionStatus.RefreshFailure -> pantalla = Pantalla.LOGIN
            is SessionStatus.Authenticated -> {
                try {
                    val respuesta = ApiClient.service.perfil()
                    when {
                        respuesta.code() == 428 -> pantalla = Pantalla.ONBOARDING
                        respuesta.isSuccessful -> {
                            perfil = respuesta.body()?.data
                            pantalla = Pantalla.PRINCIPAL
                        }
                        else -> pantalla = Pantalla.LOGIN
                    }
                } catch (e: Exception) {
                    pantalla = Pantalla.LOGIN
                }
            }
        }
    }

    when (pantalla) {
        Pantalla.CARGANDO -> Centrado { CircularProgressIndicator() }
        Pantalla.LOGIN -> LoginScreen()
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
