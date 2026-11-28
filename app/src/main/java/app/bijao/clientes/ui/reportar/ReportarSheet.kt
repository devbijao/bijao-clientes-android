package app.bijao.clientes.ui.reportar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.ReporteInput
import app.bijao.clientes.core.network.ApiClient
import app.bijao.clientes.core.network.datoOLanzar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Qué se reporta: una persona (por su @) o una reseña de un negocio (por su `ref`). */
sealed interface ObjetoReporte {
    data class Persona(val usuario: String) : ObjetoReporte
    data class Resena(val negocioId: String, val ref: String) : ObjetoReporte
}

private val MOTIVOS = listOf(
    "OFENSIVO" to "Contenido ofensivo o inapropiado",
    "SUPLANTACION" to "Se hace pasar por otra persona",
    "SPAM" to "Spam o publicidad",
    "OTRO" to "Otro motivo",
)

/**
 * «Reportar» (Apple guideline 1.2 y su equivalente en Play): el contenido de otros
 * usuarios (perfil, foto, portada, info, reseñas) se puede reportar además de bloquear.
 * Llega al equipo de Bijao, que lo revisa en menos de 24 horas. Gemelo de
 * `ReportarView` en iOS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportarSheet(objeto: ObjetoReporte, titulo: String, onCerrar: () -> Unit) {
    val scope = rememberCoroutineScope()
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var motivo by remember { mutableStateOf<String?>(null) }
    var detalle by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var gracias by remember { mutableStateOf<String?>(null) }

    fun enviar() {
        val m = motivo ?: return
        scope.launch {
            enviando = true
            error = null
            try {
                val texto = detalle.trim().ifBlank { null }
                val datos = when (objeto) {
                    is ObjetoReporte.Persona -> ReporteInput("PERSONA", m, texto, usuario = objeto.usuario)
                    is ObjetoReporte.Resena -> ReporteInput("RESENA", m, texto, negocio = objeto.negocioId, resena = objeto.ref)
                }
                gracias = ApiClient.service.reportar(datos).datoOLanzar().mensaje
                    .ifBlank { "Gracias por avisarnos. Lo revisaremos en las próximas 24 horas." }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                error = e.message ?: "No pudimos enviar el reporte. Inténtalo de nuevo."
            } finally {
                enviando = false
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = estado) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val g = gracias
            if (g != null) {
                Text("Gracias", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(g, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onCerrar, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Listo") }
                return@Column
            }
            Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("¿Qué está pasando?", style = MaterialTheme.typography.labelLarge)
            MOTIVOS.forEach { (clave, texto) ->
                Row(Modifier.fillMaxWidth().clickable { motivo = clave }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = motivo == clave, onClick = { motivo = clave })
                    Text(texto)
                }
            }
            OutlinedTextField(detalle, { if (it.length <= 500) detalle = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cuéntanos más (opcional)") }, minLines = 2, maxLines = 5)
            Text("El equipo de Bijao lo revisa en menos de 24 horas. No le diremos quién lo reportó.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(onClick = { enviar() }, enabled = motivo != null && !enviando,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                if (enviando) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Enviar reporte", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
