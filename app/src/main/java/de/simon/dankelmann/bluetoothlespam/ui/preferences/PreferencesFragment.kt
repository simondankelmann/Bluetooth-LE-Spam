package de.simon.dankelmann.bluetoothlespam.ui.preferences


import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.preference.PreferenceFragmentCompat
import de.simon.dankelmann.bluetoothlespam.R


class PreferencesFragment : PreferenceFragmentCompat(), MenuProvider {

    private val _logTag = "PreferencesFragment"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        //menuInflater.inflate(R.menu.main, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)

        val menuItems = listOf<MenuItem?>(
            menu.findItem(R.id.nav_preferences),
            menu.findItem(R.id.nav_set_tx_power)
        )

        val context = requireContext()

        menuItems.forEach { menuItem ->
            val actionSettingsMenuItem = menuItem
            val title = actionSettingsMenuItem?.title.toString()
            val spannable = SpannableString(title)

            var textColor = resources.getColor(R.color.text_color, context.theme)

            if (menuItem?.itemId == R.id.nav_preferences) {
                textColor = resources.getColor(R.color.text_color_light, context.theme)
                menuItem.isEnabled = false
            }

            spannable.setSpan(
                ForegroundColorSpan(textColor),
                0,
                spannable.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            actionSettingsMenuItem?.title = spannable
        }
    }
}