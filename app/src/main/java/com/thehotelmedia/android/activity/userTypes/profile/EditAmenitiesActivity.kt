package com.thehotelmedia.android.activity.userTypes.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.authentication.business.EditedQuestionAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityEditAmenitiesBinding
import com.thehotelmedia.android.modals.authentication.business.questionAnswer.Data
import com.thehotelmedia.android.modals.profileData.profile.BusinessAnswerRef
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class EditAmenitiesActivity : BaseActivity() {

    private lateinit var binding: ActivityEditAmenitiesBinding

    private var questionAnswerList: ArrayList<Data>? = null
    private var answerAmenitiesRefList: ArrayList<BusinessAnswerRef>? = null
    private val selectedAnswers = mutableMapOf<String, String>()

    private lateinit var authViewModel: AuthViewModel
    private val activity = this@EditAmenitiesActivity
    private lateinit var preferenceManager : PreferenceManager
    private var selectedBusinessId : String = ""
    private var selectedSubBusinessId : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAmenitiesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUi()
    }

    private fun initUi() {
        val authRepo = AuthRepo(activity)
        authViewModel = ViewModelProvider(activity, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        preferenceManager = PreferenceManager.getInstance(activity)
        val progressBar = CustomProgressBar(activity) // 'this' refers to the context
        answerAmenitiesRefList = preferenceManager.getAnswerAmenitiesList(PreferenceManager.Keys.ANSWER_AMENITIES_REF_LIST)
        selectedBusinessId = preferenceManager.getString(PreferenceManager.Keys.USER_BUSINESS_ID,"").toString()
        selectedSubBusinessId = preferenceManager.getString(PreferenceManager.Keys.USER_SUB_BUSINESS_ID,"").toString()
//        binding.doneBtn.toggleEnable(false)

        getQuestionsAnswerList()

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.doneBtn.setOnClickListener {
            if (areAllQuestionsAnswered()) {
                sentAnswers()
            } else {
                CustomSnackBar.showSnackBar(binding.root, MessageStore.pleaseAnswerAllQuestions(this))
            }
        }


        authViewModel.questionAnswerResult.observe(activity) { result ->
            if (result.status == true) {

                questionAnswerList = result.data
                val editedQuestionAdapter = EditedQuestionAdapter(activity, questionAnswerList!!,answerAmenitiesRefList!!, ::onAnswerSelected)
                binding.relatedQuestionRv.adapter = editedQuestionAdapter

            } else {
                Toast.makeText(activity, result.message, Toast.LENGTH_SHORT).show()
            }
        }

        authViewModel.answerQuestionResult.observe(activity) { result ->
            if (result.status == true) {
                onBackPressedDispatcher.onBackPressed()
//                val intent = Intent(activity, BusinessTypeMediaActivity::class.java)
//                startActivity(intent)
            } else {
                Toast.makeText(activity, result.message, Toast.LENGTH_SHORT).show()
            }
        }

        authViewModel.loading.observe(activity) {
            if (it == true) {
                progressBar.show()
            } else {
                progressBar.hide()
            }
        }

        authViewModel.toast.observe(activity) {
            Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
        }

    }

    private fun getQuestionsAnswerList() {
        authViewModel.getQuestionAnswer(selectedBusinessId,selectedSubBusinessId)
    }
    private fun sentAnswers() {
        val answerJson = printSelectedAnswers()
        val requestBody = answerJson?.toRequestBody("application/json".toMediaTypeOrNull())
        authViewModel.answerQuestion(requestBody!!)
    }
    private fun printSelectedAnswers(): String? {
        val gson = Gson()
        val json = gson.toJson(selectedAnswers.map { (question, answer) ->
            mapOf("questionID" to question, "answer" to answer)
        })
        return json
    }

    private fun areAllQuestionsAnswered(): Boolean {
        return questionAnswerList?.all { selectedAnswers.containsKey(it.id) } ?: false
    }
    private fun onAnswerSelected(questionId: String, answer: String) {
        selectedAnswers[questionId] = answer
    }
}