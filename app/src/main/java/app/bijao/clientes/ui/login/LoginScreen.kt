package app.bijao.clientes.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.auth.AuthManager
import app.bijao.clientes.core.auth.traducirErrorAuth
import kotlinx.coroutines.launch

private enum class Metodo { CELULAR, CORREO }

/**
 * Equivalente de `LoginView.swift` en bijao-clientes-ios: OTP por SMS
 * (Colombia) o código por correo, selector segmentado. Google OAuth queda
 * para una pasada siguiente dentro de la misma Fase A1 -- en Android no es
 * el mismo `ASWebAuthenticationSession` de una línea que en iOS, necesita
 * su propio flujo (Credential Manager o Custom Tabs) y no vale la pena
 * meterlo a medias en el primer slice.
 */
@Composable
fun LoginScreen() {
    var metodo by remember { mutableStateOf(Metodo.CELULAR) }
    var identificador by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var enviado by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Ingresa ahora mismo", style = MaterialTheme.typography.headlineMedium)
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 24.dp))

        if (!enviado) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = metodo == Metodo.CELULAR,
                    onClick = { metodo = Metodo.CELULAR; error = null },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Celular") }
                SegmentedButton(
                    selected = metodo == Metodo.CORREO,
                    onClick = { metodo = Metodo.CORREO; error = null },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Correo") }
            }

            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))

            OutlinedTextField(
                value = identificador,
                onValueChange = { identificador = it },
                label = { Text(if (metodo == Metodo.CELULAR) "Tu celular (10 dígitos)" else "Tu correo") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (metodo == Metodo.CELULAR) KeyboardType.Phone else KeyboardType.Email,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let {
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))

            Button(
                onClick = {
                    cargando = true
                    error = null
                    scope.launch {
                        try {
                            if (metodo == Metodo.CELULAR) {
                                AuthManager.enviarCodigoSMS(identificador)
                            } else {
                                AuthManager.enviarCodigoCorreo(identificador)
                            }
                            enviado = true
                        } catch (e: Exception) {
                            error = traducirErrorAuth(e).mensaje
                        } finally {
                            cargando = false
                        }
                    }
                },
                enabled = !cargando && identificador.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (cargando) CircularProgressIndicator(modifier = Modifier.padding(2.dp)) else Text("Enviar código")
            }
        } else {
            Text(
                if (metodo == Metodo.CELULAR) "Revisa tus mensajes" else "Revisa tu correo",
                style = MaterialTheme.typography.titleMedium,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))

            OutlinedTextField(
                value = codigo,
                onValueChange = { codigo = it },
                label = { Text("Código de 6 dígitos") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let {
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))

            Button(
                onClick = {
                    cargando = true
                    error = null
                    scope.launch {
                        try {
                            if (metodo == Metodo.CELULAR) {
                                AuthManager.verificarCodigoSMS(identificador, codigo)
                            } else {
                                AuthManager.verificarCodigoCorreo(identificador, codigo)
                            }
                            // La sesión queda establecida por supabase-kt; RootScreen
                            // reacciona solo al cambio de sessionStatus.
                        } catch (e: Exception) {
                            error = traducirErrorAuth(e).mensaje
                        } finally {
                            cargando = false
                        }
                    }
                },
                enabled = !cargando && codigo.length == 6,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (cargando) CircularProgressIndicator(modifier = Modifier.padding(2.dp)) else Text("Verificar")
            }

            TextButton(onClick = { enviado = false; codigo = ""; error = null }) {
                Text(if (metodo == Metodo.CELULAR) "Usar otro número" else "Usar otro correo")
            }
        }
    }
}
