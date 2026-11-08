package app.bijao.clientes.ui.inicio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.Perfil
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.ui.beneficios.CodigoQR
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/**
 * A propósito mínima: lo que ya se ve en Beneficios (tarjetas) o tendría su
 * propia pantalla (novedades, la campana) no se repite acá. Inicio es la
 * bienvenida + el atajo más usado del día a día -- mostrar el código en
 * caja -- no un segundo resumen de datos que ya viven en otra pestaña.
 * Equivalente de `InicioView.swift`.
 */
@Composable
fun InicioScreen(
    perfil: Perfil,
    negociosVinculadosAlRegistrarse: Int = 0,
    onToastVinculadosMostrado: () -> Unit = {},
    onAbrirNotificaciones: () -> Unit,
    onDividirCuenta: () -> Unit,
    onVerWallet: () -> Unit,
) {
    var sinLeer by remember { mutableStateOf(0) }
    var mostrarToastVinculados by remember { mutableStateOf(negociosVinculadosAlRegistrarse > 0) }

    LaunchedEffect(Unit) {
        sinLeer = try {
            ApiClient.service.notificaciones(limite = 20).body()?.sinLeer ?: 0
        } catch (e: Exception) {
            0
        }
        // Se avisa al padre en cuanto se decide mostrarlo, no cuando se oculta --
        // así, si esta pantalla se recompone (cambiar de pestaña y volver) antes
        // de que termine la animación, el valor ya está en 0 y no se repite.
        if (negociosVinculadosAlRegistrarse > 0) onToastVinculadosMostrado()
    }

    LaunchedEffect(mostrarToastVinculados) {
        if (!mostrarToastVinculados) return@LaunchedEffect
        delay(2600)
        mostrarToastVinculados = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Cabecera(perfil = perfil, sinLeer = sinLeer, onAbrirNotificaciones = onAbrirNotificaciones)

            Spacer(Modifier.padding(top = 20.dp))
            Atajos(onDividirCuenta = onDividirCuenta, onVerWallet = onVerWallet)

            // Espacio flexible antes del bloque del código (menos que el de
            // abajo) -- así el texto queda justo arriba del QR, no pegado al
            // encabezado, y el conjunto se asienta un poco por encima del
            // centro de la pantalla en vez de perfectamente centrado.
            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Muestra este código en caja", style = MaterialTheme.typography.titleMedium)
                CodigoQR()
            }

            Spacer(Modifier.weight(1.6f))
        }

        AnimatedVisibility(
            visible = mostrarToastVinculados,
            exit = fadeOut() + scaleOut(targetScale = 0.6f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.85f), CircleShape)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(
                    if (negociosVinculadosAlRegistrarse == 1) "Vinculamos tu tarjeta de 1 negocio"
                    else "Vinculamos tus tarjetas de $negociosVinculadosAlRegistrarse negocios",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun Atajos(onDividirCuenta: () -> Unit, onVerWallet: () -> Unit) {
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
        AtajoCard(Icons.AutoMirrored.Filled.CallSplit, "Dividir cuenta", onDividirCuenta, Modifier.weight(1f))
        AtajoCard(Icons.Filled.AccountBalanceWallet, "Mi Wallet", onVerWallet, Modifier.weight(1f))
    }
}

@Composable
private fun AtajoCard(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(top = 8.dp))
            Text(texto, style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun Cabecera(perfil: Perfil, sinLeer: Int, onAbrirNotificaciones: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (perfil.avatarUrl != null) {
                AsyncImage(
                    model = perfil.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text(
            "Hola" + (perfil.nombre?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )

        IconButton(onClick = onAbrirNotificaciones) {
            BadgedBox(badge = { if (sinLeer > 0) Badge() }) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = if (sinLeer > 0) "Notificaciones, $sinLeer sin leer" else "Notificaciones",
                )
            }
        }
    }
}
