package app.bijao.clientes.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Equivalente de `SelectorCiudadView.swift`: ubicación actual (reversa con
 * `Geocoder`) o búsqueda manual por nombre -- a diferencia de iOS, acá no hay
 * un autocompletado en vivo tipo `MKLocalSearchCompleter` porque eso pide la
 * API key de Google Places (bloqueada, ver §1.1 del plan); `Geocoder` viene
 * con el SDK de Android y no necesita ninguna.
 */
@Composable
fun SelectorCiudadDialog(onSeleccionar: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var texto by remember { mutableStateOf("") }
    var resultados by remember { mutableStateOf<List<String>>(emptyList()) }
    var buscandoUbicacion by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun usarUbicacionActual() {
        buscandoUbicacion = true
        error = null
        scope.launch {
            try {
                val cliente = LocationServices.getFusedLocationProviderClient(context)
                val tokenCancelacion = CancellationTokenSource()
                val ubicacion = suspendCancellableCoroutine<android.location.Location?> { cont ->
                    cliente.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenCancelacion.token)
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                }
                val ciudad = ubicacion?.let {
                    withContext(Dispatchers.IO) {
                        @Suppress("DEPRECATION")
                        Geocoder(context, Locale.getDefault())
                            .getFromLocation(it.latitude, it.longitude, 1)
                            ?.firstOrNull()?.locality
                    }
                }
                if (ciudad != null) onSeleccionar(ciudad) else error = "No pudimos identificar tu ciudad."
            } catch (e: Exception) {
                error = "No pudimos obtener tu ubicación."
            } finally {
                buscandoUbicacion = false
            }
        }
    }

    val permisoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) usarUbicacionActual() else error = "Necesitamos el permiso de ubicación para esto."
    }

    fun pedirUbicacion() {
        val concedido = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (concedido) usarUbicacionActual() else permisoLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    fun buscar(query: String) {
        if (query.isBlank()) {
            resultados = emptyList()
            return
        }
        scope.launch {
            resultados = withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    Geocoder(context, Locale.getDefault())
                        .getFromLocationName(query, 5)
                        ?.mapNotNull { it.locality }
                        ?.distinct()
                        ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tu ciudad") },
        text = {
            Column {
                TextButton(onClick = { pedirUbicacion() }, enabled = !buscandoUbicacion) {
                    if (buscandoUbicacion) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    }
                    Text("Usar mi ubicación actual")
                }
                Spacer(Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it; buscar(it) },
                    label = { Text("Busca tu ciudad") },
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Spacer(Modifier.padding(top = 8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                resultados.forEach { resultado ->
                    TextButton(onClick = { onSeleccionar(resultado) }) { Text(resultado) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
