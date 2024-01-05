package de.simon.dankelmann.bluetoothlespam.Adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.simon.dankelmann.bluetoothlespam.Enums.FlipperDeviceType
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.Models.SpamPackageScanResult
import de.simon.dankelmann.bluetoothlespam.R

/*
class FlipperDeviceScanResultListViewAdapter(private val context: Activity, var flipperDevices: MutableList<FlipperDeviceScanResult>) : ArrayAdapter<FlipperDeviceScanResult>(context, R.layout.list_item_flipper_device_scan_result, flipperDevices) {
    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.list_item_flipper_device_scan_result, null, true)

        val nameText = rowView.findViewById(R.id.list_item_flipper_device_scan_result_name) as TextView
        val addressText = rowView.findViewById(R.id.list_item_flipper_device_scan_result_address) as TextView
        val rssiText = rowView.findViewById(R.id.list_item_flipper_device_scan_result_rssi) as TextView
        val deviceTypeText = rowView.findViewById(R.id.list_item_flipper_device_scan_result_deviceType) as TextView
        //val icon = rowView.findViewById(R.id.list_item_flipper_device_scan_result_icon) as ImageView


        nameText.text = flipperDevices[position].deviceName
        addressText.text = flipperDevices[position].address
        rssiText.text = flipperDevices[position].rssi.toString() + " dBm"
        deviceTypeText.text = when(flipperDevices[position].flipperDeviceType){
            FlipperDeviceType.FLIPPER_ZERO_WHITE -> "Flipper Zero White"
            FlipperDeviceType.FLIPPER_ZERO_BLACK -> "Flipper Zero Black"
            FlipperDeviceType.FLIPPER_ZERO_TRANSPARENT -> "Flipper ZeroTransparent"
            FlipperDeviceType.UNKNOWN -> "Unknown Flipper Zero"
        }


        return rowView
    }
}*/

class FlipperDeviceScanResultListViewAdapter(var mList: MutableList<FlipperDeviceScanResult>) : RecyclerView.Adapter<FlipperDeviceScanResultListViewAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_flipper_device_scan_result, parent, false)
        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val ItemsViewModel = mList[position]

        holder.nameText.text = ItemsViewModel.deviceName
        holder.addressText.text = ItemsViewModel.address
        holder.rssiText.text = "${ItemsViewModel.rssi} dBm"

        holder.deviceTypeText.text =  when(ItemsViewModel.flipperDeviceType){
            FlipperDeviceType.FLIPPER_ZERO_WHITE -> "Flipper Zero White"
            FlipperDeviceType.FLIPPER_ZERO_BLACK -> "Flipper Zero Black"
            FlipperDeviceType.FLIPPER_ZERO_TRANSPARENT -> "Flipper ZeroTransparent"
            FlipperDeviceType.UNKNOWN -> "Unknown Flipper Zero"
        }
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val nameText = ItemView.findViewById(R.id.list_item_flipper_device_scan_result_name) as TextView
        val addressText = ItemView.findViewById(R.id.list_item_flipper_device_scan_result_address) as TextView
        val rssiText = ItemView.findViewById(R.id.list_item_flipper_device_scan_result_rssi) as TextView
        val deviceTypeText = ItemView.findViewById(R.id.list_item_flipper_device_scan_result_deviceType) as TextView
    }
}

