package de.simon.dankelmann.bluetoothlespam.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import de.simon.dankelmann.bluetoothlespam.Enums.SpamPackageType
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

class SpamPackageScanResultListViewAdapter(
    var mList: MutableList<SpamPackageScanResult>,
    private val context: Context,
) : RecyclerView.Adapter<SpamPackageScanResultListViewAdapter.ViewHolder>() {

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_spam_package_scan_result, parent, false)
        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemsViewModel = mList[position]

        holder.nameText.text = when (itemsViewModel.spamPackageType) {
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

        val spamPackageIconId: Int = when (itemsViewModel.spamPackageType) {
            SpamPackageType.UNKNOWN -> R.drawable.bluetooth
            SpamPackageType.FAST_PAIRING -> R.drawable.ic_android
            SpamPackageType.CONTINUITY_NEW_AIRTAG -> R.drawable.apple
            SpamPackageType.CONTINUITY_NEW_DEVICE -> R.drawable.apple
            SpamPackageType.CONTINUITY_NOT_YOUR_DEVICE -> R.drawable.apple
            SpamPackageType.CONTINUITY_ACTION_MODAL -> R.drawable.apple
            SpamPackageType.CONTINUITY_IOS_17_CRASH -> R.drawable.apple
            SpamPackageType.SWIFT_PAIRING -> R.drawable.microsoft
            SpamPackageType.EASY_SETUP_WATCH -> R.drawable.samsung
            SpamPackageType.EASY_SETUP_BUDS -> R.drawable.samsung
            SpamPackageType.LOVESPOUSE_PLAY -> R.drawable.heart
            SpamPackageType.LOVESPOUSE_STOP -> R.drawable.heart
        }
        val spamPackageIcon =
            ResourcesCompat.getDrawable(context.resources, spamPackageIconId, context.theme)

        holder.icon.setImageDrawable(spamPackageIcon)

        holder.addressText.text = itemsViewModel.address
        holder.rssiText.text = "${itemsViewModel.rssi} dBm"
        holder.deviceTypeText.text = ""
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.list_item_spam_package_scan_result_name)
        val addressText: TextView =
            itemView.findViewById(R.id.list_item_spam_package_scan_result_address)
        val rssiText: TextView = itemView.findViewById(R.id.list_item_spam_package_scan_result_rssi)
        val deviceTypeText: TextView =
            itemView.findViewById(R.id.list_item_spam_package_scan_result_deviceType)
        val icon: ImageView = itemView.findViewById(R.id.list_item_spam_package_scan_result_icon)
    }
}