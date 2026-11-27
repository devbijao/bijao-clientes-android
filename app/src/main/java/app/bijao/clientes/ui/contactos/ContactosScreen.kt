package app.bijao.clientes.ui.contactos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.bijao.clientes.core.models.ContactoItem
import app.bijao.clientes.core.models.Perfil
import app.bijao.clientes.core.models.PersonaBusqueda
import app.bijao.clientes.core.models.SolicitudItem
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.ui.FotoPerfil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.text.Normalizer

internal fun normalizar(s: String) =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "").lowercase()

/**
 * Contactos al estilo «Nuevo chat» de WhatsApp (F2 de
 * `MDs/plan_apps_nativas_previo_revision.md`): arriba tú, luego las
 * solicitudes, un buscador por nombre o @ y la lista con foto, nombre e info,
 * agrupada por letra. Tocar a alguien abre su perfil grande, con «Seguir» (= la
 * solicitud de conexión de siempre) si aún no es contacto. Tu @, el perfil
 * restringido y los bloqueados viven en el engranaje. Equivalente de
 * `ContactosView.swift`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactosScreen(
    miPerfil: Perfil?,
    onAtras: () -> Unit,
    onPersona: (String) -> Unit,
    onSolicitudes: () -> Unit,
    onAjustes: () -> Unit,
) {
    val ciclo = LocalLifecycleOwner.current.lifecycle
    var miUsuario by remember { mutableStateOf<String?>(null) }
    var contactos by remember { mutableStateOf<List<ContactoItem>>(emptyList()) }
    var recibidas by remember { mutableStateOf<List<SolicitudItem>>(emptyList()) }
    var enviadas by remember { mutableStateOf<List<SolicitudItem>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var q by remember { mutableStateOf("") }
    var resultados by remember { mutableStateOf<List<PersonaBusqueda>?>(null) }

    // Se recarga cada vez que la pantalla vuelve a primer plano (al volver del
    // perfil de alguien después de seguir, aceptar o bloquear).
    LaunchedEffect(Unit) {
        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                miUsuario = ApiClient.service.miUsuario().datoOLanzar().usuario
                contactos = ApiClient.service.misContactos().datoOLanzar().data
                val s = ApiClient.service.misSolicitudes().datoOLanzar()
                recibidas = s.recibidas
                enviadas = s.enviadas
                error = null
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                error = "No pudimos cargar tus contactos."
            }
            cargando = false
        }
    }

    LaunchedEffect(q, miUsuario) {
        val termino = q.trim().trimStart('@')
        if (termino.length < 3 || miUsuario == null) {
            resultados = null
            return@LaunchedEffect
        }
        delay(350)
        resultados = try {
            ApiClient.service.buscarPersonas(termino).datoOLanzar().data
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    val termino = normalizar(q.trim().trimStart('@'))
    val filtrados = if (termino.isEmpty()) contactos else contactos.filter { c ->
        listOfNotNull(c.nombre, c.usuario).any { normalizar(it).contains(termino) }
    }
    val porLetra = filtrados
        .groupBy { c -> normalizar(c.nombre ?: c.usuario ?: "").take(1).uppercase().takeIf { it.firstOrNull()?.isLetter() == true } ?: "#" }
        .toSortedMap(compareBy<String> { it == "#" }.thenBy { it })
    val nuevas = resultados.orEmpty().filter { it.relacion != "CONTACTO" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contactos") },
                navigationIcon = { IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = { IconButton(onClick = onAjustes) { Icon(Icons.Filled.Settings, contentDescription = "Tu @ y privacidad") } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                OutlinedTextField(
                    value = q, onValueChange = { q = it },
                    placeholder = { Text("Nombre o @usuario") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true, shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FotoPerfil(miPerfil?.avatarUrl, miPerfil?.nombre ?: miUsuario, tamano = 52.dp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("${miUsuario?.let { "@$it" } ?: miPerfil?.nombre ?: "Tú"} (Tú)", fontWeight = FontWeight.SemiBold)
                        Text(miPerfil?.bio?.takeIf { it.isNotBlank() } ?: "Así te ven tus contactos.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (recibidas.isNotEmpty() || enviadas.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().clickable(onClick = onSolicitudes).padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.PersonAddAlt1, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Solicitudes", modifier = Modifier.weight(1f).padding(start = 16.dp))
                        if (recibidas.isNotEmpty()) {
                            Text("${recibidas.size}", color = Color.White, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary).padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp).padding(2.dp))
                    }
                }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) } }
            when {
                cargando && contactos.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                contactos.isEmpty() && q.isBlank() -> item {
                    Text(
                        if (miUsuario == null) "Elige tu @ en el engranaje de arriba para buscar y conectar con otras personas."
                        else "Aún no tienes contactos. Búscalos por su nombre o @.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp),
                    )
                }
                else -> porLetra.forEach { (letra, lista) ->
                    item(key = "letra-$letra") {
                        Text(letra, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp))
                    }
                    items(lista.sortedBy { normalizar(it.nombre ?: it.usuario ?: "") }, key = { it.id }) { c ->
                        FilaPersona(c.nombre, c.usuario, c.avatarUrl, c.bio) { c.usuario?.let(onPersona) }
                    }
                }
            }
            if (q.isNotBlank() && miUsuario != null) {
                item {
                    Text("Más personas en Bijao", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp))
                }
                if (nuevas.isEmpty()) {
                    item {
                        Text(
                            if (termino.length < 3) "Escribe al menos 3 letras para buscar en Bijao." else "No encontramos a nadie más con ese nombre o @.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                } else {
                    items(nuevas, key = { "n-${it.usuario}" }) { p ->
                        FilaPersona(p.nombre, p.usuario, p.avatarUrl, p.bio,
                            etiqueta = when (p.relacion) { "ENVIADA" -> "Solicitud enviada"; "RECIBIDA" -> "Te pidió conectar"; else -> null }) {
                            p.usuario?.let(onPersona)
                        }
                    }
                }
            }
        }
    }
}

/** Fila de una persona: foto, nombre y debajo su info (o su @). */
@Composable
internal fun FilaPersona(
    nombre: String?,
    usuario: String?,
    avatarUrl: String?,
    bio: String? = null,
    etiqueta: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FotoPerfil(avatarUrl, nombre ?: usuario, tamano = 46.dp)
        Column(Modifier.weight(1f)) {
            Text(nombre ?: usuario?.let { "@$it" }.orEmpty(), fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(bio?.takeIf { it.isNotBlank() } ?: usuario?.let { "@$it" }.orEmpty(),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        etiqueta?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
