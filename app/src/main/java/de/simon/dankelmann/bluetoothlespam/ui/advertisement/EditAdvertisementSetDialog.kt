package de.simon.dankelmann.bluetoothlespam.ui.advertisement

import android.content.Context
import android.text.InputType
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.R
import java.util.UUID

/**
 * Generic editor for any advertisement set (all categories):
 * title, manufacturer payloads (id + hex), service payloads (uuid + hex)
 * and the include-device-name / include-tx-power flags.
 */
object EditAdvertisementSetDialog {

    data class EditedValues(
        val title: String,
        val includeDeviceName: Boolean,
        val includeTxPower: Boolean,
        val manufacturerIds: List<Int>,
        val manufacturerHex: List<String>,
        val serviceUuids: List<String>,
        val serviceHex: List<String?>
    )

    fun isValidHex(raw: String): Boolean {
        val hex = raw.trim().replace(" ", "")
        if (hex.length % 2 != 0) return false
        return hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    fun show(context: Context, set: AdvertisementSet, onSave: (EditedValues) -> Unit) {
        val padding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 16f, context.resources.displayMetrics
        ).toInt()
        val smallGap = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 8f, context.resources.displayMetrics
        ).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, smallGap, padding, smallGap)
        }

        fun addHeader(text: String) {
            container.addView(TextView(context).apply {
                this.text = text
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, smallGap, 0, smallGap / 2)
            })
        }

        fun addInput(hint: String, initial: String, inputType: Int = InputType.TYPE_CLASS_TEXT): EditText {
            return EditText(context).apply {
                this.hint = hint
                setText(initial)
                this.inputType = inputType
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = smallGap / 2 }
                container.addView(this)
            }
        }

        addHeader(context.getString(R.string.device_field_title))
        val titleInput = addInput(context.getString(R.string.device_field_title), set.title)

        val manufacturerIdInputs = mutableListOf<EditText>()
        val manufacturerHexInputs = mutableListOf<EditText>()
        if (set.advertiseData.manufacturerData.isNotEmpty()) {
            addHeader(context.getString(R.string.device_section_manufacturer))
            set.advertiseData.manufacturerData.forEachIndexed { index, entry ->
                addInput(
                    context.getString(R.string.device_field_manufacturer_id, index + 1),
                    String.format("%04X", entry.manufacturerId)
                ).also { manufacturerIdInputs.add(it) }
                addInput(
                    context.getString(R.string.device_field_manufacturer_data, index + 1),
                    entry.manufacturerSpecificData.joinToString("") { "%02x".format(it) }
                ).also { manufacturerHexInputs.add(it) }
            }
        }

        val serviceUuidInputs = mutableListOf<EditText>()
        val serviceHexInputs = mutableListOf<EditText>()
        if (set.advertiseData.services.isNotEmpty()) {
            addHeader(context.getString(R.string.device_section_service))
            set.advertiseData.services.forEachIndexed { index, entry ->
                addInput(
                    context.getString(R.string.device_field_service_uuid, index + 1),
                    entry.serviceUuid?.toString() ?: ""
                ).also { serviceUuidInputs.add(it) }
                val hex = entry.serviceData?.joinToString("") { "%02x".format(it) } ?: ""
                addInput(
                    context.getString(R.string.device_field_service_data, index + 1),
                    hex
                ).also { serviceHexInputs.add(it) }
            }
        }

        addHeader(context.getString(R.string.device_section_options))
        val includeDeviceNameBox = CheckBox(context).apply {
            text = context.getString(R.string.device_field_include_name)
            isChecked = set.advertiseData.includeDeviceName
            container.addView(this)
        }
        val includeTxPowerBox = CheckBox(context).apply {
            text = context.getString(R.string.device_field_include_tx)
            isChecked = set.advertiseData.includeTxPower
            container.addView(this)
        }

        val scrollView = ScrollView(context).apply { addView(container) }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(set.title)
            .setView(scrollView)
            .setPositiveButton(context.getString(R.string.device_save), null)
            .setNegativeButton(context.getString(android.R.string.cancel), null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = titleInput.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(context, context.getString(R.string.device_error_empty_title), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val mfrIds = mutableListOf<Int>()
                for (input in manufacturerIdInputs) {
                    val raw = input.text.toString().trim().replace(" ", "").removePrefix("0x")
                    if (!isValidHex(raw) || raw.length > 4) {
                        Toast.makeText(context, context.getString(R.string.device_error_bad_manufacturer_id), Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    mfrIds.add(raw.toInt(16))
                }

                val mfrHex = mutableListOf<String>()
                for (input in manufacturerHexInputs) {
                    val raw = input.text.toString().trim().replace(" ", "")
                    if (!isValidHex(raw)) {
                        Toast.makeText(context, context.getString(R.string.device_error_bad_hex), Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    mfrHex.add(raw.lowercase())
                }

                val srvUuids = mutableListOf<String>()
                for (input in serviceUuidInputs) {
                    val raw = input.text.toString().trim()
                    try {
                        UUID.fromString(raw)
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.device_error_bad_uuid), Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    srvUuids.add(raw.lowercase())
                }

                val srvHex = mutableListOf<String?>()
                for (input in serviceHexInputs) {
                    val raw = input.text.toString().trim().replace(" ", "")
                    if (raw.isEmpty()) {
                        srvHex.add(null)
                    } else {
                        if (!isValidHex(raw)) {
                            Toast.makeText(context, context.getString(R.string.device_error_bad_hex), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        srvHex.add(raw.lowercase())
                    }
                }

                onSave(
                    EditedValues(
                        title = title,
                        includeDeviceName = includeDeviceNameBox.isChecked,
                        includeTxPower = includeTxPowerBox.isChecked,
                        manufacturerIds = mfrIds,
                        manufacturerHex = mfrHex,
                        serviceUuids = srvUuids,
                        serviceHex = srvHex
                    )
                )
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}
