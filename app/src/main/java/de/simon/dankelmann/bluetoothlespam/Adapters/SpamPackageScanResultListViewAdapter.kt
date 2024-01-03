package de.simon.dankelmann.bluetoothlespam.Adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import de.simon.dankelmann.bluetoothlespam.Enums.FlipperDeviceType
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.Models.SpamPackageScanResult
import de.simon.dankelmann.bluetoothlespam.R

class SpamPackageScanResultListViewAdapter(private val context: Activity, var spamPackages: MutableList<SpamPackageScanResult>) : ArrayAdapter<SpamPackageScanResult>(context, R.layout.list_item_spam_package_scan_result, spamPackages) {
    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.list_item_spam_package_scan_result, null, true)

        val nameText = rowView.findViewById(R.id.list_item_spam_package_scan_result_name) as TextView
        val addressText = rowView.findViewById(R.id.list_item_spam_package_scan_result_address) as TextView
        val rssiText = rowView.findViewById(R.id.list_item_spam_package_scan_result_rssi) as TextView
        val deviceTypeText = rowView.findViewById(R.id.list_item_spam_package_scan_result_deviceType) as TextView

        nameText.text = spamPackages[position].spamPackageType.toString()
        addressText.text = spamPackages[position].address
        rssiText.text = spamPackages[position].rssi.toString() + " dBm"
        deviceTypeText.text = spamPackages[position].deviceName

        return rowView
    }
}
