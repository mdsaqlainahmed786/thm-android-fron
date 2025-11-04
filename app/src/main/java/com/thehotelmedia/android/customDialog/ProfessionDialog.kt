package com.thehotelmedia.android.customDialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.thehotelmedia.android.R
import com.thehotelmedia.android.adapters.dropDown.BusinessTypeAdapter
import com.thehotelmedia.android.adapters.dropDown.Businesses
import com.thehotelmedia.android.databinding.DialogAddProfessionBinding
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel

class ProfessionDialog(
    private val activity: AppCompatActivity, // Changed from Activity to AppCompatActivity
    private val authViewModel: AuthViewModel,
    private val onSubmit: (String) -> Unit,
    private val onCancel: () -> Unit = {} // default empty lambda
) {

    private lateinit var dialog: android.app.Dialog
    private var professionList: List<Businesses> = emptyList()

    fun show() {
        dialog = android.app.Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogAddProfessionBinding.inflate(LayoutInflater.from(activity))
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        // ✅ Set dialog to match parent width and wrap content height
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Observe GET Profession API result
        authViewModel.professionResult.observe(activity) { result ->
            if (result.status == true) {
                professionList = result.professionData.map {
                    Businesses(it.name.toString(), "", "")
                }

                val adapter = BusinessTypeAdapter(activity, professionList)
                binding.businessTypesTv.setAdapter(adapter)
                binding.businessTypesTv.setDropDownBackgroundDrawable(
                    ContextCompat.getDrawable(activity, R.drawable.blured_background)
                )
                binding.businessTypesTv.dropDownVerticalOffset = binding.businessTypesTv.height + 30
            } else {
                Toast.makeText(activity, result.message, Toast.LENGTH_SHORT).show()
            }
        }

        // Observe Profession API result
        authViewModel.editProfessionResult.observe(activity) { result ->
            if (result.status == true) {
                onSubmit("profession")
                dialog.dismiss()
            } else {
                Toast.makeText(activity, result.message, Toast.LENGTH_SHORT).show()
            }
        }

        // Call API
        authViewModel.getProfession()

        binding.cancelBtn.setOnClickListener {
            onCancel()
            dialog.dismiss()
        }

        // Handle dropdown selection
        binding.businessTypesTv.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position) as Businesses
            binding.businessTypesTv.setTextColor(ContextCompat.getColor(activity, R.color.text_color))

            if (selected.name == "Others") {
                binding.otherProfessionEt.visibility = View.VISIBLE
            } else {
                binding.otherProfessionEt.visibility = View.GONE
            }
        }

        // Submit button click
        binding.submitBtn.setOnClickListener {
            val selectedProfession = binding.businessTypesTv.text.toString().trim()
            println("asdkfajksdghk    $selectedProfession")


            val languageProfession = activity.getString(R.string.profession)
            if (selectedProfession == languageProfession) {
                Toast.makeText(activity, activity.getString(R.string.select_profession), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // If user selected "Others", check the EditText
            if (selectedProfession == "Others") {
                val otherProfession = binding.otherProfessionEt.text.toString().trim()
                if (otherProfession.isEmpty()) {
                    Toast.makeText(activity, activity.getString(R.string.enter_profession), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                } else {
                    editProfession(otherProfession)
                }
            } else {
                editProfession(selectedProfession)
            }
        }

        dialog.show()
    }

    private fun editProfession(profession: String) {
        authViewModel.editProfession(profession)
    }
}
