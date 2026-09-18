package de.simon.dankelmann.bluetoothlespam.ui.customdevices

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Enums.stringResId
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.DeviceCustomizationHelper
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentCustomDevicesBinding
import de.simon.dankelmann.bluetoothlespam.ui.setupEdgeToEdge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Settings screen listing user-customized devices:
 * removed sets and modified sets, each restorable individually,
 * plus a "reset all to defaults" action.
 */
class CustomDevicesFragment : Fragment() {

    private val _logTag = "CustomDevicesFragment"
    private var _binding: FragmentCustomDevicesBinding? = null
    private val binding get() = _binding!!

    private data class RemovedItem(val id: Int, val set: AdvertisementSet, val isDefault: Boolean)
    private data class ModifiedItem(
        val id: Int,
        val set: AdvertisementSet,
        val originalTitle: String,
        val isDefault: Boolean
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomDevicesBinding.inflate(inflater, container, false)
        val root: View = binding.root
        setupEdgeToEdge(binding.root, top = false)

        binding.customDevicesResetButton.setOnClickListener {
            onResetAllClicked()
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun loadData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val defaults = DeviceCustomizationHelper.defaultSetsByKey()

                val removed = mutableListOf<RemovedItem>()
                DeviceCustomizationHelper.getDeletedIds().forEach { id ->
                    val set = try {
                        DatabaseHelpers.getAdvertisementSetById(id)
                    } catch (e: Exception) {
                        null
                    }
                    if (set == null) {
                        DeviceCustomizationHelper.setDeleted(id, false)
                    } else {
                        removed.add(
                            RemovedItem(
                                id,
                                set,
                                DeviceCustomizationHelper.isDefaultSet(set)
                            )
                        )
                    }
                }

                val modified = mutableListOf<ModifiedItem>()
                DeviceCustomizationHelper.getModifiedIds().forEach { id ->
                    val backupTitle = DeviceCustomizationHelper.getBackupTitle(id)
                    val set = try {
                        DatabaseHelpers.getAdvertisementSetById(id)
                    } catch (e: Exception) {
                        null
                    }
                    if (set == null) {
                        DeviceCustomizationHelper.clearBackup(id)
                        DeviceCustomizationHelper.setDeleted(id, false)
                    } else if (backupTitle == null) {
                        DeviceCustomizationHelper.clearBackup(id)
                    } else if (DeviceCustomizationHelper.matchesBackup(set)) {
                        // Already back to original, drop the stale entry
                        DeviceCustomizationHelper.clearBackup(id)
                    } else {
                        val isDefault = defaults.values.any {
                            it.type == set.type && it.title == backupTitle
                        }
                        modified.add(ModifiedItem(id, set, backupTitle, isDefault))
                    }
                }

                removed.sortBy { it.set.title.lowercase() }
                modified.sortBy { it.set.title.lowercase() }

                AppContext.getActivity().runOnUiThread {
                    if (isAdded) renderLists(removed, modified)
                }
            } catch (e: Exception) {
                Log.e(_logTag, "Failed to load custom devices: ${e.message}")
            }
        }
    }

    private fun renderLists(removed: List<RemovedItem>, modified: List<ModifiedItem>) {
        val b = _binding ?: return

        b.customDevicesRemovedList.removeAllViews()
        b.customDevicesRemovedEmpty.visibility = if (removed.isEmpty()) View.VISIBLE else View.GONE
        removed.forEach { item ->
            val typeLabel = try {
                getString(item.set.type.stringResId())
            } catch (e: Exception) {
                item.set.type.name
            }
            val subtitle = if (item.isDefault) typeLabel
            else "$typeLabel · ${getString(R.string.manage_custom_badge)}"
            b.customDevicesRemovedList.addView(
                makeRow(item.set.title, subtitle) {
                    onRestoreRemovedClicked(item.id)
                }
            )
        }

        b.customDevicesModifiedList.removeAllViews()
        b.customDevicesModifiedEmpty.visibility = if (modified.isEmpty()) View.VISIBLE else View.GONE
        modified.forEach { item ->
            val typeLabel = try {
                getString(item.set.type.stringResId())
            } catch (e: Exception) {
                item.set.type.name
            }
            val badge = if (item.isDefault) "" else " · ${getString(R.string.manage_custom_badge)}"
            b.customDevicesModifiedList.addView(
                makeRow(
                    "${item.originalTitle} → ${item.set.title}",
                    "$typeLabel$badge"
                ) {
                    onRestoreModifiedClicked(item.id)
                }
            )
        }
    }

    fun makeRow(title: String, subtitle: String, onRestore: () -> Unit): View {
        val context = requireContext()
        val gap = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 8f, context.resources.displayMetrics
        ).toInt()

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, gap, 0, gap)
        }

        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(context).apply {
                text = title
                setTextColor(
                    resources.getColor(
                        R.color.text_color,
                        AppContext.getContext().theme
                    )
                )
            })
            addView(TextView(context).apply {
                text = subtitle
                setTextColor(
                    resources.getColor(
                        R.color.text_color_light,
                        AppContext.getContext().theme
                    )
                )
                textSize = 12f
            })
        }
        row.addView(texts)

        row.addView(Button(context).apply {
            text = getString(R.string.manage_restore)
            setOnClickListener { onRestore() }
        })

        return row
    }

    fun onRestoreRemovedClicked(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            DeviceCustomizationHelper.setDeleted(id, false)
            AppContext.getActivity().runOnUiThread {
                Toast.makeText(
                    AppContext.getContext(),
                    getString(R.string.manage_restored),
                    Toast.LENGTH_SHORT
                ).show()
                if (isAdded) loadData()
            }
        }
    }

    fun onRestoreModifiedClicked(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            DeviceCustomizationHelper.applyBackup(id)
            DeviceCustomizationHelper.clearBackup(id)
            AppContext.getActivity().runOnUiThread {
                Toast.makeText(
                    AppContext.getContext(),
                    getString(R.string.manage_restored),
                    Toast.LENGTH_SHORT
                ).show()
                if (isAdded) loadData()
            }
        }
    }

    fun onResetAllClicked() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.manage_reset_title))
            .setMessage(getString(R.string.manage_reset_message))
            .setPositiveButton(getString(R.string.manage_reset)) { _, _ ->
                binding.customDevicesResetButton.isEnabled = false
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val result = DeviceCustomizationHelper.resetToDefaults()
                        AppContext.getActivity().runOnUiThread {
                            _binding?.customDevicesResetButton?.isEnabled = true
                            Toast.makeText(
                                AppContext.getContext(),
                                "${getString(R.string.manage_reset_done)} " +
                                    "(+${result.readded}/-${result.removedCustoms}/~${result.restored})",
                                Toast.LENGTH_LONG
                            ).show()
                            if (isAdded) loadData()
                        }
                    } catch (e: Exception) {
                        Log.e(_logTag, "Reset failed: ${e.message}")
                        AppContext.getActivity().runOnUiThread {
                            _binding?.customDevicesResetButton?.isEnabled = true
                            Toast.makeText(AppContext.getContext(), "Reset failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .show()
    }
}
