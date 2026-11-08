package app.bijao.clientes.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Espejo de `backend/routers/app_creditos.py`, `app_creditos_avisos.py` y
 * `app_creditos_preferencias.py` -- "Mis créditos" (F7). Solo lectura salvo
 * "Ya pagué" y "Cómo te avisamos": el saldo únicamente lo cambia el ADMIN del
 * negocio al confirmar un abono. Si la cuenta no tiene el celular verificado
 * o no coincide con la ficha, el servidor manda todo vacío (nunca un 403)
 * para no confirmar que existe una deuda a nombre de ese número. Mismo shape
 * que `Credito.swift` en bijao-clientes-ios.
 */
@Serializable
data class NegocioCredito(val id: String, val nombre: String? = null)

@Serializable
data class CreditoApp(
    val id: String,
    val negocio: NegocioCredito,
    @SerialName("numero_factura") val numeroFactura: String? = null,
    @SerialName("monto_total") val montoTotal: Double,
    @SerialName("monto_interes") val montoInteres: Double,
    @SerialName("monto_pagado") val montoPagado: Double,
    val saldo: Double,
    @SerialName("fecha_limite") val fechaLimite: String? = null,
    /** Negativo = ya venció hace tantos días. Null si no hay fecha o ya está pagado. */
    @SerialName("dias_para_vencer") val diasParaVencer: Int? = null,
    val estado: String,
    val creado: String? = null,
)

@Serializable
data class ResumenCreditos(
    @SerialName("saldo_total") val saldoTotal: Double,
    val activos: Int,
    val vencidos: Int,
)

@Serializable
data class ListaCreditos(
    @SerialName("celular_verificado") val celularVerificado: Boolean,
    val data: List<CreditoApp>,
    val resumen: ResumenCreditos,
)

@Serializable
data class AbonoCreditoApp(
    val monto: Double,
    @SerialName("metodo_pago") val metodoPago: String? = null,
    val fecha: String? = null,
)

/** Mismos datos de pago que ya publica el portal (Bre-B/Nequi/Bancolombia/QR),
 * con la "doble llave": el negocio activó Pagos y cargó al menos un dato. */
@Serializable
data class BancolombiaDatos(val numero: String, val tipo: String? = null)

@Serializable
data class MetodosPago(
    val breb: String? = null,
    val nequi: String? = null,
    val bancolombia: BancolombiaDatos? = null,
)

@Serializable
data class PagosNegocio(
    val titular: String? = null,
    val metodos: MetodosPago,
    @SerialName("qr_url") val qrUrl: String? = null,
    val instrucciones: String? = null,
)

@Serializable
data class ContactoWhatsapp(val indicativo: String, val numero: String)

@Serializable
data class DetalleCreditoApp(
    val id: String,
    val negocio: NegocioCredito,
    @SerialName("numero_factura") val numeroFactura: String? = null,
    @SerialName("monto_total") val montoTotal: Double,
    @SerialName("monto_interes") val montoInteres: Double,
    @SerialName("monto_pagado") val montoPagado: Double,
    val saldo: Double,
    @SerialName("fecha_limite") val fechaLimite: String? = null,
    @SerialName("dias_para_vencer") val diasParaVencer: Int? = null,
    val estado: String,
    val creado: String? = null,
    val abonos: List<AbonoCreditoApp>,
    @SerialName("como_pagar") val comoPagar: PagosNegocio? = null,
    @SerialName("contacto_whatsapp") val contactoWhatsapp: ContactoWhatsapp? = null,
)

@Serializable
data class DetalleCreditoResponse(val data: DetalleCreditoApp)

// ── "Ya pagué" (F5/F7) ───────────────────────────────────────────────────────

enum class MedioAviso(val valor: String, val texto: String) {
    NEQUI("NEQUI", "Nequi"),
    BREB("BREB", "Bre-B"),
    BANCOLOMBIA("BANCOLOMBIA", "Bancolombia"),
    EFECTIVO("EFECTIVO", "Efectivo"),
    OTRO("OTRO", "Otro"),
}

@Serializable
data class AvisoPagoApp(
    val id: String,
    val monto: Double,
    val medio: String,
    val nota: String? = null,
    val estado: String,
    @SerialName("motivo_rechazo") val motivoRechazo: String? = null,
    val creado: String? = null,
)

@Serializable
data class AvisosPagoResponse(val data: List<AvisoPagoApp>)

@Serializable
data class AvisoPagoResponse(val data: AvisoPagoApp)

@Serializable
data class AvisoPagoInput(
    @SerialName("credito_id") val creditoId: String,
    val monto: Double,
    val medio: String,
    val nota: String? = null,
)

// ── "Cómo te avisamos" (preferencias de cobro) ──────────────────────────────

@Serializable
data class PreferenciaCobro(
    val negocio: NegocioCredito,
    val canales: List<String>,
    /** Quién pidió no contactar: la propia persona (puede quitarlo) o el negocio (no). */
    @SerialName("no_contactar") val noContactar: String? = null,
)

@Serializable
data class PreferenciasCobroResponse(
    @SerialName("canales_con_envio") val canalesConEnvio: List<String>,
    val data: List<PreferenciaCobro>,
)

@Serializable
data class PreferenciaCobroResponse(val data: PreferenciaCobro)

@Serializable
data class PreferenciaCobroInput(
    val canales: List<String>,
    @SerialName("no_contactar") val noContactar: Boolean,
)
