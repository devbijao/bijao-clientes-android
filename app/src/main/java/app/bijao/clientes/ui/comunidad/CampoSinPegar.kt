package app.bijao.clientes.ui.comunidad

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.text.TextWatcher
import android.util.TypedValue
import android.view.DragEvent
import android.view.Gravity
import android.widget.EditText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

/** Más caracteres que esto entrando de una sola vez = pegado (o la sugerencia
 * del portapapeles del teclado). Mismo umbral que la web (`SALTO_MAX`). */
const val SALTO_MAX_COMUNIDAD = 25
const val MAX_TEXTO_COMUNIDAD = 500

/**
 * Campo de respuesta de Comunidad: solo escribir, no pegar (D17 de
 * MDs/plan_comunidad.md -- la preocupación es que respondan con IA).
 *
 * - Pegar desde el menú contextual: `onTextContextMenuItem` lo descarta.
 * - Arrastrar y soltar: `onDragEvent` no acepta nada.
 * - Portapapeles del teclado (el chip de Gboard llega como un `commitText`
 *   normal): un `InputFilter` rechaza cualquier inserción de más de
 *   `SALTO_MAX_COMUNIDAD` caracteres de golpe.
 *
 * Es una barrera, no una garantía: el moderador sigue decidiendo.
 */
@Composable
fun CampoSinPegar(
    texto: String,
    onCambio: (String) -> Unit,
    placeholder: String,
    alIntentarPegar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorTexto = MaterialTheme.colorScheme.onSurface.toArgb()
    val colorPista = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    AndroidView(
        modifier = modifier,
        factory = { context ->
            EditTextSinPegar(context).apply {
                this.alIntentarPegar = alIntentarPegar
                this.onCambio = onCambio
            }
        },
        update = { vista ->
            vista.alIntentarPegar = alIntentarPegar
            vista.onCambio = onCambio
            vista.hint = placeholder
            vista.setTextColor(colorTexto)
            vista.setHintTextColor(colorPista)
            if (vista.text.toString() != texto) vista.ponerTexto(texto)
        },
    )
}

@SuppressLint("ViewConstructor")
class EditTextSinPegar(context: Context) : EditText(context) {
    var alIntentarPegar: () -> Unit = {}
    var onCambio: (String) -> Unit = {}

    /** `setText` también pasa por los filtros: sin esto, restaurar más de 25
     * caracteres (p. ej. al rotar) contaría como pegado. */
    private var programatico = false

    init {
        background = null
        gravity = Gravity.TOP or Gravity.START
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        val px = (12 * resources.displayMetrics.density).toInt()
        setPadding(px + px / 3, px, px, px)
        minLines = 1
        maxLines = 6
        inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        filters = arrayOf(
            object : InputFilter {
                override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
                    if (programatico) return null
                    if (end - start > SALTO_MAX_COMUNIDAD) {
                        post { alIntentarPegar() }
                        return ""
                    }
                    return null
                }
            },
            InputFilter.LengthFilter(MAX_TEXTO_COMUNIDAD),
        )
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!programatico) onCambio(s?.toString().orEmpty())
            }
        })
    }

    fun ponerTexto(nuevo: String) {
        programatico = true
        setText(nuevo)
        setSelection(nuevo.length)
        programatico = false
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        if (id == android.R.id.paste || id == android.R.id.pasteAsPlainText) {
            alIntentarPegar()
            return true
        }
        return super.onTextContextMenuItem(id)
    }

    override fun onDragEvent(event: DragEvent): Boolean {
        if (event.action == DragEvent.ACTION_DROP) alIntentarPegar()
        return false
    }
}
