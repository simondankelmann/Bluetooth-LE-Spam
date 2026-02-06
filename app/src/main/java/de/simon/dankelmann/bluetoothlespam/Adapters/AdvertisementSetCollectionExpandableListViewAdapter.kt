package de.simon.dankelmann.bluetoothlespam.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementState
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.databinding.ListItemAdvertisementListBinding
import de.simon.dankelmann.bluetoothlespam.databinding.ListItemAdvertisementSetBinding

/**
 * ExpandableListView adapter that displays AdvertisementSetLists (groups)
 * and their corresponding AdvertisementSets (children).
 */
class AdvertisementSetCollectionExpandableListViewAdapter internal constructor(
    private val context: Context,

    // List of group items
    val advertisementSetLists: List<AdvertisementSetList>,

    // Mapping of group -> children
    val dataList: HashMap<AdvertisementSetList, List<AdvertisementSet>>
) : BaseExpandableListAdapter() {

    // LayoutInflater for inflating XML layouts
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    // ViewBinding references (reused during inflation)
    private lateinit var groupBinding: ListItemAdvertisementListBinding
    private lateinit var itemBinding: ListItemAdvertisementSetBinding

    /**
     * Returns a child item for a given group and child position
     */
    override fun getChild(listPosition: Int, expandedListPosition: Int): Any {
        return dataList[advertisementSetLists[listPosition]]!![expandedListPosition]
    }

    /**
     * Returns the child ID (position-based)
     */
    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }

    /**
     * Creates or reuses a child view (AdvertisementSet)
     */
    override fun getChildView(
        listPosition: Int,
        expandedListPosition: Int,
        isLastChild: Boolean,
        view: View?,
        parent: ViewGroup
    ): View {
        var convertView = view
        val holder: ItemViewHolder

        // Inflate view if it does not exist yet
        if (convertView == null) {
            itemBinding = ListItemAdvertisementSetBinding.inflate(inflater)
            convertView = itemBinding.root

            // Create and store ViewHolder
            holder = ItemViewHolder()
            holder.label = itemBinding.listItemAdvertisementSetTextView
            holder.checkbox = itemBinding.checkboxAdvertisementSet
            convertView.tag = holder
        } else {
            // Reuse existing ViewHolder
            holder = convertView.tag as ItemViewHolder
        }

        // Retrieve the current AdvertisementSet
        val advertisementSet = getChild(listPosition, expandedListPosition) as AdvertisementSet

        /**
         * Determine text color based on advertising state
         */
        var textColor = when (advertisementSet.currentlyAdvertising) {
            true -> context.resources.getColor(R.color.blue_normal, context.theme)
            false -> context.resources.getColor(R.color.text_color, context.theme)
        }

        // Override color based on success or failure state
        if (advertisementSet.currentlyAdvertising) {
            if (advertisementSet.advertisementState == AdvertisementState.ADVERTISEMENT_STATE_SUCCEEDED) {
                textColor = context.resources.getColor(R.color.log_success, context.theme)
            } else if (advertisementSet.advertisementState == AdvertisementState.ADVERTISEMENT_STATE_FAILED) {
                textColor = context.resources.getColor(R.color.log_error, context.theme)
            }
        } else {
            textColor = context.resources.getColor(R.color.text_color, context.theme)
        }

        // Set label text and color
        holder.label?.text = advertisementSet.title
        holder.label?.setTextColor(textColor)

        /**
         * Checkbox handling
         */

        // Initialize checkbox state from model
        holder.checkbox?.isChecked = advertisementSet.isChecked

        // Update model when checkbox is toggled
        holder.checkbox?.setOnClickListener { view ->
            val isChecked = (view as android.widget.CheckBox).isChecked
            advertisementSet.isChecked = isChecked

            // Prevent checkbox click from triggering list item selection
            view.isPressed = false
        }

        return convertView
    }

    /**
     * Returns number of children for a group
     */
    override fun getChildrenCount(listPosition: Int): Int {
        return dataList[advertisementSetLists[listPosition]]!!.size
    }

    /**
     * Returns a group item
     */
    override fun getGroup(listPosition: Int): Any {
        return advertisementSetLists[listPosition]
    }

    /**
     * Returns number of groups
     */
    override fun getGroupCount(): Int {
        return advertisementSetLists.size
    }

    /**
     * Returns group ID (position-based)
     */
    override fun getGroupId(listPosition: Int): Long {
        return listPosition.toLong()
    }

    /**
     * Creates or reuses a group view (AdvertisementSetList)
     */
    override fun getGroupView(
        listPosition: Int,
        isExpanded: Boolean,
        view: View?,
        parent: ViewGroup
    ): View {
        var convertView = view
        val holder: GroupViewHolder

        // Inflate group view if needed
        if (convertView == null) {
            groupBinding = ListItemAdvertisementListBinding.inflate(inflater)
            convertView = groupBinding.root

            // Create and store ViewHolder
            holder = GroupViewHolder()
            holder.label = groupBinding.listItemAdvertisementSetList
            holder.checkbox = groupBinding.checkboxAdvertisementList
            convertView.tag = holder
        } else {
            holder = convertView.tag as GroupViewHolder
        }

        // Retrieve group item
        val advertisementSetList = getGroup(listPosition) as AdvertisementSetList

        // Set text color based on advertising state
        val textColor = when (advertisementSetList.currentlyAdvertising) {
            true -> context.resources.getColor(R.color.blue_normal, context.theme)
            false -> context.resources.getColor(R.color.text_color, context.theme)
        }

        holder.label?.text = advertisementSetList.title
        holder.label?.setTextColor(textColor)

        /**
         * Group checkbox logic
         */

        // Group checkbox is checked if ALL children are checked
        val allChildrenChecked =
            dataList[advertisementSetList]?.all { it.isChecked } ?: false
        holder.checkbox?.isChecked = allChildrenChecked

        // Toggle all children when group checkbox is clicked
        holder.checkbox?.setOnClickListener { view ->
            val isChecked = (view as android.widget.CheckBox).isChecked

            // Update all child items
            dataList[advertisementSetList]?.forEach { advertisementSet ->
                advertisementSet.isChecked = isChecked
            }

            // Refresh views so child checkboxes update
            notifyDataSetChanged()

            // Prevent checkbox click from expanding/collapsing group
            view.isPressed = false
        }

        return convertView
    }

    /**
     * IDs are not stable
     */
    override fun hasStableIds(): Boolean {
        return false
    }

    /**
     * Children are selectable
     */
    override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean {
        return true
    }

    /**
     * ViewHolder for child items
     */
    inner class ItemViewHolder {
        internal var label: TextView? = null
        internal var checkbox: android.widget.CheckBox? = null
    }

    /**
     * ViewHolder for group items
     */
    inner class GroupViewHolder {
        internal var label: TextView? = null
        internal var checkbox: android.widget.CheckBox? = null
    }
}
