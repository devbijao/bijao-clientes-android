package app.bijao.clientes

import android.app.Application

/**
 * Sin DI framework a propósito (ni Hilt ni Koin) -- mismo criterio que
 * bijao-clientes-ios, que arma sus objetos a mano. `AuthManager` es un
 * `object` (singleton de Kotlin) que no necesita que nada lo inicialice
 * desde acá; esta clase existe para que quede un punto de entrada claro si
 * en algún momento sí hace falta estado a nivel de aplicación.
 */
class BijaoClientesApp : Application()
