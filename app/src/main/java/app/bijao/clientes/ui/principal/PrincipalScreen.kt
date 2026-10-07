package app.bijao.clientes.ui.principal

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Explore
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
import app.bijao.clientes.ui.beneficios.CodigoQRScreen
import app.bijao.clientes.ui.beneficios.ValesScreen
import app.bijao.clientes.ui.explorar.ExplorarScreen
import app.bijao.clientes.ui.explorar.NegocioDetalleScreen

private data class Tab(val ruta: String, val etiqueta: String, val icono: androidx.compose.ui.graphics.vector.ImageVector)

private val TABS = listOf(
    Tab("explorar", "Explorar", Icons.Filled.Explore),
    Tab("beneficios", "Beneficios", Icons.Filled.CardGiftcard),
)

/**
 * Sombrilla de navegación post-login -- equivalente de `MainTabView` en iOS,
 * aunque todavía con solo dos pestañas (Explorar, Beneficios); el resto
 * (Favoritos, Perfil, Inicio) se suma a medida que se cierran sus fases.
 */
@Composable
fun PrincipalScreen() {
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
            startDestination = "explorar",
            modifier = androidx.compose.ui.Modifier.padding(padding),
        ) {
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
                BeneficiosScreen(
                    onVerVales = { navController.navigate("vales") },
                    onMostrarQR = { navController.navigate("qr") },
                )
            }
            composable("vales") {
                ValesScreen(onAtras = { navController.popBackStack() })
            }
            composable("qr") {
                CodigoQRScreen(onAtras = { navController.popBackStack() })
            }
        }
    }
}
