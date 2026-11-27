package app.bijao.clientes.ui.perfil

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.Perfil
import app.bijao.clientes.core.models.PerfilUpdateInput
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.ui.FotoPerfil
import app.bijao.clientes.core.ui.PortadaPerfil
import app.bijao.clientes.core.ui.decodificarImagen
import app.bijao.clientes.core.ui.jpegCuadrado
import app.bijao.clientes.core.ui.jpegPortada
import app.bijao.clientes.ui.onboarding.SelectorCiudadDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

const val MAX_BIO = 140

private enum class Imagen { FOTO, PORTADA }

/**
 * «Editar» del perfil (botón de arriba a la derecha, como en WhatsApp): portada,
 * foto, nombre, info y ciudad. Las fotos se suben en cuanto se eligen; el texto
 * se guarda con «Guardar». Equivalente de `EditarPerfilView.swift`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPerfilScreen(perfil: Perfil, onPerfilActualizado: (Perfil) -> Unit, onCerrar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var actual by remember { mutableStateOf(perfil) }
    var nombre by remember { mutableStateOf(perfil.nombre.orEmpty()) }
    var bio by remember { mutableStateOf(perfil.bio.orEmpty()) }
    var ciudad by remember { mutableStateOf(perfil.ciudad.orEmpty()) }
    var guardando by remember { mutableStateOf(false) }
    var subiendo by remember { mutableStateOf<Imagen?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var eligiendoCiudad by remember { mutableStateOf(false) }
    var menuAbierto by remember { mutableStateOf<Imagen?>(null) }
    var destino by remember { mutableStateOf(Imagen.FOTO) }

    fun publicar(p: Perfil) {
        actual = p
        onPerfilActualizado(p)
    }

    suspend fun subir(uri: android.net.Uri, tipo: Imagen) {
        subiendo = tipo
        error = null
        try {
            val jpeg = withContext(Dispatchers.Default) {
                val bitmap = decodificarImagen(context, uri) ?: error("No se pudo leer la imagen")
                if (tipo == Imagen.FOTO) jpegCuadrado(bitmap) else jpegPortada(bitmap)
            }
            val nombreArchivo = if (tipo == Imagen.FOTO) "avatar.jpg" else "portada.jpg"
            val parte = MultipartBody.Part.createFormData("file", nombreArchivo, jpeg.toRequestBody("image/jpeg".toMediaType()))
            publicar(
                if (tipo == Imagen.FOTO) actual.copy(avatarUrl = ApiClient.service.subirAvatar(parte).datoOLanzar().avatarUrl)
                else actual.copy(portadaUrl = ApiClient.service.subirPortada(parte).datoOLanzar().portadaUrl),
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            error = "No pudimos subir la imagen. Inténtalo de nuevo en un momento."
        } finally {
            subiendo = null
        }
    }

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { subir(uri, destino) }
    }

    fun quitar(tipo: Imagen) {
        scope.launch {
            subiendo = tipo
            try {
                publicar(
                    if (tipo == Imagen.FOTO) { ApiClient.service.quitarAvatar().datoOLanzar(); actual.copy(avatarUrl = null) }
                    else { ApiClient.service.quitarPortada().datoOLanzar(); actual.copy(portadaUrl = null) },
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                error = "No pudimos quitar la imagen."
            } finally {
                subiendo = null
            }
        }
    }

    if (eligiendoCiudad) {
        SelectorCiudadDialog(onSeleccionar = { ciudad = it; eligiendoCiudad = false }, onDismiss = { eligiendoCiudad = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar perfil") },
                navigationIcon = {
                    IconButton(onClick = onCerrar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar") }
                },
                actions = {
                    TextButton(
                        enabled = !guardando && subiendo == null,
                        onClick = {
                            scope.launch {
                                guardando = true
                                error = null
                                try {
                                    val r = ApiClient.service.actualizarPerfil(
                                        PerfilUpdateInput(nombre = nombre.trim(), ciudad = ciudad.trim(), bio = bio.trim()),
                                    ).datoOLanzar()
                                    publicar(r.data)
                                    onCerrar()
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                    error = "No pudimos guardar. Inténtalo de nuevo en un momento."
                                } finally {
                                    guardando = false
                                }
                            }
                        },
                    ) {
                        if (guardando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Guardar", fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                Box(Modifier.padding(bottom = 48.dp)) {
                    PortadaPerfil(actual.portadaUrl, alto = 150.dp)
                    if (subiendo == Imagen.PORTADA) Cargando(Modifier.fillMaxWidth().padding(top = 60.dp))
                }
                Box(contentAlignment = Alignment.Center) {
                    FotoPerfil(actual.avatarUrl, actual.nombre, tamano = 96.dp, aro = true)
                    if (subiendo == Imagen.FOTO) {
                        Box(Modifier.size(96.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                for (tipo in Imagen.entries) {
                    val etiqueta = if (tipo == Imagen.FOTO) "Foto" else "Portada"
                    val hay = if (tipo == Imagen.FOTO) actual.avatarUrl != null else actual.portadaUrl != null
                    Box {
                        FilledTonalButton(onClick = { menuAbierto = tipo }, enabled = subiendo == null) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(etiqueta, modifier = Modifier.padding(start = 6.dp))
                        }
                        DropdownMenu(expanded = menuAbierto == tipo, onDismissRequest = { menuAbierto = null }) {
                            DropdownMenuItem(
                                text = { Text(if (hay) "Cambiar ${etiqueta.lowercase()}" else "Elegir ${etiqueta.lowercase()}") },
                                onClick = {
                                    menuAbierto = null
                                    destino = tipo
                                    selector.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            )
                            if (hay) {
                                DropdownMenuItem(
                                    text = { Text("Quitar ${etiqueta.lowercase()}", color = MaterialTheme.colorScheme.error) },
                                    onClick = { menuAbierto = null; quitar(tipo) },
                                )
                            }
                        }
                    }
                }
            }

            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it.take(120) }, label = { Text("Nombre") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = bio, onValueChange = { bio = it.take(MAX_BIO) },
                    label = { Text("Info") }, placeholder = { Text("Ej. Amante del café y las buenas ofertas") },
                    supportingText = {
                        Row(Modifier.fillMaxWidth()) {
                            Text("La ven también quienes aún no son tus contactos.", modifier = Modifier.weight(1f))
                            Text("${bio.length}/$MAX_BIO")
                        }
                    },
                    minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { eligiendoCiudad = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(ciudad.ifBlank { "Escoger ciudad" })
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun Cargando(modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
}
