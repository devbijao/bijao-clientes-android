package app.bijao.clientes.ui.contactos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.PerfilPersona
import app.bijao.clientes.core.models.SolicitudInput
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.ui.ColorExito
import app.bijao.clientes.core.ui.FotoPerfil
import app.bijao.clientes.core.ui.PortadaPerfil
import app.bijao.clientes.ui.reportar.ObjetoReporte
import app.bijao.clientes.ui.reportar.ReportarSheet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * El perfil grande de otra persona (`GET /contactos/perfil/{@}`): portada, foto,
 * nombre, @ e info. La acción depende de la relación: «Seguir» (la solicitud de
 * siempre), «Solicitud enviada» (cancelable), «Aceptar / Ignorar» o «Es tu
 * contacto». Eliminar y bloquear van en el menú. Equivalente de
 * `PerfilPersonaView.swift`.
 */
@Composable
fun PerfilPersonaScreen(usuario: String, onAtras: () -> Unit) {
    val scope = rememberCoroutineScope()
    var persona by remember { mutableStateOf<PerfilPersona?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var trabajando by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf<String?>(null) }   // "bloquear" | "eliminar" | "cancelar"
    var reportando by remember { mutableStateOf(false) }

    suspend fun cargar() {
        try {
            persona = ApiClient.service.perfilDe(usuario).datoOLanzar().data
            error = null
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (persona == null) error = "Puede que haya cambiado su @ o que ya no esté disponible."
        }
    }

    fun ejecutar(fallo: String, accion: suspend () -> Unit) {
        if (trabajando) return
        scope.launch {
            trabajando = true
            try {
                accion()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                error = fallo
            } finally {
                trabajando = false
            }
        }
    }

    LaunchedEffect(usuario) { cargar() }

    val p = persona
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            p == null && error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(32.dp))
            p == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.BottomCenter) {
                    PortadaPerfil(p.portadaUrl, alto = 230.dp, modifier = Modifier.padding(bottom = 70.dp))
                    FotoPerfil(p.avatarUrl, p.nombre ?: p.usuario, tamano = 140.dp, aro = true)
                }
                Text(p.nombre ?: "@$usuario", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp, start = 24.dp, end = 24.dp))
                Text("@${p.usuario ?: usuario}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                p.bio?.takeIf { it.isNotBlank() }?.let {
                    Text(it, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp))
                }

                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    when (p.relacion) {
                        "CONTACTO" -> Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ColorExito)
                            Text("Es tu contacto", color = ColorExito, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
                        }
                        "ENVIADA" -> OutlinedButton(onClick = { confirmar = "cancelar" }, enabled = !trabajando, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Solicitud enviada", modifier = Modifier.padding(start = 8.dp))
                        }
                        "RECIBIDA" -> {
                            Text("Te pidió conectar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { ejecutar("No pudimos aceptar.") { seguir(usuario); cargar() } },
                                    enabled = !trabajando, modifier = Modifier.weight(1f)) { Text("Aceptar") }
                                OutlinedButton(onClick = {
                                    ejecutar("No pudimos completar la acción.") {
                                        p.conexionId?.let { ApiClient.service.quitarSolicitud(it).datoOLanzar() }
                                        cargar()
                                    }
                                }, enabled = !trabajando, modifier = Modifier.weight(1f)) { Text("Ignorar") }
                            }
                        }
                        else -> Button(onClick = { ejecutar("No pudimos enviar la solicitud.") { seguir(usuario); cargar() } },
                            enabled = !trabajando, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            if (trabajando) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            else {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Seguir", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
                }
            }
        }

        IconButton(onClick = onAtras, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            .clip(CircleShape).background(Color.Black.copy(alpha = 0.3f))) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
        }
        if (p != null) {
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                IconButton(onClick = { menu = true }, modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.3f))) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones", tint = Color.White)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (p.relacion == "CONTACTO") {
                        DropdownMenuItem(text = { Text("Eliminar de mis contactos") }, onClick = { menu = false; confirmar = "eliminar" })
                    }
                    DropdownMenuItem(text = { Text("Reportar") }, onClick = { menu = false; reportando = true })
                    DropdownMenuItem(text = { Text("Bloquear", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; confirmar = "bloquear" })
                }
            }
        }
    }

    if (reportando) {
        ReportarSheet(ObjetoReporte.Persona(usuario), "Reportar a @$usuario") { reportando = false }
    }

    confirmar?.let { accion ->
        val (titulo, texto, boton) = when (accion) {
            "bloquear" -> Triple("¿Bloquear a @$usuario?", "Dejarán de verse y se cancelará la conexión. No se le avisa.", "Bloquear")
            "eliminar" -> Triple("¿Eliminar de tus contactos?", "Podrán volver a conectar con una solicitud.", "Eliminar")
            else -> Triple("¿Cancelar la solicitud?", "Podrás volver a enviarla cuando quieras.", "Cancelar solicitud")
        }
        AlertDialog(
            onDismissRequest = { confirmar = null },
            title = { Text(titulo) },
            text = { Text(texto) },
            confirmButton = {
                TextButton(onClick = {
                    confirmar = null
                    when (accion) {
                        "bloquear" -> ejecutar("No pudimos bloquear a esta persona.") {
                            ApiClient.service.bloquearPersona(SolicitudInput(usuario)).datoOLanzar()
                            onAtras()
                        }
                        else -> ejecutar("No pudimos completar la acción.") {
                            p?.conexionId?.let { id ->
                                if (accion == "eliminar") ApiClient.service.eliminarContacto(id).datoOLanzar()
                                else ApiClient.service.quitarSolicitud(id).datoOLanzar()
                            }
                            cargar()
                        }
                    }
                }) { Text(boton, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmar = null }) { Text("Volver") } },
        )
    }
}

/** «Seguir» o «Aceptar»: es la misma solicitud; si la otra persona ya me había
 * pedido, el backend la acepta sola. */
private suspend fun seguir(usuario: String) {
    ApiClient.service.pedirConexion(SolicitudInput(usuario)).datoOLanzar()
}
