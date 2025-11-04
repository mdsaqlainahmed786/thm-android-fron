package com.thehotelmedia.android.activity.authentication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.authentication.business.BusinessSubscriptionActivity
import com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity
import com.thehotelmedia.android.customClasses.Constants.LANGUAGE_CODE
import com.thehotelmedia.android.customClasses.Constants.business_type_business
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.ActivityTermsAndConditionsBinding
import com.thehotelmedia.android.extensions.makeCall
import com.thehotelmedia.android.extensions.openUrl
import com.thehotelmedia.android.extensions.sendEmail
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel
import java.util.regex.Pattern

class TermsAndConditionsActivity : BaseActivity() {

    private lateinit var binding: ActivityTermsAndConditionsBinding
    private var from: String? = null
    private var isChecked = false  // To track the checkbox state
    private lateinit var authViewModel: AuthViewModel
    private val activity = this@TermsAndConditionsActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTermsAndConditionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()
    }

    private fun initUI() {

        setupTermsTextView(this@TermsAndConditionsActivity)
        from = intent.getStringExtra("From") ?: ""

        val authRepo = AuthRepo(activity)
        authViewModel = ViewModelProvider(activity, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        val progressBar = CustomProgressBar(activity)


        // Initially disable the next button
        updateNextButtonState(isChecked)

        binding.backBtn.setOnClickListener {
            this.onBackPressedDispatcher.onBackPressed()
        }

        binding.checkBox.setOnClickListener {
            isChecked = !isChecked  // Toggle checkbox state
            updateNextButtonState(isChecked)
        }

        binding.nextBtn.setOnClickListener {
            setTermsAndConditions()
        }

        authViewModel.termsAndConditionsResult.observe(activity){result->
            if (result.status==true){
                val intent = if (from == business_type_business) {
                    Intent(this, BusinessSubscriptionActivity::class.java)
                } else {
                    Intent(this, BottomNavigationIndividualMainActivity::class.java)
                }
                startActivity(intent)
            }else{
                val msg = result.message
                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
            }
        }


        authViewModel.loading.observe(activity){
            if (it == true){
                progressBar.show() // To show the progress bar
            }else{
                progressBar.hide() // To hide the progress bar
            }
        }

        authViewModel.toast.observe(activity){
            Toast.makeText(activity,it, Toast.LENGTH_SHORT).show()
        }

    }

    private fun setTermsAndConditions() {
        authViewModel.termsAndConditions(isChecked)
    }

    private fun updateNextButtonState(isChecked: Boolean) {
        if (isChecked) {
            // Checkbox is checked, enable the next button
            binding.checkBoxIv.setImageResource(R.drawable.ic_selected_checkbox)  // Set to selected icon
            binding.nextBtn.isEnabled = true
            binding.nextBtn.strokeColor = ContextCompat.getColor(this, R.color.blue) // Set stroke to blue
            binding.nextBtnIcon.setBackgroundColor(ContextCompat.getColor(this, R.color.transaction_background))  // Set background to white
        } else {
            // Checkbox is unchecked, disable the next button
            binding.checkBoxIv.setImageResource(R.drawable.ic_unselected_checkbox)  // Set to unselected icon
            binding.nextBtn.isEnabled = false
            binding.nextBtn.strokeColor = ContextCompat.getColor(this, R.color.blue_50)  // Set stroke to blue_50
            binding.nextBtnIcon.setBackgroundColor(ContextCompat.getColor(this, R.color.white_60))  // Set background to white_60
        }
    }


    private fun formatTextWithMultipleHeadings(content: String): SpannableString {
        val spannableContent = SpannableStringBuilder(content) // Use SpannableStringBuilder for mutability


        val lines = content.lines()
        var startIndex = 0

        for (line in lines) {
            val originalLine = line

            // Apply styles to the text first, before removing the "#" symbols
            val cleanLine = when {
                line.startsWith("###") -> {
                    // Apply italic style for Level 2 headings
                    spannableContent.setSpan(
                        StyleSpan(Typeface.ITALIC), startIndex, startIndex + line.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    line // Return the original line with "###" to remove later
                }
                line.startsWith("##") -> {
                    // Apply bold style for Level 1 headings
                    spannableContent.setSpan(
                        StyleSpan(Typeface.BOLD), startIndex, startIndex + line.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannableContent.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(this@TermsAndConditionsActivity, R.color.text_color)), startIndex, startIndex + line.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    line // Return the original line with "##" to remove later
                }
                else -> {
                    line // No styling needed for non-heading lines
                }
            }

            // After applying the styles, remove the "#" symbols from the line
            val cleanLineWithoutHash = cleanLine.removePrefix("###").removePrefix("##").trim()

            // Replace the original line with the cleaned-up line (without the "#" symbols)
            spannableContent.replace(startIndex, startIndex + line.length, cleanLineWithoutHash)

            // Move startIndex to the end of the current line (including the newline character)
            startIndex += cleanLineWithoutHash.length + 1 // +1 for the newline character
        }

        return SpannableString(spannableContent)
    }

    private fun loadTextFileFromAssets(fileName: String, context: Context): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

//    private fun setupTermsTextView(activity: Activity) {
//        val fileContent = loadTextFileFromAssets("terms_en.txt", activity)
//
//        val formattedText = formatTextWithMultipleHeadings(fileContent)
//
//
//
//        val initialTextLength = 1100 // Limit for initial text display
//        var isExpanded = false // Keep track of the text state
//
//        // Declare the toggle function first
//        lateinit var toggleState: () -> Unit
//
//        // Function to update the text based on the state
//        val updateTextView: () -> Unit = {
//            binding.textView3.text = if (isExpanded) {
//                SpannableStringBuilder(formattedText).append(createReadLessSpan { toggleState() })
//            } else {
//                SpannableStringBuilder(
//                    formattedText.subSequence(0, initialTextLength)
//                ).append(createReadMoreSpan { toggleState() })
//            }
//        }
//
//        // Initialize the toggleState function after updateTextView is defined
//        toggleState = {
//            isExpanded = !isExpanded
//            updateTextView()
//        }
//
//        binding.textView3.apply {
//            // Set initial state
//            updateTextView()
//
//            // Ensure TextView handles links properly
//            movementMethod = LinkMovementMethod.getInstance()
//            highlightColor = Color.TRANSPARENT // Optional: remove background highlight on click
//        }
//
//
//
//    }


    private fun setupTermsTextView(activity: Activity) {

        val sharedPreferences = getSharedPreferences("ONBOARD", MODE_PRIVATE)
        val currentLanguageCode = sharedPreferences.getString(LANGUAGE_CODE, "en").orEmpty()

        // Construct the file name dynamically based on language
        val fileName = "terms_${currentLanguageCode}.txt"
        val fileContent = loadTextFileFromAssets(fileName, activity)


        val formattedText = formatTextWithMultipleHeadings(fileContent)
        val spannableText = SpannableStringBuilder(formattedText)

        // Add clickable spans for phone numbers, emails, and URLs
        addClickableSpans(spannableText)

        val initialTextLength = 1100 // Limit for initial text display
        var isExpanded = false // Keep track of the text state

        // Declare the toggle function first
        lateinit var toggleState: () -> Unit

        // Function to update the text based on the state
        val updateTextView: () -> Unit = {
            binding.textView3.text = if (isExpanded) {
                SpannableStringBuilder(spannableText).append(createReadLessSpan { toggleState() })
            } else {
                SpannableStringBuilder(
                    spannableText.subSequence(0, initialTextLength)
                ).append(createReadMoreSpan { toggleState() })
            }
        }

        // Initialize the toggleState function after updateTextView is defined
        toggleState = {
            isExpanded = !isExpanded
            updateTextView()
        }

        binding.textView3.apply {
            // Set initial state
            updateTextView()

            // Ensure TextView handles links properly
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT // Optional: remove background highlight on click
        }
    }


    private fun addClickableSpans(spannableText: SpannableStringBuilder) {
        // Patterns for phone numbers, emails, and URLs
        val phonePattern = Pattern.compile("\\+?\\d{1,3}\\s?\\d{10}")
        val emailPattern = Patterns.EMAIL_ADDRESS
        val urlPattern = Patterns.WEB_URL

        // Apply clickable spans for phone numbers
        applyPattern(spannableText, phonePattern) { match ->
            makeCall(match)
        }

        // Apply clickable spans for emails
        applyPattern(spannableText, emailPattern) { match ->
            sendEmail(match)
        }

        // Apply clickable spans for URLs
        applyPattern(spannableText, urlPattern) { match ->
            openUrl(match)
        }
    }

    private fun applyPattern(
        spannable: SpannableStringBuilder,
        pattern: Pattern,
        onClickAction: (String) -> Unit // Lambda to handle clicks
    ) {
        val matcher = pattern.matcher(spannable)
        while (matcher.find()) {
            val matchedText = matcher.group() ?: continue // Extract the matched text safely

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // Execute the passed lambda on click
//                    onClickAction(matcher.group() ?: "")
                    onClickAction(matchedText)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(this@TermsAndConditionsActivity, R.color.blue_70) // Set text color
                    ds.isUnderlineText = false // Disable underline
                }
            }
            spannable.setSpan(
                clickableSpan,
                matcher.start(),
                matcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    // Helper function to create a clickable "Read More" span
    private fun createReadMoreSpan(onClick: () -> Unit): SpannableString {
        val readMoreText = "... Read More"
        val spannable = SpannableString(readMoreText)
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onClick()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(this@TermsAndConditionsActivity, R.color.blue_70) // Change to desired color
                    ds.isUnderlineText = false // Optional: underline the text
                }
            },
            0,
            readMoreText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    // Helper function to create a clickable "Read Less" span
    private fun createReadLessSpan(onClick: () -> Unit): SpannableString {
        val readLessText = " Read Less"
        val spannable = SpannableString(readLessText)
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onClick()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(this@TermsAndConditionsActivity, R.color.blue_70) // Change to desired color
                    ds.isUnderlineText = false // Optional: underline the text
                }
            },
            0,
            readLessText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }
}

