package app.bijao.clientes.ui.perfil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.auth.AuthManager
import app.bijao.clientes.core.models.Perfil
import app.bijao.clientes.core.models.PerfilUpdateInput
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.ui.onboarding.SelectorCiudadDialog
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

private const val PREFS = "bijao_clientes"
private const val CLAVE_NOTIFICACIONES = "notificaciones_activadas"

/** Al estilo "Cuenta de Apple": avatar+nombre llevan el protagonismo arriba,
 * sin verse como una fila más. Equivalente de `PerfilView.swift`. */
@Composable
fun PerfilScreen(perfilInicial: Perfil, onPerfilActualizado: (Perfil) -> Unit, onVerHistorial: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    var perfil by remember { mutableStateOf(perfilInicial) }
    var nombre by remember { mutableStateOf(perfilInicial.nombre ?: "") }
    var ciudad by remember { mutableStateOf(perfilInicial.ciudad ?: "") }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mostrandoSelectorCiudad by remember { mutableStateOf(false) }
    var subiendoAvatar by remember { mutableStateOf(false) }
    var errorAvatar by remember { mutableStateOf<String?>(null) }
    var notificacionesActivadas by remember { mutableStateOf(prefs.getBoolean(CLAVE_NOTIFICACIONES, true)) }

    suspend fun guardar() {
        guardando = true
        error = null
        try {
            val respuesta = ApiClient.service.actualizarPerfil(
                PerfilUpdateInput(nombre = nombre.trim(), ciudad = ciudad.trim()),
            ).datoOLanzar()
            perfil = respuesta.data
            onPerfilActualizado(respuesta.data)
        } catch (e: Exception) {
            error = e.message ?: "No pudimos guardar los cambios."
        } finally {
            guardando = false
        }
    }

    suspend fun subirAvatar(bitmapOriginal: Bitmap) {
        subiendoAvatar = true
        errorAvatar = null
        try {
            val jpeg = withContext(Dispatchers.Default) { recortarYComprimir(bitmapOriginal) }
            val cuerpo = jpeg.toRequestBody("image/jpeg".toMediaType())
            val parte = MultipartBody.Part.createFormData("file", "avatar.jpg", cuerpo)
            val respuesta = ApiClient.service.subirAvatar(parte).datoOLanzar()
            perfil = perfil.copy(avatarUrl = respuesta.avatarUrl)
            onPerfilActualizado(perfil)
        } catch (e: Exception) {
            errorAvatar = e.message ?: "No pudimos subir la foto."
        } finally {
            subiendoAvatar = false
        }
    }

    val selectorFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
            bitmap?.let { subirAvatar(it) }
        }
    }

    if (mostrandoSelectorCiudad) {
        SelectorCiudadDialog(
            onSeleccionar = {
                ciudad = it
                mostrandoSelectorCiudad = false
                scope.launch { guardar() }
            },
            onDismiss = { mostrandoSelectorCiudad = false },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (subiendoAvatar) {
                            CircularProgressIndicator()
                        } else if (perfil.avatarUrl != null) {
                            AsyncImage(
                                model = perfil.avatarUrl,
                                contentDescription = "Foto de perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(6.dp),
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Cambiar foto de perfil", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Row {
                    TextButton(onClick = {
                        selectorFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Cambiar foto") }
                    if (perfil.avatarUrl != null) {
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    ApiClient.service.quitarAvatar().datoOLanzar()
                                    perfil = perfil.copy(avatarUrl = null)
                                    onPerfilActualizado(perfil)
                                } catch (e: Exception) {
                                    errorAvatar = e.message ?: "No pudimos quitar la foto."
                                }
                            }
                        }) { Text("Quitar foto", color = MaterialTheme.colorScheme.error) }
                    }
                }
                Text(
                    perfil.nombre?.takeIf { it.isNotBlank() } ?: "Sin nombre",
                    style = MaterialTheme.typography.titleLarge,
                )
                perfil.celular?.let {
                    Text("+57 $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                perfil.email?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                errorAvatar?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Editar perfil", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(onClick = { mostrandoSelectorCiudad = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(ciudad.ifBlank { "Escoger ciudad" })
                    }
                    error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = { scope.launch { guardar() } }, enabled = !guardando, modifier = Modifier.fillMaxWidth()) {
                        if (guardando) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Text("Guardar cambios")
                    }
                }
            }
        }

        item {
            Card(onClick = onVerHistorial, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Historial de compras", modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Avisarme de promociones y novedades",
                            modifier = Modifier.weight(1f).padding(end = 16.dp),
                        )
                        Switch(
                            checked = notificacionesActivadas,
                            onCheckedChange = {
                                notificacionesActivadas = it
                                prefs.edit().putBoolean(CLAVE_NOTIFICACIONES, it).apply()
                            },
                        )
                    }
                    Text(
                        // El envío real todavía depende de la Fase A9 (FCM) -- esto
                        // solo guarda la preferencia local por ahora.
                        "El envío real de notificaciones llega con una fase siguiente.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { scope.launch { AuthManager.salir() } },
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cerrar sesión")
            }
        }
    }
}

private fun recortarYComprimir(bitmap: Bitmap, lado: Int = 512): ByteArray {
    val tam = minOf(bitmap.width, bitmap.height)
    val x = (bitmap.width - tam) / 2
    val y = (bitmap.height - tam) / 2
    val cuadrado = Bitmap.createBitmap(bitmap, x, y, tam, tam)
    val escalado = Bitmap.createScaledBitmap(cuadrado, lado, lado, true)
    return ByteArrayOutputStream().apply { escalado.compress(Bitmap.CompressFormat.JPEG, 85, this) }.toByteArray()
}
