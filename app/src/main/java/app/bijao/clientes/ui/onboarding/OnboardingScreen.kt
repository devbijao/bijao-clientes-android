package app.bijao.clientes.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.Perfil
import app.bijao.clientes.core.models.RegistroInput
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.ApiException
import app.bijao.clientes.core.network.datoOLanzar
import kotlinx.coroutines.launch

/** Mismas categorías que el onboarding web (`bienvenida/page.tsx`) y que
 * `FlexibleChips.swift` en iOS. */
val INTERESES = listOf("Barberías", "Restaurantes", "Belleza", "Tecnología", "Salud", "Moda")

/**
 * Equivalente de `OnboardingView.swift`: alta en `app_usuarios` tras el login
 * OTP. El celular ya quedó probado por el SMS cuando se entró por ahí, así
 * que no se vuelve a pedir -- solo nombre, ciudad, intereses y
 * consentimiento, que es lo que el backend exige de verdad
 * (`POST /api/app/registro` responde 400 sin el consentimiento).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(celularSesion: String, onRegistrado: (Perfil, Int) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var ciudad by remember { mutableStateOf("") }
    var intereses by remember { mutableStateOf(setOf<String>()) }
    var consiente by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mostrandoSelectorCiudad by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (mostrandoSelectorCiudad) {
        SelectorCiudadDialog(
            onSeleccionar = { ciudad = it; mostrandoSelectorCiudad = false },
            onDismiss = { mostrandoSelectorCiudad = false },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        Text("Un último paso", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Para personalizar tu experiencia en Bijao.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(top = 20.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Tu nombre") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.padding(top = 12.dp))

        OutlinedButton(onClick = { mostrandoSelectorCiudad = true }, modifier = Modifier.fillMaxWidth()) {
            Text(ciudad.ifBlank { "Tu ciudad" })
        }
        Spacer(Modifier.padding(top = 20.dp))

        Text("¿Qué te interesa?", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.padding(top = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            INTERESES.forEach { interes ->
                FilterChip(
                    selected = intereses.contains(interes),
                    onClick = {
                        intereses = if (intereses.contains(interes)) intereses - interes else intereses + interes
                    },
                    label = { Text(interes) },
                )
            }
        }
        Spacer(Modifier.padding(top = 20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = consiente, onCheckedChange = { consiente = it })
            Spacer(Modifier.padding(start = 8.dp))
            Text("Acepto el tratamiento de mis datos personales", style = MaterialTheme.typography.bodySmall)
        }

        error?.let {
            Spacer(Modifier.padding(top = 12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.padding(top = 20.dp))
        Button(
            onClick = {
                guardando = true
                error = null
                scope.launch {
                    try {
                        val respuesta = ApiClient.service.registrar(
                            RegistroInput(
                                nombre = nombre.trim().ifBlank { null },
                                celular = celularSesion.ifBlank { null },
                                intereses = intereses.toList(),
                                ciudad = ciudad.trim().ifBlank { null },
                                consentimientoDatos = consiente,
                            )
                        ).datoOLanzar()
                        onRegistrado(respuesta.data, respuesta.negociosVinculados ?: 0)
                    } catch (e: ApiException) {
                        error = e.message
                    } catch (e: Exception) {
                        error = "No pudimos completar el registro."
                    } finally {
                        guardando = false
                    }
                }
            },
            enabled = consiente && !guardando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (guardando) CircularProgressIndicator(modifier = Modifier.padding(2.dp)) else Text("Entrar a Bijao")
        }
    }
}
