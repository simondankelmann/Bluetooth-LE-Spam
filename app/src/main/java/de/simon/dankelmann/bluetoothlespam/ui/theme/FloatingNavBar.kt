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

// On-screen footprint of SpecterTopAppBar now that content scrolls full-bleed behind it too,
// the same way FloatingNavBarClearance already accounts for the bottom pill. 100.dp (a guess at
// 64dp TopAppBar + a "typical" status bar) measured ~28dp short on a real device -- the actual
// bar (its 64dp content row plus the real status bar inset) came out to ~144dp -- so this is
// that measured value plus a small buffer rather than another guess.
val SpecterTopAppBarClearance = 150.dp

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
    // Same shape as each item's own pill (RoundedCornerShape(50) is percent-based -- a full
    // stadium curve derived from the bar's own height, not an unrelated fixed dp value).
    val pillShape = RoundedCornerShape(50)
    // Equal padding on every side between the bar's edge and the items -- see the Row below for
    // why this also drives the outer/inner corner-radius relationship.
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
            // Never fully transparent: blur alone can still blend into a similarly-toned
            // background, so a tonal tint is always layered on top of it (lighter when blur is
            // doing most of the separation work, stronger when there's no blur to help).
            // surfaceContainerHighest (not surfaceContainer) + a strong border: on an
            // AMOLED/pure-black background even the lightest container tone at moderate alpha
            // still read as barely-there, so this leans further into both the fill opacity and
            // a full-contrast `outline` (not the deliberately-subtle `outlineVariant`) ring.
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                alpha = if (blurEnabled && !hazeRenderFailed) 0.85f else 1f,
            ),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
            modifier = blurModifier,
        ) {
            Row(
                // No fillMaxWidth: the bar shrink-wraps to exactly the 4 items' content width
                // (then MainActivity centers it) instead of stretching to the screen width and
                // pushing all the leftover space into the gaps -- SpaceEvenly put it at the
                // ends, SpaceBetween put it between items, either way stretching this Row is
                // what created the "spaced out" look. A small fixed gap keeps items snug.
                //
                // Padding is equal on every side (not separate horizontal/vertical values) so
                // the ring around the items reads as one consistent thickness -- and because
                // both this bar's shape and each pill's own shape are the same percent-based
                // RoundedCornerShape(50) (radius = height / 2), that equal padding also makes
                // the outer corner radius come out to exactly the pill's own radius plus this
                // padding: outerHeight = pillHeight + 2 * navBarInnerPadding, so
                // outerRadius = outerHeight / 2 = pillRadius + navBarInnerPadding automatically.
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

    // No animateContentSize here: AnimatedVisibility below already animates the label's width
    // every frame, so the Surface naturally tracks that same measured size on its own. Layering
    // animateContentSize on top raced its own interpolation against AnimatedVisibility's,
    // which is what caused the pill to visibly cut off right as a tap started the animation.
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
                        // Bounds the pill's max width regardless of label length/locale/font
                        // scale -- without this the pill (and the Row of all 4 items) can grow
                        // wider than the outer bar and get hard-clipped by its .clip(pillShape)
                        // instead of ellipsizing gracefully.
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 96.dp),
                    )
                }
            }
        }
    }
}
