package app.bijao.clientes.core

/**
 * Configuración fija de entorno -- producción por ahora, sin flavors de
 * staging todavía (mismo criterio que `AppConfig.swift` en bijao-clientes-ios).
 *
 * El anon key de Supabase es público por diseño (va embebido en cualquier
 * cliente, igual que en la web y en iOS): la seguridad real vive en RLS del
 * lado del servidor, no en ocultar esta clave.
 */
object AppConfig {
    /** Proyecto Supabase AISLADO de Bijao Clientes (no el de negocios) --
     * el MISMO proyecto que usa bijao-clientes-ios. Un JWT de este proyecto
     * no sirve contra el backend de negocios y viceversa. */
    const val SUPABASE_URL = "https://ppwoqmhsjzpxlqpzmdlz.supabase.co"
    const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBwd29xbWhzanpweGxxcHptZGx6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODcyNzkyOTIsImV4cCI6MjEwMjg1NTI5Mn0.dSVV1dFX94Tb8vOtl_N41j_zwqGFv9FKcBTAhwDYlXc"

    /** Backend FastAPI compartido con la web y con iOS -- mismas rutas,
     * todas bajo `/api/app`. */
    const val API_BASE_URL = "https://bijao-u637.onrender.com/"

    /** Colombia: único país soportado hoy en el OTP por SMS. */
    const val INDICATIVO_TELEFONO = "+57"
}
