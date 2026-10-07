package app.bijao.clientes.core.location

import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Ubicación bajo demanda (nunca al abrir la app), compartida entre el
 * selector de ciudad del Onboarding y "cerca de mí" en Explorar -- equivalente
 * de `UbicacionManager.swift`. Asume que el permiso ya se pidió/concedió. */
suspend fun obtenerUbicacionActual(context: Context): Location? {
    val cliente = LocationServices.getFusedLocationProviderClient(context)
    val tokenCancelacion = CancellationTokenSource()
    return suspendCancellableCoroutine { cont ->
        cliente.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenCancelacion.token)
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(null) }
    }
}
