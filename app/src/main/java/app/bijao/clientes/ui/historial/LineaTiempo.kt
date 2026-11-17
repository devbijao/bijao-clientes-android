package app.bijao.clientes.ui.historial

import app.bijao.clientes.core.models.Compra
import app.bijao.clientes.core.models.ConsumoApp
import app.bijao.clientes.core.pdf.parsearISO

/** Línea de tiempo del historial: compras y consumos de suscripción en una
 * sola lista, del más reciente al más viejo. Puerto exacto de
 * `mezclarLineaDeTiempo` en `frontend/lib/app-clientes/linea-tiempo.ts`
 * (también portado a `LineaTiempo.swift` en iOS).
 *
 * Cada fuente se pagina por separado en el servidor, así que mezclar "lo
 * cargado" de ambas sin cuidado deja huecos: si ya llegaron 50 compras pero
 * solo 10 consumos, los consumos más viejos todavía no están y la lista
 * parecería saltarse movimientos. Mientras una fuente tenga más por traer,
 * solo se muestra hasta la fecha del último elemento que ya cargó. */
sealed class Movimiento(val id: String, val fecha: String?) {
    class DeCompra(val compra: Compra) : Movimiento("c-${compra.id}", compra.fecha)
    class DeConsumo(val consumo: ConsumoApp) : Movimiento("s-${consumo.id}", consumo.fecha)
}

private fun instante(iso: String?): Long = iso?.let { parsearISO(it)?.toEpochMilli() } ?: 0L

fun mezclarLineaDeTiempo(
    compras: List<Compra>,
    consumos: List<ConsumoApp>,
    hayMasCompras: Boolean,
    hayMasConsumos: Boolean,
): List<Movimiento> {
    val todos = (compras.map { Movimiento.DeCompra(it) } + consumos.map { Movimiento.DeConsumo(it) })
        .sortedByDescending { instante(it.fecha) }

    val piso = buildList {
        if (hayMasCompras && compras.isNotEmpty()) add(instante(compras.last().fecha))
        if (hayMasConsumos && consumos.isNotEmpty()) add(instante(consumos.last().fecha))
    }
    val corte = piso.maxOrNull() ?: return todos
    return todos.filter { instante(it.fecha) >= corte }
}
