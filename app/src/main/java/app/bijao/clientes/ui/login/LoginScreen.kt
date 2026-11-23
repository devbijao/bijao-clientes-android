package app.bijao.clientes.ui.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bijao.clientes.R
import app.bijao.clientes.core.AppConfig
import app.bijao.clientes.core.auth.AuthManager
import app.bijao.clientes.core.auth.traducirErrorAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Metodo { CELULAR, CORREO }
private enum class Cargando { ENVIAR, VERIFICAR, GOOGLE }

private val Redondeo = RoundedCornerShape(16.dp)

/**
 * Equivalente de `LoginView.swift` en bijao-clientes-ios (y de
 * `entrar/page.tsx` en la web): logo, frase que rota, selector
 * Celular/Correo, botón principal azul, Google y la onda azul al pie. Sin
 * botón de Apple en Android. Google OAuth va por Custom Tabs (ver
 * `AuthManager.iniciarSesionConGoogle`/`MainActivity`).
 */
@Composable
fun LoginScreen() {
    var metodo by remember { mutableStateOf(Metodo.CELULAR) }
    var celular by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var enviado by remember { mutableStateOf<Metodo?>(null) }
    var cargando by remember { mutableStateOf<Cargando?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var espera by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(espera) {
        if (espera > 0) {
            delay(1000)
            espera -= 1
        }
    }

    fun manejarError(e: Exception) {
        val traducido = traducirErrorAuth(e)
        error = traducido.mensaje
        traducido.esperar?.let { espera = it }
    }

    fun enviarCodigo() {
        cargando = Cargando.ENVIAR
        error = null
        scope.launch {
            try {
                if (metodo == Metodo.CELULAR) {
                    AuthManager.enviarCodigoSMS(celular)
                } else {
                    AuthManager.enviarCodigoCorreo(email.trim())
                }
                enviado = metodo
            } catch (e: Exception) {
                manejarError(e)
            } finally {
                cargando = null
            }
        }
    }

    fun verificar(para: Metodo) {
        if (cargando != null) return
        cargando = Cargando.VERIFICAR
        error = null
        scope.launch {
            try {
                if (para == Metodo.CELULAR) {
                    AuthManager.verificarCodigoSMS(celular, codigo)
                } else {
                    AuthManager.verificarCodigoCorreo(email.trim(), codigo)
                }
                // La sesión queda establecida por supabase-kt; RootScreen
                // reacciona solo al cambio de sessionStatus.
            } catch (e: Exception) {
                manejarError(e)
                codigo = ""
            } finally {
                cargando = null
            }
        }
    }

    fun entrarConGoogle() {
        cargando = Cargando.GOOGLE
        error = null
        scope.launch {
            try {
                // El Custom Tab se abre solo y la sesión se completa cuando
                // MainActivity recibe el deep link de vuelta.
                AuthManager.iniciarSesionConGoogle()
            } catch (e: Exception) {
                manejarError(e)
            } finally {
                cargando = null
            }
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Fondo()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Encabezado(enviado)

            val paraCodigo = enviado
            if (paraCodigo != null) {
                FormularioCodigo(
                    metodo = paraCodigo,
                    destino = if (paraCodigo == Metodo.CELULAR) "+57 $celular" else email.trim(),
                    codigo = codigo,
                    verificando = cargando == Cargando.VERIFICAR,
                    onCodigo = { nuevo ->
                        codigo = nuevo.filter(Char::isDigit).take(6)
                        if (codigo.length == 6) verificar(paraCodigo)
                    },
                    onCambiar = { enviado = null; codigo = ""; error = null },
                )
            } else {
                SelectorMetodo(metodo) { metodo = it; error = null }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (metodo == Metodo.CELULAR) {
                        CampoCelular(celular) { celular = it.filter(Char::isDigit).take(10) }
                    } else {
                        CampoCorreo(email) { email = it }
                    }
                    BotonPrimario(
                        titulo = "Enviar código",
                        activo = if (metodo == Metodo.CELULAR) celular.length == 10 else email.contains("@"),
                        cargando = cargando == Cargando.ENVIAR,
                        bloqueado = cargando != null,
                        espera = espera,
                        onClick = ::enviarCodigo,
                    )
                }

                Separador()
                BotonGoogle(cargando = cargando == Cargando.GOOGLE, habilitado = cargando == null, onClick = ::entrarConGoogle)
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (enviado == null) {
                Confianza()
                Legal()
            }
        }
    }
}

@Composable
private fun Fondo() {
    val azul = MaterialTheme.colorScheme.primary
    // Trazo de color suave arriba a la derecha + dos olas al pie: el acento
    // de marca sin competir con el formulario (igual que `OndasFondo` en iOS).
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            Brush.radialGradient(
                colors = listOf(azul.copy(alpha = 0.10f), azul.copy(alpha = 0f)),
                center = Offset(size.width, 0f),
                radius = 420.dp.toPx(),
            ),
        )

        val w = size.width
        val altoTrasera = 240.dp.toPx()
        val topT = size.height - altoTrasera
        drawPath(
            Path().apply {
                moveTo(0f, topT + altoTrasera * 0.55f)
                cubicTo(
                    w * 0.33f, topT + altoTrasera * 0.15f,
                    w * 0.66f, topT + altoTrasera * 0.70f,
                    w, topT + altoTrasera * 0.30f,
                )
                lineTo(w, size.height)
                lineTo(0f, size.height)
                close()
            },
            color = azul.copy(alpha = 0.16f),
        )

        val altoDelantera = 170.dp.toPx()
        val topD = size.height - altoDelantera
        drawPath(
            Path().apply {
                moveTo(0f, topD + altoDelantera * 0.35f)
                cubicTo(
                    w * 0.33f, topD + altoDelantera * 0.75f,
                    w * 0.66f, topD + altoDelantera * 0.05f,
                    w, topD + altoDelantera * 0.55f,
                )
                lineTo(w, size.height)
                lineTo(0f, size.height)
                close()
            },
            color = azul.copy(alpha = 0.9f),
        )
    }
}

@Composable
private fun Encabezado(enviado: Metodo?) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.logo_completo),
            contentDescription = "Bijao",
            modifier = Modifier.height(90.dp),
        )
        Spacer(Modifier.height(48.dp))
        Text(
            when (enviado) {
                Metodo.CELULAR -> "Revisa tus mensajes"
                Metodo.CORREO -> "Revisa tu correo"
                null -> "Ingresa ahora mismo"
            },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (enviado == null) {
            Spacer(Modifier.height(20.dp))
            FraseCiclica()
        }
    }
}

/** Dos renglones centrados: la palabra de arriba rota en bucle (sale hacia
 * arriba, la siguiente entra desde abajo), igual que `FraseCiclica.swift`. */
@Composable
private fun FraseCiclica() {
    val palabras = remember { listOf("sellos", "recompensas", "beneficios", "descuentos") }
    var indice by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1800)
            indice = (indice + 1) % palabras.size
        }
    }
    val estilo = MaterialTheme.typography.bodyMedium
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = indice,
            transitionSpec = {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            },
            label = "frase",
        ) { i -> Text(palabras[i], style = estilo, color = color) }
        Text("De todos los negocios donde compras", style = estilo, color = color)
    }
}

@Composable
private fun SelectorMetodo(metodo: Metodo, onCambio: (Metodo) -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = metodo == Metodo.CELULAR,
                onClick = { onCambio(Metodo.CELULAR) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                icon = {},
            ) { Text("Celular") }
            SegmentedButton(
                selected = metodo == Metodo.CORREO,
                onClick = { onCambio(Metodo.CORREO) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                icon = {},
            ) { Text("Correo") }
        }
    }
}

@Composable
private fun coloresCampo() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
)

@Composable
private fun CampoCelular(valor: String, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        placeholder = { Text("Número de celular") },
        leadingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 14.dp, end = 4.dp),
            ) {
                Icon(Icons.Filled.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text("+57", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                VerticalDivider(Modifier.height(18.dp), color = MaterialTheme.colorScheme.outline)
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        shape = Redondeo,
        colors = coloresCampo(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CampoCorreo(valor: String, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        placeholder = { Text("Correo") },
        leadingIcon = {
            Icon(Icons.Filled.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        shape = Redondeo,
        colors = coloresCampo(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BotonPrimario(
    titulo: String,
    activo: Boolean,
    cargando: Boolean,
    bloqueado: Boolean,
    espera: Int,
    onClick: () -> Unit,
) {
    val azul = MaterialTheme.colorScheme.primary
    Button(
        onClick = onClick,
        enabled = activo && !bloqueado && espera == 0,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = azul,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = azul.copy(alpha = 0.4f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        when {
            cargando -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            espera > 0 -> Text("Espera ${espera}s", fontWeight = FontWeight.SemiBold)
            else -> Text(titulo, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Separador() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        Text(
            "o continúa con",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun BotonGoogle(cargando: Boolean, habilitado: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        shape = Redondeo,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        if (cargando) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Image(painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Continuar con Google", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Confianza() {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "Tus datos están seguros con nosotros.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Legal() {
    val abrir = LocalUriHandler.current
    val gris = MaterialTheme.colorScheme.onSurfaceVariant
    val estilo = MaterialTheme.typography.labelSmall
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Al continuar, aceptas nuestros:", style = estilo, color = gris)
        Row(verticalAlignment = Alignment.CenterVertically) {
            EnlaceLegal("Términos") { abrir.openUri(AppConfig.Legal.TERMINOS) }
            Text(" · ", style = estilo, color = gris)
            EnlaceLegal("Privacidad") { abrir.openUri(AppConfig.Legal.PRIVACIDAD) }
            Text(" · ", style = estilo, color = gris)
            EnlaceLegal("Cookies") { abrir.openUri(AppConfig.Legal.COOKIES) }
        }
    }
}

@Composable
private fun EnlaceLegal(texto: String, onClick: () -> Unit) {
    Text(
        texto,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 2.dp),
    )
}

@Composable
private fun FormularioCodigo(
    metodo: Metodo,
    destino: String,
    codigo: String,
    verificando: Boolean,
    onCodigo: (String) -> Unit,
    onCambiar: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Te mandamos un código a $destino.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = codigo,
            onValueChange = onCodigo,
            placeholder = {
                Text(
                    "· · · · · ·",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 30.sp,
                )
            },
            singleLine = true,
            enabled = !verificando,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            shape = RoundedCornerShape(18.dp),
            colors = coloresCampo(),
            modifier = Modifier.fillMaxWidth(),
        )
        if (verificando) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
        TextButton(onClick = onCambiar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (metodo == Metodo.CELULAR) "Usar otro número" else "Usar otro correo", fontWeight = FontWeight.Medium)
        }
    }
}
