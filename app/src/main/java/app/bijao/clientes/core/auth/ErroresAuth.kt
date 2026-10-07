package app.bijao.clientes.core.auth

/**
 * Traduce los errores de Supabase Auth a algo que una persona pueda entender
 * y, cuando toca, accionar. Puerto directo de `ErroresAuth.swift` en
 * bijao-clientes-ios (que a su vez porta
 * `frontend/app/(app-clientes)/_componentes/erroresAuth.ts` del monorepo) --
 * mismas reglas, mismos mensajes, para que la experiencia de error sea igual
 * en las tres plataformas. Mantener los tres en sync si cambia uno.
 */
data class ErrorTraducido(
    val mensaje: String,
    /** Segundos que conviene esperar antes de reintentar, si aplica. */
    val esperar: Int? = null,
)

private class Regla(val patron: Regex, val resolver: (MatchResult) -> ErrorTraducido)

private val REGLAS: List<Regla> = listOf(
    // El servicio de correo interno de Supabase está muy limitado a propósito:
    // es para pruebas. Sin SMTP propio, unos pocos envíos por hora lo agotan.
    Regla(Regex("email rate limit exceeded|over_email_send_rate_limit", RegexOption.IGNORE_CASE)) {
        ErrorTraducido(
            "Pediste varios enlaces seguidos. Espera unos minutos antes de intentarlo otra vez, o revisa si ya te llegó alguno.",
            60,
        )
    },
    Regla(Regex("for security purposes.*?(\\d+) seconds", RegexOption.IGNORE_CASE)) { match ->
        val segundos = match.groupValues.getOrNull(1)?.toIntOrNull() ?: 30
        ErrorTraducido("Espera $segundos segundos antes de pedir otro enlace.", segundos)
    },
    Regla(Regex("rate limit|too many requests", RegexOption.IGNORE_CASE)) {
        ErrorTraducido("Demasiados intentos seguidos. Espera un momento y vuelve a probar.", 60)
    },
    // 500 con `unexpected_failure`: Supabase aceptó la petición pero su SMTP
    // rechazó el envío. Casi siempre son credenciales mal puestas o un
    // remitente sin verificar en el proveedor de correo.
    Regla(Regex("error sending.*email|unexpected_failure|smtp", RegexOption.IGNORE_CASE)) {
        ErrorTraducido("No pudimos enviarte el correo. Es un problema nuestro, no tuyo: prueba entrando con Google mientras lo revisamos.")
    },
    Regla(Regex("invalid.*email|unable to validate email", RegexOption.IGNORE_CASE)) {
        ErrorTraducido("Ese correo no parece válido. Revísalo.")
    },
    Regla(Regex("signups not allowed|signup is disabled", RegexOption.IGNORE_CASE)) {
        ErrorTraducido("El registro está cerrado por ahora. Escríbenos si necesitas entrar.")
    },
    // El consentimiento de OAuth quedó en "Interno": solo admite cuentas de la
    // organización de Google Workspace, y esta app es para el público.
    Regla(Regex("org_internal|access_denied|restricted to users within its organization", RegexOption.IGNORE_CASE)) {
        ErrorTraducido("Google rechazó el acceso. Prueba entrando con tu correo mientras lo revisamos.")
    },
    // El SMS sale por Bird (ver MDs/plan_bird_otp_sms.md). Si el sender pierde
    // la aprobación para el país o se agota el saldo, Supabase devuelve un
    // fallo del hook y el usuario no tiene forma de saber que no es culpa suya.
    Regla(Regex("error sending.*sms|sms.*not.*sent|hook.*sms", RegexOption.IGNORE_CASE)) {
        ErrorTraducido("No pudimos enviarte el mensaje. Es un problema nuestro: prueba entrando con tu correo mientras lo revisamos.")
    },
    Regla(Regex("invalid.*phone|phone.*invalid|invalid format", RegexOption.IGNORE_CASE)) {
        ErrorTraducido("Ese número no parece válido. Escríbelo sin espacios, con los 10 dígitos.")
    },
    Regla(Regex("phone.*not.*confirmed|otp.*expired|token has expired|invalid.*otp|token.*invalid", RegexOption.IGNORE_CASE)) {
        ErrorTraducido("Ese código no es válido o ya venció. Pide uno nuevo.")
    },
    Regla(Regex("failed to fetch|network|load failed|could not connect", RegexOption.IGNORE_CASE)) {
        ErrorTraducido("No hay conexión. Revisa tu internet e inténtalo otra vez.")
    },
)

fun traducirErrorAuth(error: Throwable): ErrorTraducido {
    val bruto = error.message ?: error.toString()
    for (regla in REGLAS) {
        val match = regla.patron.find(bruto) ?: continue
        return regla.resolver(match)
    }
    return ErrorTraducido("No pudimos completar la operación. Inténtalo de nuevo en un momento.")
}
