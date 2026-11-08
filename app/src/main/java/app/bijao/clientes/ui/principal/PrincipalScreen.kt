package app.bijao.clientes.ui.principal

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.bijao.clientes.ui.beneficios.BeneficiosScreen
import app.bijao.clientes.ui.beneficios.ValesScreen
import app.bijao.clientes.core.models.Perfil
import app.bijao.clientes.ui.contactos.ContactosScreen
import app.bijao.clientes.ui.creditos.CreditoDetalleScreen
import app.bijao.clientes.ui.creditos.CreditosScreen
import app.bijao.clientes.ui.cuentas.DividirScreen
import app.bijao.clientes.ui.cuentas.MisCuentasScreen
import app.bijao.clientes.ui.explorar.ExplorarScreen
import app.bijao.clientes.ui.explorar.NegocioDetalleScreen
import app.bijao.clientes.ui.favoritos.FavoritosScreen
import app.bijao.clientes.ui.historial.CompraDetalleScreen
import app.bijao.clientes.ui.historial.HistorialScreen
import app.bijao.clientes.ui.inicio.InicioScreen
import app.bijao.clientes.ui.inicio.NotificacionesScreen
import app.bijao.clientes.ui.perfil.PerfilScreen
import app.bijao.clientes.ui.wallet.WalletScreen

private data class Tab(val ruta: String, val etiqueta: String, val icono: androidx.compose.ui.graphics.vector.ImageVector)

/** Mismo gris de borde que `--border` en la web (`globals.css`). */
private val GrisBordeTabs = Color(0xFFE2E8F0)

private val TABS = listOf(
    Tab("inicio", "Inicio", Icons.Filled.Home),
    Tab("explorar", "Explorar", Icons.Filled.Explore),
    Tab("beneficios", "Beneficios", Icons.Filled.CardGiftcard),
    Tab("favoritos", "Favoritos", Icons.Filled.Favorite),
    Tab("perfil", "Perfil", Icons.Filled.Person),
)

/**
 * Sombrilla de navegación post-login -- equivalente de `MainTabView` en iOS:
 * mismas cinco pestañas (Inicio, Explorar, Beneficios, Favoritos, Perfil).
 */
@Composable
fun PrincipalScreen(
    perfil: Perfil,
    negociosVinculadosAlRegistrarse: Int = 0,
    onToastVinculadosMostrado: () -> Unit = {},
    onPerfilActualizado: (Perfil) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Plana a propósito (sin la pastilla de Material detrás del ícono
            // seleccionado ni elevación tonal): mismo lenguaje que la barra
            // inferior de la web (`ShellClientes.tsx`) y los tabs de iOS --
            // ícono+label en azul si está activo, gris si no.
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = GrisBordeTabs,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                },
            ) {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = rutaActual == tab.ruta,
                        onClick = {
                            navController.navigate(tab.ruta) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icono, contentDescription = tab.etiqueta) },
                        label = { Text(tab.etiqueta, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "inicio",
            modifier = androidx.compose.ui.Modifier.padding(padding),
        ) {
            composable("inicio") {
                InicioScreen(
                    perfil = perfil,
                    negociosVinculadosAlRegistrarse = negociosVinculadosAlRegistrarse,
                    onToastVinculadosMostrado = onToastVinculadosMostrado,
                    onAbrirNotificaciones = { navController.navigate("notificaciones") },
                    onDividirCuenta = { navController.navigate("dividir") },
                    onVerWallet = { navController.navigate("wallet") },
                )
            }
            composable("notificaciones") {
                NotificacionesScreen(
                    onCerrar = { navController.popBackStack() },
                    onCompraClick = { id -> navController.navigate("compra/$id") },
                    onContactosClick = { navController.navigate("contactos") },
                    onCreditoClick = { id -> navController.navigate("credito/$id") },
                )
            }
            composable("contactos") {
                ContactosScreen(onAtras = { navController.popBackStack() })
            }
            composable("creditos") {
                CreditosScreen(
                    onAtras = { navController.popBackStack() },
                    onCreditoClick = { id -> navController.navigate("credito/$id") },
                )
            }
            composable(
                "credito/{creditoId}",
                arguments = listOf(navArgument("creditoId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val creditoId = backStackEntry.arguments?.getString("creditoId") ?: return@composable
                CreditoDetalleScreen(creditoId = creditoId, onAtras = { navController.popBackStack() })
            }
            composable("dividir") {
                DividirScreen(
                    onAtras = { navController.popBackStack() },
                    onVerMisCuentas = { navController.navigate("cuentas") },
                )
            }
            composable("cuentas") {
                MisCuentasScreen(onAtras = { navController.popBackStack() })
            }
            composable("wallet") {
                WalletScreen(onAtras = { navController.popBackStack() })
            }
            composable("explorar") {
                ExplorarScreen(onNegocioClick = { id -> navController.navigate("negocio/$id") })
            }
            composable(
                "negocio/{negocioId}",
                arguments = listOf(navArgument("negocioId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val negocioId = backStackEntry.arguments?.getString("negocioId") ?: return@composable
                NegocioDetalleScreen(negocioId = negocioId, onAtras = { navController.popBackStack() })
            }
            composable("beneficios") {
                BeneficiosScreen(onVerVales = { navController.navigate("vales") })
            }
            composable("vales") {
                ValesScreen(onAtras = { navController.popBackStack() })
            }
            composable("favoritos") {
                FavoritosScreen(onNegocioClick = { id -> navController.navigate("negocio/$id") })
            }
            composable("perfil") {
                PerfilScreen(
                    perfilInicial = perfil,
                    onPerfilActualizado = onPerfilActualizado,
                    onVerHistorial = { navController.navigate("historial") },
                    onVerWallet = { navController.navigate("wallet") },
                    onVerContactos = { navController.navigate("contactos") },
                    onVerCreditos = { navController.navigate("creditos") },
                )
            }
            composable("historial") {
                HistorialScreen(
                    onAtras = { navController.popBackStack() },
                    onCompraClick = { id -> navController.navigate("compra/$id") },
                )
            }
            composable(
                "compra/{compraId}",
                arguments = listOf(navArgument("compraId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val compraId = backStackEntry.arguments?.getString("compraId") ?: return@composable
                CompraDetalleScreen(compraId = compraId, onAtras = { navController.popBackStack() })
            }
        }
    }
}
