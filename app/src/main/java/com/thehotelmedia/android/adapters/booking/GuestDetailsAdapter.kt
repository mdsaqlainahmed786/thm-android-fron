package com.thehotelmedia.android.adapters.booking

import android.content.Context
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.R
import com.thehotelmedia.android.adapters.dropDown.BusinessTypeAdapter
import com.thehotelmedia.android.adapters.dropDown.Businesses
import com.thehotelmedia.android.databinding.GuestDetailsItemLayoutBinding
import org.json.JSONArray
import org.json.JSONObject

// Data class representing a guest
data class Guest(
    @SerializedName("title") var title: String = "Mr",
    @SerializedName("fullName") var fullName: String = "",
    @SerializedName("email") var email: String = "",
    @SerializedName("mobileNumber") var mobileNumber: String = ""
)


class GuestDetailsAdapter(
    private val context: Context,
    private var guests: MutableList<Guest>,  // Yeh ab Guest ka list hoga
    private val maxGuestNumber: Int,
    private val onSelectionChanged: (String) -> Unit
) : RecyclerView.Adapter<GuestDetailsAdapter.MyViewHolder>() {

    private var selectedCountryCode : String = "+91"
    private var selectedValue: String = "myself"
    inner class MyViewHolder(private val binding: GuestDetailsItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            val guest = guests[position]

            // Set existing values
            binding.titleAutoTv.setText(guest.title, false)
            binding.nameEt.setText(guest.fullName)
            binding.emailEt.setText(guest.email)
            binding.contactEt.setText(guest.mobileNumber)

            //             Cancel button only for index > 0
            if (position == 0) {
                binding.cancelBtn.visibility = View.GONE // Hide for first index
                binding.checkboxMyself.visibility = View.VISIBLE
                binding.checkboxSomeoneElse.visibility = View.VISIBLE
            } else {
                binding.cancelBtn.visibility = View.VISIBLE // Show for all others
                binding.checkboxMyself.visibility = View.GONE
                binding.checkboxSomeoneElse.visibility = View.GONE
            }


            // EditText listeners to update the list
            binding.nameEt.addTextChangedListener { text ->
                guests[position].fullName = text.toString()
            }
            binding.emailEt.addTextChangedListener { text ->
                guests[position].email = text.toString()
            }
            binding.contactEt.addTextChangedListener { text ->
                guests[position].mobileNumber = "$selectedCountryCode ${text.toString()}"
            }

            // Set Gender Dropdown
            setGenderDropdown(guest)



            // Cancel Button Logic
            binding.cancelBtn.setOnClickListener {
                removeGuest(position)
            }

            // Set a listener for country code change
            binding.countryCodePicker.setOnCountryChangeListener {
                selectedCountryCode = binding.countryCodePicker.selectedCountryCodeWithPlus

                // Ensure that the contact number updates properly
                val currentContactNumber = binding.contactEt.text.toString()
                guests[position].mobileNumber = "$selectedCountryCode $currentContactNumber"

                // Update the ImageView with the selected country's flag
                binding.countryFlagImageView.setImageResource(binding.countryCodePicker.selectedCountryFlagResourceId)
            }

            setUpCheckBox(binding)
        }

        private fun setUpCheckBox(binding: GuestDetailsItemLayoutBinding) {
            binding.checkboxMyself.isChecked = true  // Ensure default selection
            binding.checkboxMyself.isEnabled = false
            binding.checkboxSomeoneElse.setTextColor(ContextCompat.getColor(context, R.color.text_color_60))

            binding.checkboxMyself.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedValue = "myself"
                    binding.checkboxMyself.setTextColor(ContextCompat.getColor(context, R.color.text_color))
                    binding.checkboxSomeoneElse.setTextColor(ContextCompat.getColor(context, R.color.text_color_60))
                    binding.checkboxSomeoneElse.isChecked = false
                    binding.checkboxMyself.isEnabled = false
                    binding.checkboxSomeoneElse.isEnabled = true
                    onSelectionChanged(selectedValue) // Notify activity
                }
            }

            binding.checkboxSomeoneElse.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedValue = "someone-else"
                    binding.checkboxSomeoneElse.setTextColor(ContextCompat.getColor(context, R.color.text_color))
                    binding.checkboxMyself.setTextColor(ContextCompat.getColor(context, R.color.text_color_60))
                    binding.checkboxMyself.isChecked = false
                    binding.checkboxMyself.isEnabled = true
                    binding.checkboxSomeoneElse.isEnabled = false
                    onSelectionChanged(selectedValue) // Notify activity
                }
            }
        }

        private fun setGenderDropdown(guest: Guest) {
            val genderTitles = listOf("Mr", "Mrs", "Ms")
            val titles = genderTitles.map { Businesses(it, "", it) }

            binding.titleAutoTv.setDropDownBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.blured_background))
            binding.titleAutoTv.dropDownVerticalOffset = binding.titleAutoTv.height + 30

            val businessTypeAdapter = BusinessTypeAdapter(context, titles)
            binding.titleAutoTv.setAdapter(businessTypeAdapter)

            binding.titleAutoTv.setOnItemClickListener { parent, _, position, _ ->
                val selectedItem = parent.getItemAtPosition(position) as Businesses
                guest.title = selectedItem.name
                binding.titleAutoTv.setText(selectedItem.name, false)
            }

            // **Set default selection**
            if (titles.isNotEmpty()) {
                val defaultTitle = titles.first()
                binding.titleAutoTv.setText(defaultTitle.name, false)
                binding.titleAutoTv.setTextColor(ContextCompat.getColor(context, R.color.text_color))
                guest.title = defaultTitle.name
//                selectTitleType = defaultTitle.name
//                selectTitleId = defaultTitle.id
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = GuestDetailsItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = guests.size

    fun addGuest() {
        if (guests.size < maxGuestNumber) {
            guests.add(Guest())  // Default Guest object add karenge
            notifyItemInserted(guests.size - 1)
        }
    }

    fun removeGuest(position: Int) {
        if (guests.size > 1) {
            guests.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, guests.size)
        }
    }

    fun getGuests(): List<Guest> = guests  // Yeh list directly JSON format me aa jayegi

    fun isAllGuestsFilled(): Boolean {
        return guests.all { guest ->
            val isNameValid = guest.fullName.isNotBlank()
            val isEmailValid = guest.email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(guest.email).matches()
            val isMobileValid = guest.mobileNumber.matches(Regex("^\\+\\d{1,4}\\d{6,15}$")) // Dial code + valid number

            isNameValid && isEmailValid && isMobileValid
        }
    }

    fun getGuestsAsJson(): String {
        return Gson().toJson(guests)
    }


    fun validateGuests(): String? {
        for (guest in guests) {
            if (guest.fullName.isBlank()) {
                return "Please enter the full name for all guests!"
            }
            if (guest.email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(guest.email).matches()) {
                return "Please enter a valid email for all guests!"
            }
            if (guest.email.any { it.isUpperCase() }) {
                return "Email should be in lowercase only!"
            }

            if (guest.mobileNumber.isBlank()) {
                return "Please enter a valid mobile number for all guests!"
            }
        }
        return null // Means all data is valid
    }


}
