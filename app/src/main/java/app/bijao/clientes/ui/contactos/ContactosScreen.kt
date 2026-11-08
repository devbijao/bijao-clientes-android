package app.bijao.clientes.ui.contactos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.ContactoItem
import app.bijao.clientes.core.models.PersonaBusqueda
import app.bijao.clientes.core.models.RestriccionInput
import app.bijao.clientes.core.models.SolicitudInput
import app.bijao.clientes.core.models.SolicitudItem
import app.bijao.clientes.core.models.UsuarioInput
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.ui.ColorExito
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Pestana { CONTACTOS, SOLICITUDES, BLOQUEADOS }

/**
 * Contactos (Fase S1) -- tu @, buscar por @ o nombre, solicitudes con
 * aceptación y bloqueados. Espejo de `frontend/app/(app-clientes)/contactos/page.tsx`
 * y de `ContactosView.swift`. Solo se puede encontrar a quien eligió un @; de
 * los demás nunca se ve más que nombre, @ y foto -- nunca celular ni correo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactosScreen(onAtras: () -> Unit) {
    val scope = rememberCoroutineScope()

    var miUsuario by remember { mutableStateOf<String?>(null) }
    var editandoUsuario by remember { mutableStateOf(false) }
    var borrador by remember { mutableStateOf("") }
    var guardandoUsuario by remember { mutableStateOf(false) }
    var errorUsuario by remember { mutableStateOf<String?>(null) }

    var restringido by remember { mutableStateOf(false) }
    var cambiandoRestriccion by remember { mutableStateOf(false) }

    var contactos by remember { mutableStateOf(listOf<ContactoItem>()) }
    var recibidas by remember { mutableStateOf(listOf<SolicitudItem>()) }
    var enviadas by remember { mutableStateOf(listOf<SolicitudItem>()) }
    var bloqueados by remember { mutableStateOf(listOf<ContactoItem>()) }
    var cargando by remember { mutableStateOf(true) }
    var pestana by remember { mutableStateOf(Pestana.CONTACTOS) }
    var yaElegioPestanaInicial by remember { mutableStateOf(false) }

    var q by remember { mutableStateOf("") }
    var resultados by remember { mutableStateOf<List<PersonaBusqueda>?>(null) }
    var buscando by remember { mutableStateOf(false) }
    var accion by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    var personaABloquear by remember { mutableStateOf<Pair<String, String?>?>(null) }
    var contactoAEliminar by remember { mutableStateOf<ContactoItem?>(null) }

    suspend fun cargar() {
        cargando = true
        error = null
        try {
            val u = ApiClient.service.miUsuario().datoOLanzar()
            val c = ApiClient.service.misContactos().datoOLanzar()
            val s = ApiClient.service.misSolicitudes().datoOLanzar()
            val b = ApiClient.service.misBloqueos().datoOLanzar()
            miUsuario = u.usuario
            restringido = u.restringido
            contactos = c.data
            recibidas = s.recibidas
            enviadas = s.enviadas
            bloqueados = b.data
            if (!yaElegioPestanaInicial) {
                yaElegioPestanaInicial = true
                if (recibidas.isNotEmpty()) pestana = Pestana.SOLICITUDES
            }
        } catch (e: Exception) {
            error = "No pudimos cargar tus contactos."
        }
        cargando = false
    }

    LaunchedEffect(Unit) { cargar() }

    LaunchedEffect(q) {
        val termino = q.trim().removePrefix("@")
        if (termino.length < 3) {
            resultados = null
            buscando = false
            return@LaunchedEffect
        }
        buscando = true
        delay(350)
        try {
            val r = ApiClient.service.buscarPersonas(termino).datoOLanzar()
            resultados = r.data
        } catch (e: Exception) {
            error = "No pudimos buscar."
        }
        buscando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contactos") },
                navigationIcon = { IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Tu @", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.padding(top = 4.dp))
                if (!editandoUsuario) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(miUsuario?.let { "@$it" } ?: "No pudimos crearlo; elige uno", modifier = Modifier.weight(1f))
                        TextButton(onClick = { borrador = miUsuario ?: ""; editandoUsuario = true }) {
                            Text(if (miUsuario != null) "Cambiar" else "Elegir")
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = borrador,
                            onValueChange = { borrador = it },
                            placeholder = { Text("ana.perez") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = KeyboardCapitalization.None),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.padding(start = 8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    guardandoUsuario = true
                                    errorUsuario = null
                                    try {
                                        val r = ApiClient.service.elegirUsuario(UsuarioInput(borrador)).datoOLanzar()
                                        miUsuario = r.usuario
                                        editandoUsuario = false
                                    } catch (e: Exception) {
                                        errorUsuario = "No pudimos guardar tu @. Puede que ya esté en uso."
                                    }
                                    guardandoUsuario = false
                                }
                            },
                            enabled = !guardandoUsuario && borrador.trim().length >= 3,
                        ) {
                            if (guardandoUsuario) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Text("Guardar")
                        }
                    }
                    errorUsuario?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                }
                Text(
                    "Lo creamos con tu nombre; puedes cambiarlo cuando quieras. Nunca verán tu celular ni tu correo.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (miUsuario != null) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Restringir perfil", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (restringido) "Nadie te encuentra en las búsquedas; solo conectas si tú mandas la solicitud."
                                else "Nadie podrá enviarte solicitudes; tú sí podrás enviar.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = restringido,
                            onCheckedChange = { nuevo ->
                                scope.launch {
                                    if (cambiandoRestriccion) return@launch
                                    cambiandoRestriccion = true
                                    try {
                                        ApiClient.service.restringirPerfil(RestriccionInput(nuevo)).datoOLanzar()
                                        restringido = nuevo
                                    } catch (e: Exception) {
                                        error = "No pudimos cambiar la privacidad."
                                    }
                                    cambiandoRestriccion = false
                                }
                            },
                            enabled = !cambiandoRestriccion,
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = q,
                        onValueChange = { q = it },
                        placeholder = { Text("Buscar por @ o nombre") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = { if (buscando) CircularProgressIndicator(modifier = Modifier.size(18.dp)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = KeyboardCapitalization.None),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                resultados?.let { lista ->
                    if (lista.isEmpty() && !buscando) {
                        item { Text("No encontramos a nadie con ese nombre o @.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(lista, key = { it.usuario ?: it.nombre.orEmpty() }) { p ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            FilaPersona(nombre = p.nombre, usuario = p.usuario, modifier = Modifier.weight(1f))
                            when (p.relacion) {
                                "CONTACTO" -> Text("Contacto", color = ColorExito, style = MaterialTheme.typography.labelMedium)
                                "ENVIADA" -> Text("Enviada", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                                "NINGUNA", "RECIBIDA" -> OutlinedButton(
                                    onClick = {
                                        val usuario = p.usuario ?: return@OutlinedButton
                                        scope.launch {
                                            accion = "pedir:$usuario"
                                            try {
                                                val r = ApiClient.service.pedirConexion(SolicitudInput(usuario)).datoOLanzar()
                                                resultados = resultados?.map { x ->
                                                    if (x.usuario == usuario) x.copy(relacion = if (r.estado == "ACEPTADA") "CONTACTO" else "ENVIADA") else x
                                                }
                                                cargar()
                                            } catch (e: Exception) {
                                                error = "No pudimos completar la acción."
                                            }
                                            accion = null
                                        }
                                    },
                                    enabled = accion != "pedir:${p.usuario}",
                                ) { Icon(Icons.Filled.PersonAdd, contentDescription = null); Text(if (p.relacion == "RECIBIDA") " Aceptar" else " Conectar") }
                            }
                            p.usuario?.let { usuario ->
                                IconButton(onClick = { personaABloquear = usuario to p.nombre }) {
                                    Icon(Icons.Filled.Block, contentDescription = "Bloquear", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            } else {
                item { Text("Elige tu @ para poder buscar y conectar con otras personas.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = pestana == Pestana.CONTACTOS,
                        onClick = { pestana = Pestana.CONTACTOS },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    ) { Text(if (contactos.isEmpty()) "Contactos" else "Contactos (${contactos.size})") }
                    SegmentedButton(
                        selected = pestana == Pestana.SOLICITUDES,
                        onClick = { pestana = Pestana.SOLICITUDES },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    ) { Text(if (recibidas.isEmpty()) "Solicitudes" else "Solicitudes (${recibidas.size})") }
                    SegmentedButton(
                        selected = pestana == Pestana.BLOQUEADOS,
                        onClick = { pestana = Pestana.BLOQUEADOS },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    ) { Text("Bloqueados") }
                }
            }

            if (cargando) {
                item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else when (pestana) {
                Pestana.CONTACTOS -> if (contactos.isEmpty()) {
                    item { Text("Aún no tienes contactos. Búscalos por su @.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(contactos, key = { it.id }) { c ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            FilaPersona(nombre = c.nombre, usuario = c.usuario, modifier = Modifier.weight(1f))
                            IconButton(onClick = { contactoAEliminar = c }) {
                                Icon(Icons.Filled.PersonRemove, contentDescription = "Eliminar contacto")
                            }
                            c.usuario?.let { usuario ->
                                IconButton(onClick = { personaABloquear = usuario to c.nombre }) {
                                    Icon(Icons.Filled.Block, contentDescription = "Bloquear", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                Pestana.SOLICITUDES -> {
                    if (recibidas.isEmpty() && enviadas.isEmpty()) {
                        item { Text("No tienes solicitudes pendientes.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    if (recibidas.isNotEmpty()) {
                        item { Text("Recibidas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        items(recibidas, key = { it.id }) { s ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                FilaPersona(nombre = s.nombre, usuario = s.usuario, modifier = Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            accion = "ok:${s.id}"
                                            try {
                                                ApiClient.service.aceptarSolicitud(s.id).datoOLanzar()
                                                cargar()
                                            } catch (e: Exception) {
                                                error = "No pudimos aceptar la solicitud."
                                            }
                                            accion = null
                                        }
                                    },
                                    enabled = accion != "ok:${s.id}",
                                ) { Text("Aceptar") }
                                Spacer(Modifier.padding(start = 6.dp))
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            accion = "no:${s.id}"
                                            try {
                                                ApiClient.service.quitarSolicitud(s.id).datoOLanzar()
                                                cargar()
                                            } catch (e: Exception) {
                                                error = "No pudimos completar la acción."
                                            }
                                            accion = null
                                        }
                                    },
                                    enabled = accion != "no:${s.id}",
                                ) { Text("Ignorar") }
                            }
                        }
                    }
                    if (enviadas.isNotEmpty()) {
                        item { Text("Enviadas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        items(enviadas, key = { it.id }) { s ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                FilaPersona(nombre = s.nombre, usuario = s.usuario, modifier = Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            accion = "no:${s.id}"
                                            try {
                                                ApiClient.service.quitarSolicitud(s.id).datoOLanzar()
                                                cargar()
                                            } catch (e: Exception) {
                                                error = "No pudimos completar la acción."
                                            }
                                            accion = null
                                        }
                                    },
                                    enabled = accion != "no:${s.id}",
                                ) { Text("Cancelar") }
                            }
                        }
                    }
                }

                Pestana.BLOQUEADOS -> if (bloqueados.isEmpty()) {
                    item { Text("No has bloqueado a nadie.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(bloqueados, key = { it.id }) { b ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            FilaPersona(nombre = b.nombre, usuario = b.usuario, modifier = Modifier.weight(1f))
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        accion = "desb:${b.id}"
                                        try {
                                            ApiClient.service.desbloquearPersona(b.id).datoOLanzar()
                                            cargar()
                                        } catch (e: Exception) {
                                            error = "No pudimos desbloquear a esa persona."
                                        }
                                        accion = null
                                    }
                                },
                                enabled = accion != "desb:${b.id}",
                            ) { Text("Desbloquear") }
                        }
                    }
                }
            }
        }
    }

    personaABloquear?.let { (usuario, nombre) ->
        AlertDialog(
            onDismissRequest = { personaABloquear = null },
            title = { Text("¿Bloquear a ${nombre ?: "@$usuario"}?") },
            text = { Text("Dejarán de verse y se cancelará la conexión.") },
            confirmButton = {
                TextButton(onClick = {
                    personaABloquear = null
                    scope.launch {
                        accion = "bloq:$usuario"
                        try {
                            ApiClient.service.bloquearPersona(SolicitudInput(usuario)).datoOLanzar()
                            resultados = resultados?.filterNot { it.usuario == usuario }
                            cargar()
                        } catch (e: Exception) {
                            error = "No pudimos bloquear a esa persona."
                        }
                        accion = null
                    }
                }) { Text("Bloquear") }
            },
            dismissButton = { TextButton(onClick = { personaABloquear = null }) { Text("Cancelar") } },
        )
    }

    contactoAEliminar?.let { c ->
        AlertDialog(
            onDismissRequest = { contactoAEliminar = null },
            title = { Text("¿Eliminar a ${c.nombre ?: "@${c.usuario}"} de tus contactos?") },
            confirmButton = {
                TextButton(onClick = {
                    contactoAEliminar = null
                    scope.launch {
                        accion = "del:${c.id}"
                        try {
                            ApiClient.service.eliminarContacto(c.id).datoOLanzar()
                            cargar()
                        } catch (e: Exception) {
                            error = "No pudimos eliminar el contacto."
                        }
                        accion = null
                    }
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { contactoAEliminar = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun FilaPersona(nombre: String?, usuario: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(nombre ?: usuario?.let { "@$it" } ?: "", style = MaterialTheme.typography.bodyMedium)
        usuario?.let { Text("@$it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
