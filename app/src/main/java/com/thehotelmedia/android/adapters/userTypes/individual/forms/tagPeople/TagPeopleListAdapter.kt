package com.thehotelmedia.android.adapters.userTypes.individual.forms.tagPeople

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.userTypes.forms.createPost.TagPeople
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.modals.forms.taggedPeople.TaggedData
import com.thehotelmedia.android.databinding.TagPeopleListItemsLayoutBinding
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.modals.forms.taggedPeople.BusinessProfilePic
import com.thehotelmedia.android.modals.forms.taggedPeople.BusinessProfileRef
import com.thehotelmedia.android.modals.forms.taggedPeople.BusinessTypeRef
import com.thehotelmedia.android.modals.forms.taggedPeople.ProfilePic

class TagPeopleListAdapter(
    private val context: Context,
    private val onItemSelected: (ArrayList<TagPeople>) -> Unit,
    private val isCollaboration: Boolean = false
) : PagingDataAdapter<TaggedData, TagPeopleListAdapter.ViewHolder>(TagPeopleDiffCallback()) {
    private val MAX_TAGGED_PEOPLE = 30
    private val MAX_COLLABORATORS = 1 // Only one collaborator allowed
    // List to keep track of selected items
    private val selectedItems = ArrayList<TaggedData>()

    inner class ViewHolder(val binding: TagPeopleListItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = TagPeopleListItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tagPeople = getItem(position) // Use getItem() to retrieve items in PagingDataAdapter

        tagPeople?.let {
            // Set text for name and username
            val accountType = it.accountType?.capitalizeFirstLetter() ?: ""
            var name: String
            var profilePic: String
            val userName = it.username ?: ""
            val businessType = it.businessProfileRef?.businessTypeRef?.name ?: ""
            val businessTypeImage = it.businessProfileRef?.businessTypeRef?.icon ?: ""

            if (accountType == business_type_individual) {
                name = it.name.toString()
                profilePic = it.profilePic?.medium.toString()
                holder.binding.userNameTv.visibility = View.VISIBLE
                holder.binding.businessLayout.visibility = View.GONE
            } else {
                name = it.businessProfileRef?.name.toString()
                profilePic = it.businessProfileRef?.businessProfilePic?.medium.toString()
                holder.binding.userNameTv.visibility = View.GONE
                holder.binding.businessLayout.visibility = View.VISIBLE
            }

            holder.binding.nameTv.text = name
            holder.binding.userNameTv.text = userName
            holder.binding.businessTypeTv.text = businessType
            Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(holder.binding.profileIv)
            Glide.with(context).load(businessTypeImage)
                .placeholder(R.drawable.ic_hotel)
                .transform(ColorFilterTransformation(ContextCompat.getColor(context, R.color.text_color)))
                .into(holder.binding.businessTypeIv)

            // Check if the item is selected
            val isSelected = selectedItems.any { selectedItem -> selectedItem.Id == it.Id }
            holder.binding.tick.visibility = if (isSelected) View.VISIBLE else View.GONE

            // Handle item click
            holder.itemView.setOnClickListener { view ->
                view.isEnabled = false // Disable the item click temporarily

                if (isSelected) {
                    selectedItems.removeAll { selectedItem -> selectedItem.Id == it.Id }
                } else {
                    // For collaboration, only allow one selection - replace existing selection
                    if (isCollaboration) {
                        if (selectedItems.isNotEmpty()) {
                            // Clear the previous selection first
                            val previousPosition = selectedItems.firstOrNull()?.let { prevItem ->
                                // Find the position of the previously selected item
                                // We'll need to notify all items to update their checkmarks
                                notifyDataSetChanged()
                            }
                            selectedItems.clear()
                        }
                        selectedItems.add(it)
                    } else {
                        // For tag people, check if limit is reached
                        if (selectedItems.size >= MAX_TAGGED_PEOPLE) {
                            Toast.makeText(context, context.getString(R.string.max_tagged_people_message, MAX_TAGGED_PEOPLE), Toast.LENGTH_SHORT).show()
                            view.isEnabled = true // Re-enable the item click
                            return@setOnClickListener
                        }
                        selectedItems.add(it)
                    }
                }

                notifyItemChanged(position)

                // Convert selectedItems from TaggedData to TagPeople
                val selectedTagPeopleList = selectedItems.map { selectedItem ->
                    TagPeople(
                        id = selectedItem.Id.toString(),
                        name = if (selectedItem.accountType == "individual") selectedItem.name.toString()
                        else selectedItem.businessProfileRef?.name.toString(),
                        profilePic = if (selectedItem.accountType == "individual") selectedItem.profilePic?.medium.toString()
                        else selectedItem.businessProfileRef?.businessProfilePic?.medium.toString(),
                        accountType = selectedItem.accountType ?: "", // Add account type
                        businessTypeName = selectedItem.businessProfileRef?.businessTypeRef?.name ?: "", // Add business type name
                        businessTypeIcon = selectedItem.businessProfileRef?.businessTypeRef?.icon ?: "" // Add business type icon
                    )
                } as ArrayList<TagPeople>

                android.util.Log.d("TagPeopleListAdapter", "Selected ${selectedTagPeopleList.size} people: ${selectedTagPeopleList.map { it.name }}")
                // Call the lambda function with the updated list of TagPeople
                onItemSelected(selectedTagPeopleList)

                // Re-enable the item click after a short delay
                view.postDelayed({ view.isEnabled = true }, 400)
            }
        }
    }

//    fun setSelectedItems(selectedTagPeopleList: ArrayList<TagPeople>) {
//        selectedItems.clear()
//        // Convert TagPeople back to TaggedData
//        val convertedItems = selectedTagPeopleList.map { tagPeople ->
//            TaggedData(
//                Id = tagPeople.id,  // Use TagPeople's id to map back to TaggedData's Id
//                name = tagPeople.name,  // Use TagPeople's name
//                accountType = tagPeople.accountType,
//                profilePic = ProfilePic(medium = tagPeople.profilePic),  // Convert TagPeople's profilePic string back to TaggedData's ProfilePic
//            )
//        }
//
//        selectedItems.addAll(convertedItems)
//        println("fajskadhfjkashfj   $selectedItems")
//        notifyDataSetChanged()
//    }

    fun setSelectedItems(selectedTagPeopleList: ArrayList<TagPeople>) {
        selectedItems.clear()
        // For collaboration, only keep the first item if multiple are provided
        val itemsToProcess = if (isCollaboration && selectedTagPeopleList.size > 1) {
            arrayListOf(selectedTagPeopleList.first())
        } else {
            selectedTagPeopleList
        }
        // Convert TagPeople back to TaggedData
        val convertedItems = itemsToProcess.map { tagPeople ->
            // Check the account type
            if (tagPeople.accountType == "individual") {
                TaggedData(
                    Id = tagPeople.id,
                    name = tagPeople.name,
                    accountType = tagPeople.accountType,
                    profilePic = ProfilePic(medium = tagPeople.profilePic),  // Assign directly to profilePic
                    // No need for BusinessProfileRef
                    businessProfileRef = null
                )
            } else { // Assuming any other accountType is business
                TaggedData(
                    Id = tagPeople.id,
                    name = tagPeople.name,
                    accountType = tagPeople.accountType,
                    profilePic = null,  // Set profilePic to null for business
                    // Assign to BusinessProfileRef
                    businessProfileRef = BusinessProfileRef(
                        businessProfilePic = BusinessProfilePic(medium = tagPeople.profilePic),
                        name = tagPeople.name,  // Assigning the name for business
                        address = null, // or set to a valid Address object if you have one
                        businessTypeRef = BusinessTypeRef(name = tagPeople.businessTypeName, icon = tagPeople.businessTypeIcon) // Assuming these are also part of TagPeople
                    )
                )
            }
        }

        selectedItems.addAll(convertedItems)
        android.util.Log.d("TagPeopleListAdapter", "Restored ${selectedItems.size} selected items: ${selectedItems.map { it.Id }}")
        println("Converted TaggedData: $selectedItems")
        notifyDataSetChanged()
    }




    fun unselectItem(item: TagPeople) {
        selectedItems.removeAll { selectedItem -> selectedItem.Id == item.id }
        notifyDataSetChanged() // Refresh the entire list
    }
}

// DiffUtil for TaggedData class to calculate the difference between old and new items
class TagPeopleDiffCallback : DiffUtil.ItemCallback<TaggedData>() {
    override fun areItemsTheSame(oldItem: TaggedData, newItem: TaggedData): Boolean {
        return oldItem.Id == newItem.Id
    }

    override fun areContentsTheSame(oldItem: TaggedData, newItem: TaggedData): Boolean {
        return oldItem == newItem
    }
}
