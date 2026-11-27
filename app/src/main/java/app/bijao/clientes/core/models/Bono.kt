package app.bijao.clientes.core.models

import app.bijao.clientes.core.network.ApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Espejo de `GET /api/app/bonos` en `backend/routers/app_bonos.py` (monorepo,
 * plan en `MDs/plan_bonos.md`). Un voucher de la billetera: regalo de Bijao,
 * premio de un evento de Comunidad o premio de una tarjeta de sellos
 * (`origen == "FIDELIDAD"`, con `tarjeta` = la recompensa que se completó).
 *
 * «VENCIDO» lo calcula el backend al leer (no hay cron): la app usa `estado`
 * tal cual, nunca decide por su cuenta si un bono sirve.
 */
@Serializable
data class Bono(
    val id: String,
    /** Ya formateado por el backend para leerse en caja. */
    val codigo: String,
    val nombre: String? = null,
    val descripcion: String? = null,
    val etiqueta: String = "",
    /** VALOR | PORCENTAJE | PRODUCTO */
    val tipo: String,
    val valor: Double? = null,
    val producto: String? = null,
    /** UNICO | SALDO (el saldo solo existe en bonos en pesos) */
    val uso: String? = null,
    val saldo: Double? = null,
    @SerialName("valor_inicial") val valorInicial: Double? = null,
    @SerialName("tope_por_compra") val topePorCompra: Double? = null,
    @SerialName("vence_en") val venceEn: String,
    /** ACTIVO | PAUSADO | USADO | VENCIDO */
    val estado: String,
    val color: String? = null,
    /** TODOS | ELEGIDOS | NEGOCIO */
    val alcance: String? = null,
    val donde: String? = null,
    /** El texto del talón: «Regalo de Bijao.», «Lo regala X.», «<evento>. Patrocina Bijao.» */
    val desprendible: String? = null,
    /** STAFF | COMUNIDAD | FIDELIDAD */
    val origen: String? = null,
    val tarjeta: String? = null,
    @SerialName("emitido_at") val emitidoAt: String? = null,
) {
    val esEnPesos: Boolean get() = tipo == "VALOR"
    val esDeSaldo: Boolean get() = esEnPesos && uso == "SALDO"
    val estaActivo: Boolean get() = estado == "ACTIVO"
    val estaPausado: Boolean get() = estado == "PAUSADO"
}

/** «Guardados»: un uso ya cobrado (comprobante BONO-#####) o un bono que
 * venció sin usarse. */
@Serializable
data class BonoGuardado(
    /** USO | VENCIDO */
    val tipo: String,
    val bono: String,
    val nombre: String? = null,
    val etiqueta: String? = null,
    val color: String? = null,
    val negocio: String? = null,
    val monto: Double? = null,
    val numero: String? = null,
    val fecha: String? = null,
)

@Serializable
data class BonosResponse(val data: List<Bono> = emptyList(), val guardados: List<BonoGuardado> = emptyList())

@Serializable
data class NegocioDelBono(
    val id: String,
    val nombre: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    val ciudad: String? = null,
)

@Serializable
data class MovimientoBono(
    val negocio: String? = null,
    val monto: Double? = null,
    val numero: String? = null,
    val fecha: String? = null,
)

@Serializable
private data class ExtrasBono(
    val negocios: List<NegocioDelBono> = emptyList(),
    val excluidos: List<String> = emptyList(),
    val movimientos: List<MovimientoBono> = emptyList(),
)

/** `GET /api/app/bonos/{id}`: el voucher más dónde se usa y sus movimientos.
 * `negocios` llega vacío con alcance TODOS: ahí va el directorio sin los
 * `excluidos`. */
data class BonoDetalle(
    val bono: Bono,
    val negocios: List<NegocioDelBono>,
    val excluidos: List<String>,
    val movimientos: List<MovimientoBono>,
) {
    companion object {
        fun desde(obj: JsonObject): BonoDetalle {
            val extras = ApiClient.json.decodeFromJsonElement<ExtrasBono>(obj)
            return BonoDetalle(
                bono = ApiClient.json.decodeFromJsonElement<Bono>(obj),
                negocios = extras.negocios, excluidos = extras.excluidos, movimientos = extras.movimientos,
            )
        }
    }
}

/** `POST /api/app/bonos/{id}/qr`: mismas reglas que el QR de identidad (un
 * código vivo por persona, 90 s), pero lleva el bono adentro. */
@Serializable
data class QRBono(
    val token: String,
    @SerialName("expira_at") val expiraAt: String,
    @SerialName("ttl_segundos") val ttlSegundos: Int,
)

/** `GET /api/app/bonos/{id}/estado`, que se consulta cada 2 s con el QR abierto. */
@Serializable
data class EstadoBono(
    val estado: String,
    val saldo: Double? = null,
    val reclamo: ReclamoBono? = null,
)

/** El último reclamo del bono en una caja. RESERVADA = el cajero lo acaba de
 * escanear; CONFIRMADA = se cobró; LIBERADA/ANULADA = no se aplicó. */
@Serializable
data class ReclamoBono(
    val id: String,
    val estado: String,
    val negocio: String? = null,
    val monto: Double? = null,
    val numero: String? = null,
) {
    val confirmado: Boolean get() = estado == "CONFIRMADA"
    val noSeAplico: Boolean get() = estado == "LIBERADA" || estado == "ANULADA"
}
