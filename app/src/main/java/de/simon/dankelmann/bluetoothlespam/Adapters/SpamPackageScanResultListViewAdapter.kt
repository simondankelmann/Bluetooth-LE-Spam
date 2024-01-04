package de.simon.dankelmann.bluetoothlespam.Adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Enums.FlipperDeviceType
import de.simon.dankelmann.bluetoothlespam.Enums.SpamPackageType
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.Models.SpamPackageScanResult
import de.simon.dankelmann.bluetoothlespam.R


/*
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
*/

class SpamPackageScanResultListViewAdapter(var mList: MutableList<SpamPackageScanResult>) : RecyclerView.Adapter<SpamPackageScanResultListViewAdapter.ViewHolder>() {
    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_spam_package_scan_result, parent, false)
        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val ItemsViewModel = mList[position]

        holder.nameText.text = when(ItemsViewModel.spamPackageType){
            SpamPackageType.UNKNOWN -> "Unknown Spam"
            SpamPackageType.FAST_PAIRING -> "Fast Pairing"
            SpamPackageType.CONTINUITY_NEW_AIRTAG -> "Continuity Airtag"
            SpamPackageType.CONTINUITY_NEW_DEVICE -> "Continuity new Device"
            SpamPackageType.CONTINUITY_NOT_YOUR_DEVICE -> "Continuity not your Device"
            SpamPackageType.CONTINUITY_ACTION_MODAL -> "Continuity Action Modal"
            SpamPackageType.CONTINUITY_IOS_17_CRASH -> "Continuity iOS 17 Crash"
            SpamPackageType.SWIFT_PAIRING -> "Swift Pairing"
            SpamPackageType.EASY_SETUP_WATCH -> "Easy Setup Watch"
            SpamPackageType.EASY_SETUP_BUDS -> "Easy Setup Buds"
            SpamPackageType.LOVESPOUSE_PLAY -> "Lovespouse Play"
            SpamPackageType.LOVESPOUSE_STOP -> "Lovespouse Stop"
        }

        var spamPackageIcon:Drawable = when(ItemsViewModel.spamPackageType){
            SpamPackageType.UNKNOWN -> AppContext.getActivity().resources.getDrawable(R.drawable.bluetooth, AppContext.getContext().theme)
            SpamPackageType.FAST_PAIRING -> AppContext.getActivity().resources.getDrawable(R.drawable.ic_android, AppContext.getContext().theme)
            SpamPackageType.CONTINUITY_NEW_AIRTAG -> AppContext.getActivity().resources.getDrawable(R.drawable.apple, AppContext.getContext().theme)
            SpamPackageType.CONTINUITY_NEW_DEVICE -> AppContext.getActivity().resources.getDrawable(R.drawable.apple, AppContext.getContext().theme)
            SpamPackageType.CONTINUITY_NOT_YOUR_DEVICE -> AppContext.getActivity().resources.getDrawable(R.drawable.apple, AppContext.getContext().theme)
            SpamPackageType.CONTINUITY_ACTION_MODAL -> AppContext.getActivity().resources.getDrawable(R.drawable.apple, AppContext.getContext().theme)
            SpamPackageType.CONTINUITY_IOS_17_CRASH -> AppContext.getActivity().resources.getDrawable(R.drawable.apple, AppContext.getContext().theme)
            SpamPackageType.SWIFT_PAIRING -> AppContext.getActivity().resources.getDrawable(R.drawable.microsoft, AppContext.getContext().theme)
            SpamPackageType.EASY_SETUP_WATCH -> AppContext.getActivity().resources.getDrawable(R.drawable.samsung, AppContext.getContext().theme)
            SpamPackageType.EASY_SETUP_BUDS -> AppContext.getActivity().resources.getDrawable(R.drawable.samsung, AppContext.getContext().theme)
            SpamPackageType.LOVESPOUSE_PLAY -> AppContext.getActivity().resources.getDrawable(R.drawable.heart, AppContext.getContext().theme)
            SpamPackageType.LOVESPOUSE_STOP -> AppContext.getActivity().resources.getDrawable(R.drawable.heart, AppContext.getContext().theme)
        }

        holder.icon.setImageDrawable(spamPackageIcon)


        holder.addressText.text = ItemsViewModel.address
        holder.rssiText.text = "${ItemsViewModel.rssi} dBm"
        holder.deviceTypeText.text = ""
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val nameText = ItemView.findViewById(R.id.list_item_spam_package_scan_result_name) as TextView
        val addressText = ItemView.findViewById(R.id.list_item_spam_package_scan_result_address) as TextView
        val rssiText = ItemView.findViewById(R.id.list_item_spam_package_scan_result_rssi) as TextView
        val deviceTypeText = ItemView.findViewById(R.id.list_item_spam_package_scan_result_deviceType) as TextView
        val icon = ItemView.findViewById(R.id.list_item_spam_package_scan_result_icon) as ImageView

    }
}