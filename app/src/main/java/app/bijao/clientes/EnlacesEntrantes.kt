package app.bijao.clientes

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Enlaces web que abren la app (App Links). Hoy solo uno: el QR de un evento
 * de Bijao Comunidad, `https://clientes.bijao.app/comunidad/<slug>`, que la web
 * redirige a `/perfil/comunidad/<slug>` (ambas formas llegan acá).
 *
 * El intent-filter con `autoVerify` está en `AndroidManifest.xml` y el monorepo
 * publica el `.well-known/assetlinks.json` que lo autoriza. Sin la app
 * instalada, el mismo enlace sigue abriendo la web.
 *
 * Si llega sin sesión, el slug queda pendiente: `PrincipalScreen` lo consume en
 * cuanto aparece, es decir, apenas termina el login. Equivalente de
 * `EnlacesEntrantes.swift` en bijao-clientes-ios.
 */
object EnlacesEntrantes {
    private const val HOST = "clientes.bijao.app"

    /** Mismo formato que valida el backend (`_SLUG_OK` y `SLUG_MAX` en
     *  `backend/comunidad_reglas.py`): minúsculas, números y guiones sueltos. */
    private val SLUG_VALIDO = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    private const val SLUG_MAX = 80

    private val _eventoComunidad = MutableStateFlow<String?>(null)

    /** Slug del evento de Comunidad que hay que abrir en Perfil › Comunidad. */
    val eventoComunidad: StateFlow<String?> = _eventoComunidad.asStateFlow()

    fun abrir(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val slug = intent.data?.let(::slugComunidad) ?: return
        _eventoComunidad.value = slug
    }

    /** Lo llama quien ya abrió el evento, para no volver a abrirlo. */
    fun consumirEventoComunidad() {
        _eventoComunidad.value = null
    }

    fun slugComunidad(uri: Uri): String? {
        if (uri.scheme?.lowercase() != "https" || uri.host?.lowercase() != HOST) return null
        val partes = uri.pathSegments.orEmpty()
        val slug = when {
            partes.size == 2 && partes[0] == "comunidad" -> partes[1]
            partes.size == 3 && partes[0] == "perfil" && partes[1] == "comunidad" -> partes[2]
            else -> return null
        }.lowercase()
        return slug.takeIf { it.length <= SLUG_MAX && SLUG_VALIDO.matches(it) }
    }
}
