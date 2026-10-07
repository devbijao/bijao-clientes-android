package app.bijao.clientes.ui.principal

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.bijao.clientes.ui.explorar.ExplorarScreen
import app.bijao.clientes.ui.explorar.NegocioDetalleScreen

/**
 * Sombrilla de navegación post-login -- por ahora solo Explorar (Fase A3) y
 * su ficha de negocio. Sin bottom nav todavía: con una sola sección no hay
 * nada que pestañear; eso llega naturalmente cuando existan más (Beneficios,
 * Favoritos, Perfil, Inicio), como en el `MainTabView` de iOS.
 */
@Composable
fun PrincipalScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "explorar") {
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
    }
}
