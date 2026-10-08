package app.bijao.clientes.ui.principal

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import app.bijao.clientes.ui.explorar.ExplorarScreen
import app.bijao.clientes.ui.explorar.NegocioDetalleScreen
import app.bijao.clientes.ui.favoritos.FavoritosScreen
import app.bijao.clientes.ui.historial.CompraDetalleScreen
import app.bijao.clientes.ui.historial.HistorialScreen
import app.bijao.clientes.ui.inicio.InicioScreen
import app.bijao.clientes.ui.inicio.NotificacionesScreen
import app.bijao.clientes.ui.perfil.PerfilScreen

private data class Tab(val ruta: String, val etiqueta: String, val icono: androidx.compose.ui.graphics.vector.ImageVector)

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
            NavigationBar {
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
                        label = { Text(tab.etiqueta) },
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
                )
            }
            composable("notificaciones") {
                NotificacionesScreen(
                    onCerrar = { navController.popBackStack() },
                    onCompraClick = { id -> navController.navigate("compra/$id") },
                )
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
