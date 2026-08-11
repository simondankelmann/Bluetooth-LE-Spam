package de.simon.dankelmann.bluetoothlespam.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import de.simon.dankelmann.bluetoothlespam.Navigation.SpecterDestinations
import de.simon.dankelmann.bluetoothlespam.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

// Approximate on-screen footprint of the pill (NavigationBar's own 80dp + this Box's top/bottom
// padding); screens with a FloatingActionButton need this much bottom clearance so the FAB
// doesn't render underneath the pill, which floats on top of content rather than reserving space.
val FloatingNavBarClearance = 100.dp

data class FloatingNavDestination(
    val route: String,
    val iconRes: Int,
    val labelRes: Int,
)

val floatingNavDestinations = listOf(
    FloatingNavDestination(SpecterDestinations.START, R.drawable.ic_info, R.string.bottom_nav_start),
    FloatingNavDestination(SpecterDestinations.ADVERTISEMENT_COLLECTION, R.drawable.bluetooth_searching, R.string.bottom_nav_advertise),
    FloatingNavDestination(SpecterDestinations.SPAM_DETECTOR, R.drawable.ic_location_searching, R.string.bottom_nav_detect),
    FloatingNavDestination(SpecterDestinations.PREFERENCES, R.drawable.settings, R.string.menu_preferences),
)

/**
 * MD3 "floating/docked" navigation bar (plan §3) — a pill island inset from the screen edges,
 * not edge-to-edge. Syncs selection to [navController]'s current route (plan §6 step 4 — real
 * navigation-compose `NavHost`, not the earlier classic-Fragment interop this comment used to
 * describe).
 *
 * Blur backdrop (haze) is the ONLY blur surface in the app (plan §4) and is user-toggleable;
 * falls back to a translucent tonal surface if `haze` rendering fails (e.g. unaccelerated GPU,
 * plan Failure Modes).
 */
@Composable
fun FloatingNavBar(
    navController: NavController,
    hazeState: HazeState,
    blurEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val pillShape = RoundedCornerShape(28.dp)
    val hazeStyle = HazeMaterials.thin()
    var hazeRenderFailed = false
    val blurModifier = if (blurEnabled && !hazeRenderFailed) {
        try {
            Modifier
                .clip(pillShape)
                .hazeEffect(state = hazeState) { style = hazeStyle }
        } catch (e: Exception) {
            // Best-effort: catches synchronous failures during modifier/style construction.
            // GPU-level render failures on unaccelerated hardware happen later in the draw
            // phase and aren't guaranteed to be caught here.
            hazeRenderFailed = true
            Modifier.clip(pillShape)
        }
    } else {
        Modifier.clip(pillShape)
    }

    Box(
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
    ) {
        Surface(
            shape = pillShape,
            tonalElevation = 3.dp,
            color = if (blurEnabled && !hazeRenderFailed) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
            },
            modifier = blurModifier,
        ) {
            NavigationBar(containerColor = Color.Transparent) {
                floatingNavDestinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            androidx.compose.material3.Icon(
                                painter = painterResource(destination.iconRes),
                                contentDescription = androidx.compose.ui.res.stringResource(destination.labelRes),
                                modifier = Modifier.size(28.dp),
                            )
                        },
                        label = { Text(androidx.compose.ui.res.stringResource(destination.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    )
                }
            }
        }
    }
}
