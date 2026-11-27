package app.bijao.clientes.ui.contactos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.ContactoItem
import app.bijao.clientes.core.models.RestriccionInput
import app.bijao.clientes.core.models.SolicitudItem
import app.bijao.clientes.core.models.UsuarioInput
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Tu @, el perfil restringido y los bloqueados (antes estaban arriba de la
 * lista de Contactos). Equivalente de `AjustesContactosView` en iOS. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesContactosScreen(onAtras: () -> Unit) {
    val scope = rememberCoroutineScope()
    var miUsuario by remember { mutableStateOf<String?>(null) }
    var borrador by remember { mutableStateOf("") }
    var editando by remember { mutableStateOf(false) }
    var restringido by remember { mutableStateOf(false) }
    var bloqueados by remember { mutableStateOf<List<ContactoItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val u = ApiClient.service.miUsuario().datoOLanzar()
            miUsuario = u.usuario
            restringido = u.restringido
            bloqueados = ApiClient.service.misBloqueos().datoOLanzar().data
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            error = "No pudimos cargar tus ajustes."
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Tu @ y privacidad") },
            navigationIcon = { IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Tu @", style = MaterialTheme.typography.titleSmall)
                if (editando) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(borrador, { borrador = it.trim() }, singleLine = true, placeholder = { Text("ana.perez") },
                            modifier = Modifier.weight(1f))
                        TextButton(enabled = borrador.length >= 3, onClick = {
                            scope.launch {
                                try {
                                    miUsuario = ApiClient.service.elegirUsuario(UsuarioInput(borrador)).datoOLanzar().usuario
                                    editando = false
                                    error = null
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                    error = e.message ?: "No pudimos guardar tu @."
                                }
                            }
                        }) { Text("Guardar") }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(miUsuario?.let { "@$it" } ?: "Aún no tienes @", modifier = Modifier.weight(1f))
                        TextButton(onClick = { borrador = miUsuario.orEmpty(); editando = true }) { Text(if (miUsuario != null) "Cambiar" else "Elegir") }
                    }
                }
                Text("Otras personas pueden encontrarte por tu @ o tu nombre y pedirte conectar. Nunca verán tu celular ni tu correo.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (miUsuario != null) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Restringir perfil", modifier = Modifier.weight(1f))
                        Switch(checked = restringido, onCheckedChange = { nuevo ->
                            scope.launch {
                                try {
                                    ApiClient.service.restringirPerfil(RestriccionInput(nuevo)).datoOLanzar()
                                    restringido = nuevo
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                    error = "No pudimos cambiar la privacidad."
                                }
                            }
                        })
                    }
                    Text(
                        if (restringido) "Nadie te encuentra en las búsquedas. Solo conectas con quien tú le mandes la solicitud."
                        else "Activa esto para no aparecer en las búsquedas. Tú podrás enviar solicitudes, pero nadie podrá enviártelas.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { Text("Bloqueados", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
            if (bloqueados.isEmpty()) {
                item { Text("No has bloqueado a nadie.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(bloqueados, key = { it.id }) { b ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { FilaPersona(b.nombre, b.usuario, b.avatarUrl) }
                    OutlinedButton(onClick = {
                        scope.launch {
                            try {
                                ApiClient.service.desbloquearPersona(b.id).datoOLanzar()
                                bloqueados = bloqueados.filterNot { it.id == b.id }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                error = "No pudimos desbloquear a esa persona."
                            }
                        }
                    }) { Text("Desbloquear") }
                }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
    }
}

/** Solicitudes recibidas (aceptar o ignorar) y enviadas (cancelar). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudesScreen(onAtras: () -> Unit, onPersona: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var recibidas by remember { mutableStateOf<List<SolicitudItem>>(emptyList()) }
    var enviadas by remember { mutableStateOf<List<SolicitudItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var trabajando by remember { mutableStateOf(false) }

    suspend fun cargar() {
        try {
            val s = ApiClient.service.misSolicitudes().datoOLanzar()
            recibidas = s.recibidas
            enviadas = s.enviadas
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            error = "No pudimos cargar tus solicitudes."
        }
    }
    LaunchedEffect(Unit) { cargar() }

    fun responder(s: SolicitudItem, aceptar: Boolean) {
        if (trabajando) return
        scope.launch {
            trabajando = true
            try {
                if (aceptar) ApiClient.service.aceptarSolicitud(s.id).datoOLanzar() else ApiClient.service.quitarSolicitud(s.id).datoOLanzar()
                cargar()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                error = "No pudimos completar la acción."
            } finally {
                trabajando = false
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Solicitudes") },
            navigationIcon = { IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            if (recibidas.isNotEmpty()) {
                item { Text("Recibidas", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 20.dp, top = 12.dp)) }
                items(recibidas, key = { it.id }) { s ->
                    Column {
                        FilaPersona(s.nombre, s.usuario, s.avatarUrl, s.bio) { s.usuario?.let(onPersona) }
                        Row(Modifier.fillMaxWidth().padding(start = 74.dp, end = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { responder(s, true) }, enabled = !trabajando) { Text("Aceptar") }
                            OutlinedButton(onClick = { responder(s, false) }, enabled = !trabajando) { Text("Ignorar") }
                        }
                    }
                }
            }
            if (enviadas.isNotEmpty()) {
                item { Text("Enviadas", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 20.dp, top = 16.dp)) }
                items(enviadas, key = { it.id }) { s ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                        Column(Modifier.weight(1f)) { FilaPersona(s.nombre, s.usuario, s.avatarUrl, s.bio) }
                        OutlinedButton(onClick = { responder(s, false) }, enabled = !trabajando) { Text("Cancelar") }
                    }
                }
            }
            if (recibidas.isEmpty() && enviadas.isEmpty()) {
                item { Text("No tienes solicitudes pendientes.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp)) }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium, modifier = Modifier.padding(20.dp)) } }
        }
    }
}
