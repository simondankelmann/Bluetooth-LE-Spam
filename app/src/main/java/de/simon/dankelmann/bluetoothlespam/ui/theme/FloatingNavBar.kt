package de.simon.dankelmann.bluetoothlespam.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import de.simon.dankelmann.bluetoothlespam.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

data class FloatingNavDestination(
    val destinationId: Int,
    val iconRes: Int,
    val labelRes: Int,
)

val floatingNavDestinations = listOf(
    FloatingNavDestination(R.id.nav_start, R.drawable.ic_info, R.string.bottom_nav_start),
    FloatingNavDestination(R.id.nav_advertisement_collection, R.drawable.bluetooth_searching, R.string.bottom_nav_advertise),
    FloatingNavDestination(R.id.nav_spam_detector, R.drawable.ic_location_searching, R.string.bottom_nav_detect),
    FloatingNavDestination(R.id.nav_preferences, R.drawable.settings, R.string.menu_preferences),
)

/**
 * MD3 "floating/docked" navigation bar (plan §3) — a pill island inset from the screen edges,
 * not edge-to-edge. Syncs selection to [navController] manually since this NavController is
 * hosted by the classic XML NavHostFragment, not a navigation-compose NavHost (plan §6 nav
 * interop finding — navigation-compose can't host raw Fragments as destinations).
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
    val currentDestinationId = navBackStackEntry?.destination?.id

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
                    val selected = currentDestinationId == destination.destinationId
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                // Named `resId =` forces resolution to the classic @IdRes
                                // navigate() overload — a bare Int argument is ambiguous with
                                // navigation 2.9's generic navigate(route: Any) and resolves to
                                // the wrong one at runtime (IllegalArgumentException: "Destination
                                // with route Int cannot be found"), caught via on-device testing.
                                navController.navigate(
                                    resId = destination.destinationId,
                                    args = null,
                                    navOptions = androidx.navigation.navOptions {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    },
                                )
                            }
                        },
                        icon = {
                            androidx.compose.material3.Icon(
                                painter = painterResource(destination.iconRes),
                                contentDescription = androidx.compose.ui.res.stringResource(destination.labelRes),
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
