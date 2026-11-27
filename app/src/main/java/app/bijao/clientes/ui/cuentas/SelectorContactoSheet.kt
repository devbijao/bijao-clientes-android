package app.bijao.clientes.ui.cuentas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bijao.clientes.core.models.ContactoItem
import coil.compose.AsyncImage
import java.text.Normalizer

private fun normalizar(s: String) =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "").lowercase()

/**
 * Elegir a quién le toca una parte de la cuenta, como «Nuevo chat» de
 * WhatsApp: buscador y lista con foto, nombre y @usuario. Antes era un
 * DropdownMenu que, con cientos de contactos, no terminaba nunca.
 * Equivalente de `SelectorContactoView` en iOS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorContactoSheet(
    contactos: List<ContactoItem>,
    elegido: String?,
    onElegir: (ContactoItem?) -> Unit,
    onCerrar: () -> Unit,
) {
    var busqueda by remember { mutableStateOf("") }
    val q = normalizar(busqueda.trim().trimStart('@'))
    val lista = contactos
        .sortedBy { normalizar(it.nombre ?: it.usuario ?: "") }
        .filter { c -> q.isEmpty() || listOfNotNull(c.nombre, c.usuario).any { normalizar(it).contains(q) } }

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Text("Elegir contacto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp))
            OutlinedTextField(
                value = busqueda, onValueChange = { busqueda = it },
                placeholder = { Text("Nombre o @usuario") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true, shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            )
            LazyColumn(Modifier.fillMaxWidth()) {
                if (elegido != null) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().clickable { onElegir(null) }.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text("Quitar contacto", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }
                item {
                    Text("Tus contactos en Bijao", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                }
                items(lista, key = { it.id }) { c ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onElegir(c) }.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center) {
                            Text((c.nombre ?: c.usuario ?: "?").take(1).uppercase(), style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (c.avatarUrl != null) {
                                AsyncImage(c.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp))
                            }
                        }
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(c.nombre ?: "@${c.usuario.orEmpty()}", style = MaterialTheme.typography.bodyLarge)
                            if (c.nombre != null && c.usuario != null) {
                                Text("@${c.usuario}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (c.id == elegido) Icon(Icons.Filled.Check, contentDescription = "Elegido", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                if (lista.isEmpty()) {
                    item {
                        Text(if (busqueda.isBlank()) "Aún no tienes contactos." else "Nadie coincide con «$busqueda».",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
                    }
                }
            }
        }
    }
}
