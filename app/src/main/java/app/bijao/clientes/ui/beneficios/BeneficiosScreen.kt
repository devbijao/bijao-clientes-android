package app.bijao.clientes.ui.beneficios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.CanjeData
import app.bijao.clientes.core.models.CanjeInput
import app.bijao.clientes.core.models.Tarjeta
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.ui.explorar.EstadoVacio
import kotlinx.coroutines.launch

/** Tarjetas de sellos de todos los negocios vinculados -- equivalente de
 * `BeneficiosView.swift`. "Ver mis vales" y "Mostrar mi código" son filas al
 * final de la misma lista, no íconos en una barra de arriba. */
@Composable
fun BeneficiosScreen(onVerVales: () -> Unit, onMostrarQR: () -> Unit) {
    val scope = rememberCoroutineScope()

    var tarjetas by remember { mutableStateOf<List<Tarjeta>>(emptyList()) }
    var logosPorNegocio by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var canjeandoId by remember { mutableStateOf<String?>(null) }
    var resultadoCanje by remember { mutableStateOf<CanjeData?>(null) }
    var errorCanje by remember { mutableStateOf<String?>(null) }

    suspend fun cargarLogos() {
        val idsUnicos = tarjetas.map { it.negocio.id }.distinct().filterNot { logosPorNegocio.containsKey(it) }
        idsUnicos.forEach { id ->
            try {
                val detalle = ApiClient.service.negocioDetalle(id).datoOLanzar().data
                detalle.logoUrl?.let { logosPorNegocio = logosPorNegocio + (id to it) }
            } catch (e: Exception) {
                // Sin logo no es grave -- la tarjeta cae al monograma.
            }
        }
    }

    suspend fun cargar() {
        cargando = true
        error = null
        try {
            tarjetas = ApiClient.service.beneficios().datoOLanzar().data
            scope.launch { cargarLogos() }
        } catch (e: Exception) {
            error = e.message ?: "No pudimos cargar tus sellos."
        } finally {
            cargando = false
        }
    }

    LaunchedEffect(Unit) { cargar() }

    resultadoCanje?.let { resultado ->
        CanjeResultSheet(resultado = resultado) {
            resultadoCanje = null
            scope.launch { cargar() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            cargando && tarjetas.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null && tarjetas.isEmpty() -> EstadoVacio(
                icono = Icons.Filled.WifiOff,
                titulo = "No pudimos cargar tus sellos",
                descripcion = error ?: "",
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (tarjetas.isEmpty()) {
                    item {
                        EstadoVacio(
                            icono = Icons.Filled.CardGiftcard,
                            titulo = "Aún no tienes tarjetas",
                            descripcion = "Compra en un negocio Bijao y tu primera tarjeta aparece aquí.",
                        )
                    }
                } else {
                    items(tarjetas, key = { it.recompensaId }) { tarjeta ->
                        TarjetaCard(
                            tarjeta = tarjeta,
                            canjeando = canjeandoId == tarjeta.recompensaId,
                            logoUrl = logosPorNegocio[tarjeta.negocio.id],
                        ) {
                                scope.launch {
                                    canjeandoId = tarjeta.recompensaId
                                    try {
                                        resultadoCanje = ApiClient.service.canjear(
                                            CanjeInput(tarjeta.negocio.id, tarjeta.recompensaId),
                                        ).datoOLanzar().data
                                    } catch (e: Exception) {
                                        errorCanje = e.message ?: "No pudimos canjear."
                                    } finally {
                                        canjeandoId = null
                                    }
                                }
                        }
                    }
                }

                item { FilaAtajo(icono = Icons.Filled.QrCode, texto = "Mostrar mi código", onClick = onMostrarQR) }
                item { FilaAtajo(icono = Icons.Filled.ConfirmationNumber, texto = "Ver mis vales", onClick = onVerVales) }
            }
        }

        errorCanje?.let { mensaje ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { errorCanje = null },
                title = { Text("No pudimos canjear") },
                text = { Text(mensaje) },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { errorCanje = null }) { Text("Ok") }
                },
            )
        }
    }
}

@Composable
private fun FilaAtajo(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(texto, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 12.dp).weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
