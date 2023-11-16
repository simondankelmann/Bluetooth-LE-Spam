package de.simon.dankelmann.bluetoothlespam.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.databinding.ListItemAdvertisementListBinding
import de.simon.dankelmann.bluetoothlespam.databinding.ListItemAdvertisementSetBinding


class AdvertisementSetCollectionExpandableListViewAdapter  internal constructor(
    private val context: Context,
    val advertisementSetLists: List<AdvertisementSetList>,
    val dataList: HashMap<AdvertisementSetList, List<AdvertisementSet>>
) : BaseExpandableListAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var groupBinding: ListItemAdvertisementListBinding
    private lateinit var itemBinding: ListItemAdvertisementSetBinding

    override fun getChild(listPosition: Int, expandedListPosition: Int): Any {
        return this.dataList[this.advertisementSetLists[listPosition]]!![expandedListPosition]
    }

    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }

    override fun getChildView(
        listPosition: Int,
        expandedListPosition: Int,
        isLastChild: Boolean,
        view: View?,
        parent: ViewGroup
    ): View {
        var convertView = view
        val holder: ItemViewHolder
        if (convertView == null) {
            itemBinding = ListItemAdvertisementSetBinding.inflate(inflater)
            convertView = itemBinding.root
            holder = ItemViewHolder()
            holder.label = itemBinding.listItemAdvertisementSetTextView
            convertView.tag = holder
        } else {
            holder = convertView.tag as ItemViewHolder
        }

        var advertisementSet = getChild(listPosition, expandedListPosition) as AdvertisementSet

        var textColor = when(advertisementSet.currentlyAdvertising){
            true -> AppContext.getActivity().resources.getColor(R.color.blue_normal, AppContext.getContext().theme)
            false -> AppContext.getActivity().resources.getColor(R.color.text_color, AppContext.getContext().theme)
        }

        holder.label!!.text = advertisementSet.title
        holder.label!!.setTextColor(textColor)
        return convertView
    }

    override fun getChildrenCount(listPosition: Int): Int {
        return this.dataList[this.advertisementSetLists[listPosition]]!!.size
    }

    override fun getGroup(listPosition: Int): Any {
        return this.advertisementSetLists[listPosition]
    }

    override fun getGroupCount(): Int {
        return this.advertisementSetLists.size
    }

    override fun getGroupId(listPosition: Int): Long {
        return listPosition.toLong()
    }

    override fun getGroupView(listPosition: Int, isExpanded: Boolean, view: View?, parent: ViewGroup): View {
        var convertView = view
        val holder: GroupViewHolder

        if (convertView == null) {
            groupBinding = ListItemAdvertisementListBinding.inflate(inflater)
            convertView = groupBinding.root
            holder = GroupViewHolder()
            holder.label = groupBinding.listItemAdvertisementSetList
            convertView.tag = holder
        } else {
            holder = convertView.tag as GroupViewHolder
        }

        var advertisementSetList = getGroup(listPosition) as AdvertisementSetList

        var textColor = when(advertisementSetList.currentlyAdvertising){
            true -> AppContext.getActivity().resources.getColor(R.color.blue_normal, AppContext.getContext().theme)
            false -> AppContext.getActivity().resources.getColor(R.color.text_color, AppContext.getContext().theme)
        }

        val listTitle = advertisementSetList.title

        holder.label!!.text = listTitle
        holder.label!!.setTextColor(textColor)

        return convertView
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean {
        return true
    }

    inner class ItemViewHolder {
        internal var label: TextView? = null
    }

    inner class GroupViewHolder {
        internal var label: TextView? = null
    }
}

/*
class AdvertisementSetCollectionExpandableListViewAdapter  internal constructor(
    private val context: Context,
    private val titleList: List<String>,
    private val dataList: HashMap<String, List<String>>
) : BaseExpandableListAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var groupBinding: ListItemAdvertisementListBinding
    private lateinit var itemBinding: ListItemAdvertisementsetBinding

    override fun getChild(listPosition: Int, expandedListPosition: Int): Any {
        return this.dataList[this.titleList[listPosition]]!![expandedListPosition]
    }

    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }

    override fun getChildView(
        listPosition: Int,
        expandedListPosition: Int,
        isLastChild: Boolean,
        view: View?,
        parent: ViewGroup
    ): View {
        var convertView = view
        val holder: ItemViewHolder
        if (convertView == null) {
            itemBinding = ListItemAdvertisementsetBinding.inflate(inflater)
            convertView = itemBinding.root
            holder = ItemViewHolder()
            holder.label = itemBinding.listItemAdvertisementSetTextView
            convertView.tag = holder
        } else {
            holder = convertView.tag as ItemViewHolder
        }
        val expandedListText = getChild(listPosition, expandedListPosition) as String
        holder.label!!.text = expandedListText
        return convertView
    }

    override fun getChildrenCount(listPosition: Int): Int {
        return this.dataList[this.titleList[listPosition]]!!.size
    }

    override fun getGroup(listPosition: Int): Any {
        return this.titleList[listPosition]
    }

    override fun getGroupCount(): Int {
        return this.titleList.size
    }

    override fun getGroupId(listPosition: Int): Long {
        return listPosition.toLong()
    }

    override fun getGroupView(
        listPosition: Int,
        isExpanded: Boolean,
        view: View?,
        parent: ViewGroup
    ): View {
        var convertView = view
        val holder: GroupViewHolder
        if (convertView == null) {
            groupBinding = ListItemAdvertisementListBinding.inflate(inflater)
            convertView = groupBinding.root
            holder = GroupViewHolder()
            holder.label = groupBinding.listItemAdvertisementSetList
            convertView.tag = holder
        } else {
            holder = convertView.tag as GroupViewHolder
        }
        val listTitle = getGroup(listPosition) as String
        holder.label!!.text = listTitle
        return convertView
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean {
        return true
    }

    inner class ItemViewHolder {
        internal var label: TextView? = null
    }

    inner class GroupViewHolder {
        internal var label: TextView? = null
    }
}
*/
