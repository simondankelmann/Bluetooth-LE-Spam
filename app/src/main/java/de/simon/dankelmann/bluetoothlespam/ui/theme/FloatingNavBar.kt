package de.simon.dankelmann.bluetoothlespam.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.simon.dankelmann.bluetoothlespam.Navigation.SpecterDestinations
import de.simon.dankelmann.bluetoothlespam.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

// Approximate on-screen footprint of the pill (NavigationBar's own 80dp + this Box's top/bottom
// padding); screens with a FloatingActionButton need this much bottom clearance so the FAB
// doesn't render underneath the pill, which floats on top of content rather than reserving space.
val FloatingNavBarClearance = 100.dp

// Exact height of SpecterTopAppBar (status bar inset + 64dp TopAppBar row), which floats over
// content, so screens start right under it.
val SpecterTopAppBarClearance: Dp
    @Composable get() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp

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
 * not edge-to-edge. Selection is driven by [selectedIndex] (the top-level tabs' HorizontalPager
 * page, hosted in MainActivity) rather than a NavController route, so tapping a tab and swiping
 * the pager land on the exact same selection state.
 *
 * Blur backdrop (haze) is the ONLY blur surface in the app (plan §4) and is user-toggleable;
 * falls back to a translucent tonal surface if `haze` rendering fails (e.g. unaccelerated GPU,
 * plan Failure Modes).
 */
@Composable
fun FloatingNavBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    hazeState: HazeState,
    blurEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    // Same stadium shape as each item; equal inner padding keeps outer radius = item radius + padding.
    val pillShape = RoundedCornerShape(50)
    val navBarInnerPadding = 6.dp
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
            .padding(start = 16.dp, end = 16.dp, top = 10.dp)
            .navigationBarsPadding()
            .padding(bottom = 6.dp),
    ) {
        Surface(
            shape = pillShape,
            tonalElevation = 3.dp,
            // Always tinted + outlined so the pill stays visible even on AMOLED black.
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                alpha = if (blurEnabled && !hazeRenderFailed) 0.85f else 1f,
            ),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
            modifier = blurModifier,
        ) {
            Row(
                // Shrink-wraps to the items (no fillMaxWidth) so they stay snug; MainActivity centers it.
                modifier = Modifier.padding(navBarInnerPadding),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                floatingNavDestinations.forEachIndexed { index, destination ->
                    val selected = index == selectedIndex
                    FloatingNavItem(
                        destination = destination,
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                onTabSelected(index)
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * A single floating-nav destination. Unselected items are icon-only circles; the selected item
 * expands into a pill with its label popping out next to the icon (per design sketch).
 */
@Composable
private fun FloatingNavItem(
    destination: FloatingNavDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        label = "floatingNavItemContainerColor",
    )
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = stringResource(destination.labelRes)

    // No animateContentSize: it races AnimatedVisibility's width animation and clips the pill.
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(destination.iconRes),
                contentDescription = if (selected) null else label,
                modifier = Modifier.size(24.dp),
            )
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = label,
                        maxLines = 1,
                        // Caps width so long labels/font scales ellipsize instead of clipping.
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 96.dp),
                    )
                }
            }
        }
    }
}
