package app.bijao.clientes.ui.explorar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.DejarResenaInput
import app.bijao.clientes.core.models.Resena
import app.bijao.clientes.core.models.ResenasResponse
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import app.bijao.clientes.core.ui.Estrellas
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/** Promedio a la izquierda, dos reseñas recientes a la derecha -- equivalente
 * de `ResumenResenas.swift`. El listado completo vive en [TodasResenasScreen]. */
@Composable
fun ResumenResenas(respuesta: ResenasResponse, onVerTodas: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Reseñas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (respuesta.conteo > 0) {
                    androidx.compose.material3.TextButton(onClick = onVerTodas) { Text("Ver todas") }
                }
            }

            if (respuesta.conteo == 0) {
                Text(
                    "Todavía no hay reseñas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(verticalAlignment = Alignment.Top) {
                    Column(
                        modifier = Modifier.width(92.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            respuesta.promedio?.let { "%.1f".format(it) } ?: "—",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Estrellas(valor = respuesta.promedio ?: 0.0)
                        Text(
                            "(${respuesta.conteo} reseña${if (respuesta.conteo == 1) "" else "s"})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        respuesta.data.take(2).forEach { resena -> ResenaRow(resena) }
                    }
                }
            }
        }
    }
}

@Composable
fun ResenaRow(resena: Resena) {
    Row(verticalAlignment = Alignment.Top) {
        AsyncImage(
            model = resena.avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(38.dp).clip(CircleShape),
        )
        Spacer(Modifier.padding(start = 10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(resena.autor, style = MaterialTheme.typography.labelLarge)
                if (resena.esMia) {
                    Spacer(Modifier.padding(start = 6.dp))
                    Text(
                        "Tú",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            resena.comentario?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            resena.respuesta?.let {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Estrellas(valor = resena.calificacion.toDouble(), tamano = 12.dp)
    }
}

/** Solo aparece si `puedeResenar` -- el backend exige una compra en ese
 * negocio para habilitarlo. Equivalente de `FormularioResena` en iOS. */
@Composable
fun FormularioResena(negocioId: String, onGuardado: () -> Unit) {
    var calificacion by remember { mutableIntStateOf(5) }
    var comentario by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Deja tu opinión", style = MaterialTheme.typography.labelLarge)
            Row {
                for (i in 1..5) {
                    IconButton(onClick = { calificacion = i }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (i <= calificacion) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "$i estrella${if (i == 1) "" else "s"}",
                            tint = androidx.compose.ui.graphics.Color(0xFFFFC107),
                        )
                    }
                }
            }
            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it },
                placeholder = { Text("Cuéntales a otros cómo te fue (opcional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.padding(top = 8.dp))
            Button(
                onClick = {
                    enviando = true
                    scope.launch {
                        try {
                            ApiClient.service.dejarResena(
                                negocioId,
                                DejarResenaInput(calificacion, comentario.ifBlank { null }),
                            ).datoOLanzar()
                            comentario = ""
                            onGuardado()
                        } catch (e: Exception) {
                            // Igual que iOS: si falla, la persona puede reintentar sin perder lo escrito.
                        } finally {
                            enviando = false
                        }
                    }
                },
                enabled = !enviando,
            ) {
                if (enviando) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Text("Publicar reseña")
            }
        }
    }
}

/** "Ver todas" desde la ficha -- arranca con lo ya cargado ahí para no
 * mostrar un spinner de entrada. Equivalente de `TodasResenasView.swift`. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TodasResenasScreen(negocioId: String, resumenInicial: ResenasResponse, onAtras: () -> Unit) {
    var respuesta by remember { mutableStateOf(resumenInicial) }

    LaunchedEffect(negocioId) {
        try {
            respuesta = ApiClient.service.resenas(negocioId).datoOLanzar()
        } catch (e: Exception) {
            // Se queda con el resumen inicial si el refetch falla.
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reseñas") },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        respuesta.promedio?.let { "%.1f".format(it) } ?: "—",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Estrellas(valor = respuesta.promedio ?: 0.0)
                    Text(
                        "${respuesta.conteo} reseña${if (respuesta.conteo == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(respuesta.data) { resena -> ResenaRow(resena) }
        }
    }
}
