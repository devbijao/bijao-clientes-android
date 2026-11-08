package app.bijao.clientes.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Espejo de `backend/routers/app_cuentas.py` -- "Dividir la cuenta" (D5). El
 * link público (`GET /cuentas/{codigo}`) se abre en la web, no acá: ver
 * MDs/handoff_apps_nativas_cuentas_wallet.md. Esta app solo crea la cuenta,
 * la comparte y gestiona "Mis cuentas". Mismo shape que `CuentaDividida.swift`.
 */
@Serializable
data class ParteCuentaInput(val nombre: String, val monto: Int, val contacto: String? = null)

@Serializable
data class CuentaInput(
    val total: Int,
    @SerialName("propina_pct") val propinaPct: Int,
    val descripcion: String? = null,
    @SerialName("negocio_id") val negocioId: String? = null,
    val partes: List<ParteCuentaInput>,
)

@Serializable
data class CrearCuentaResponse(val codigo: String, @SerialName("vence_en") val venceEn: String)

@Serializable
data class ParteMiCuenta(
    val id: String,
    val nombre: String,
    val monto: Int,
    val estado: String,
    @SerialName("es_contacto") val esContacto: Boolean = false,
)

@Serializable
data class MiCuenta(
    val codigo: String,
    val descripcion: String? = null,
    val negocio: String? = null,
    @SerialName("total_con_propina") val totalConPropina: Int,
    @SerialName("creada_en") val creadaEn: String,
    @SerialName("vence_en") val venceEn: String,
    val vencida: Boolean,
    val partes: List<ParteMiCuenta>,
)

@Serializable
data class MisCuentasResponse(val data: List<MiCuenta>)

/**
 * Fila de "Compartidas conmigo" (`GET /cuentas-compartidas`, Fase S3) --
 * cuentas donde otra persona me incluyó como contacto. Solo lectura: quien
 * marca los pagos sigue siendo quien la creó.
 */
@Serializable
data class CuentaCompartida(
    val codigo: String,
    val descripcion: String? = null,
    val negocio: String? = null,
    val creador: String? = null,
    @SerialName("total_con_propina") val totalConPropina: Int,
    @SerialName("mi_monto") val miMonto: Int,
    @SerialName("mi_estado") val miEstado: String,
    @SerialName("creada_en") val creadaEn: String,
    @SerialName("vence_en") val venceEn: String,
    val vencida: Boolean,
)

@Serializable
data class CuentasCompartidasResponse(val data: List<CuentaCompartida>)

@Serializable
data class EstadoParteInput(val estado: String)

@Serializable
data class EstadoParteResponse(val estado: String)

/** `{"ok": true}` -- respuesta de los DELETE de cuentas y de movimientos del Wallet. */
@Serializable
data class OkResponse(val ok: Boolean)
