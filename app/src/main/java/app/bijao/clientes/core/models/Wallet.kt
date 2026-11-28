package app.bijao.clientes.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Espejo de `backend/routers/app_wallet.py` -- Wallet (W3). El periodo
 * (día/semana/mes) por defecto lo decide el servidor según la actividad de
 * los últimos 90 días, para que web, iOS y Android muestren siempre lo mismo.
 * Mismo shape que `Wallet.swift`.
 */
@Serializable
data class PeriodoResumen(val desde: String, val hasta: String, val ingresos: Int, val gastos: Int)

@Serializable
data class ActualWallet(val desde: String, val hasta: String, val ingresos: Int, val gastos: Int, val balance: Int)

@Serializable
data class CategoriaTotal(val categoria: String, val total: Int)

@Serializable
data class NegocioTotal(val negocio: String, val total: Int)

@Serializable
data class MovimientoWallet(
    val id: String,
    /** "VENTA" (automático, no se borra) | "MANUAL" */
    val origen: String,
    /** "INGRESO" | "GASTO" */
    val tipo: String,
    val monto: Int,
    /** "YYYY-MM-DD" */
    val fecha: String,
    val categoria: String,
    val negocio: String? = null,
    val nota: String? = null,
    /** Lo generó un movimiento recurrente (F3). */
    val recurrente: Boolean = false,
)

/** Las claves del JSON son literalmente "GASTO"/"INGRESO" (no snake_case). */
@Serializable
data class CategoriasWallet(
    @SerialName("GASTO") val gasto: List<String>,
    @SerialName("INGRESO") val ingreso: List<String>,
)

@Serializable
data class WalletResponse(
    /** El periodo efectivo: "dia" | "semana" | "mes". */
    val periodo: String,
    @SerialName("periodo_auto") val periodoAuto: String,
    val automatico: Boolean,
    val categorias: CategoriasWallet,
    /** El periodo DETALLADO (el actual salvo que se pida otro con `fecha`). */
    val actual: ActualWallet,
    @SerialName("es_actual") val esActual: Boolean,
    val serie: List<PeriodoResumen>,
    @SerialName("por_categoria") val porCategoria: List<CategoriaTotal>,
    @SerialName("por_negocio") val porNegocio: List<NegocioTotal>,
    val movimientos: List<MovimientoWallet>,
)

@Serializable
data class MovimientoInput(
    /** "INGRESO" | "GASTO" */
    val tipo: String,
    val monto: Int,
    val categoria: String,
    val nota: String? = null,
    /** "YYYY-MM-DD"; null = hoy. */
    val fecha: String? = null,
)

@Serializable
data class MovimientoResponse(val id: String, val categoria: String)

// ── Recurrentes (F3, `/api/app/wallet/recurrentes`) ──────────────────────────

/** Un ingreso o gasto que se repite. El backend genera sus movimientos al abrir
 * el Wallet (sin cron). */
@Serializable
data class RecurrenteWallet(
    val id: String,
    val tipo: String,
    val monto: Int,
    val categoria: String,
    val nota: String? = null,
    /** DIA | SEMANA | MES | CADA_N_DIAS */
    val frecuencia: String,
    @SerialName("cada_n") val cadaN: Int = 1,
    val desde: String,
    val hasta: String? = null,
    val activo: Boolean = true,
    /** La siguiente fecha en que toca (null si está en pausa o ya terminó). */
    val proxima: String? = null,
) {
    val textoFrecuencia: String get() = when (frecuencia) {
        "DIA" -> "Cada día"
        "SEMANA" -> "Cada semana"
        "MES" -> "Cada mes"
        else -> if (cadaN == 1) "Cada día" else "Cada $cadaN días"
    }
}

@Serializable
data class RecurrentesResponse(val data: List<RecurrenteWallet> = emptyList())

@Serializable
data class RecurrenteInput(
    val tipo: String,
    val monto: Int,
    val categoria: String,
    val nota: String? = null,
    val frecuencia: String,
    @SerialName("cada_n") val cadaN: Int = 1,
    val desde: String? = null,
    val hasta: String? = null,
)

@Serializable
data class RecurrenteCambioInput(val activo: Boolean)
