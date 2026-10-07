package app.bijao.clientes.core.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import app.bijao.clientes.core.models.CompraDetalle
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Genera el PDF de una factura a partir de los mismos datos que ya muestra
 * `CompraDetalleScreen`. No existe un endpoint de PDF en el backend -- las
 * facturas del CRM se generan con jsPDF en el navegador, algo que no aplica
 * a una app nativa -- así que se arma acá con `PdfDocument` (equivalente
 * directo de `UIGraphicsPDFRenderer` en iOS), sin depender de la red.
 */
private const val ANCHO_CARTA = 612
private const val ALTO_CARTA = 792
private const val MARGEN = 48f
private val AZUL_BIJAO = Color.rgb(0, 123, 255)

fun generarFacturaPDF(detalle: CompraDetalle): ByteArray {
    val documento = PdfDocument()
    val pagina = documento.startPage(PdfDocument.PageInfo.Builder(ANCHO_CARTA, ALTO_CARTA, 1).create())
    val canvas = pagina.canvas
    var y = MARGEN

    val paintNormal = Paint().apply { textSize = 12f; color = Color.BLACK; isAntiAlias = true }
    val paintNegrita = Paint(paintNormal).apply { typeface = Typeface.DEFAULT_BOLD }
    val paintTitulo = Paint().apply { textSize = 24f; color = AZUL_BIJAO; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
    val paintSubtitulo = Paint().apply { textSize = 16f; color = Color.BLACK; isAntiAlias = true }
    val paintSeccion = Paint(paintNegrita).apply { textSize = 14f }
    val paintTotal = Paint().apply { textSize = 16f; color = AZUL_BIJAO; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }

    fun escribir(texto: String, paint: Paint, salto: Float) {
        canvas.drawText(texto, MARGEN, y, paint)
        y += salto
    }

    escribir("Bijao", paintTitulo, 34f)
    escribir(detalle.negocio ?: "Negocio", paintSubtitulo, 30f)

    detalle.numeroFactura?.let { escribir("Factura: $it", paintNormal, 18f) }
    detalle.fecha?.let { fecha ->
        parsearISO(fecha)?.let { escribir("Fecha: ${formatearFecha(it)}", paintNormal, 18f) }
    }
    y += 20f

    escribir("Productos", paintSeccion, 24f)
    detalle.items.forEach { item ->
        val nombre = item.nombre ?: "Producto"
        val linea = "$nombre  —  ${"%.0f".format(item.cantidad)} × ${formatoCOP(item.precioUnitario)}"
        escribir(linea, paintNormal, 20f)
    }
    y += 16f

    escribir("Subtotal: ${formatoCOP(detalle.subtotal)}", paintNormal, 18f)
    if (detalle.descuento > 0) {
        escribir("Descuento: -${formatoCOP(detalle.descuento)}", paintNormal, 18f)
    }
    y += 8f
    escribir("Total: ${formatoCOP(detalle.total)}", paintTotal, 0f)

    documento.finishPage(pagina)
    val salida = java.io.ByteArrayOutputStream()
    documento.writeTo(salida)
    documento.close()
    return salida.toByteArray()
}

private fun formatoCOP(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).apply { maximumFractionDigits = 0 }.format(valor)

/** Postgres manda `fecha_emision` con o sin fracción de segundo según la fila
 * -- `Instant.parse` de Java ya tolera ambos formatos sin necesitar dos
 * parsers distintos como en iOS. */
fun parsearISO(texto: String): Instant? = try {
    Instant.parse(texto)
} catch (e: Exception) {
    null
}

fun formatearFecha(instante: Instant): String =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-CO"))
        .withZone(ZoneId.systemDefault())
        .format(instante)

/** Genera el PDF y lo deja en el caché -- tanto abrirlo con un visor externo
 * como compartirlo necesitan un archivo accesible por `content://` (vía
 * `FileProvider`), no los bytes en memoria. */
fun generarYGuardarFacturaPDF(context: Context, detalle: CompraDetalle): File? {
    return try {
        val datos = generarFacturaPDF(detalle)
        val carpeta = File(context.cacheDir, "facturas").apply { mkdirs() }
        val archivo = File(carpeta, "Factura-${detalle.numeroFactura ?: detalle.id}.pdf")
        FileOutputStream(archivo).use { it.write(datos) }
        archivo
    } catch (e: Exception) {
        null
    }
}
