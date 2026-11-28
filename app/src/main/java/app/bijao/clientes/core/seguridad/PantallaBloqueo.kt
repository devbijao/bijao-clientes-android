package app.bijao.clientes.core.seguridad

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import app.bijao.clientes.R
import kotlinx.coroutines.launch

/** Lo que se ve con la app bloqueada. Pide la huella sola al aparecer; si se
 * cancela, queda el botón (no vuelve a saltar en bucle). Equivalente de
 * `PantallaBloqueo` en iOS. */
@Composable
fun PantallaBloqueo() {
    val activity = LocalContext.current as? FragmentActivity
    val scope = rememberCoroutineScope()
    var intentando by remember { mutableStateOf(false) }

    fun intentar() {
        val a = activity ?: return
        if (intentando) return
        scope.launch {
            intentando = true
            BloqueoApp.desbloquear(a)
            intentando = false
        }
    }

    LaunchedEffect(Unit) { intentar() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Se traga los toques: la app de abajo no se puede usar.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(R.drawable.logo_completo), contentDescription = "Bijao", modifier = Modifier.width(180.dp))
        Spacer(Modifier.height(28.dp))
        Text("Bijao está bloqueado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Para proteger tus datos, desbloquéalo con tu huella, tu rostro o el bloqueo del teléfono.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(40.dp))
        Button(onClick = ::intentar, enabled = !intentando, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("Desbloquear", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
