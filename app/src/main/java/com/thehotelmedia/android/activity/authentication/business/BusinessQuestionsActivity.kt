package com.thehotelmedia.android.activity.authentication.business

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.authentication.business.RelatedQuestionAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityBusinessQuestionsBinding
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel
import com.thehotelmedia.android.modals.authentication.business.questionAnswer.Data
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class BusinessQuestionsActivity : BaseActivity() {

    private lateinit var binding: ActivityBusinessQuestionsBinding
    private var questionAnswerList: ArrayList<Data>? = null
    private val selectedAnswers = mutableMapOf<String, String>()

    private lateinit var authViewModel: AuthViewModel
    private val activity = this@BusinessQuestionsActivity
    private lateinit var preferenceManager : PreferenceManager
    private var selectedBusinessId : String = ""
    private var selectedSubBusinessId : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusinessQuestionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()
    }

    private fun initUI() {


        val authRepo = AuthRepo(activity)
        authViewModel = ViewModelProvider(activity, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        preferenceManager = PreferenceManager.getInstance(activity)
        val progressBar = CustomProgressBar(activity) // 'this' refers to the context

        selectedBusinessId = preferenceManager.getString(PreferenceManager.Keys.USER_BUSINESS_ID,"").toString()
        selectedSubBusinessId = preferenceManager.getString(PreferenceManager.Keys.USER_SUB_BUSINESS_ID,"").toString()

        getQuestionsAnswerList()


        binding.nextBtn.setOnClickListener {
            if (areAllQuestionsAnswered()) {
                sentAnswers()
            } else {
                CustomSnackBar.showSnackBar(binding.root, MessageStore.pleaseAnswerAllQuestions(this))
            }
        }

        binding.backBtn.setOnClickListener {
            this.onBackPressedDispatcher.onBackPressed()
        }



        authViewModel.questionAnswerResult.observe(activity) { result ->
            if (result.status == true) {

                questionAnswerList = result.data
                val relatedQuestionAdapter = RelatedQuestionAdapter(activity, questionAnswerList!!, ::onAnswerSelected)
                binding.relatedQuestionRv.adapter = relatedQuestionAdapter

            } else {
                Toast.makeText(activity, result.message, Toast.LENGTH_SHORT).show()
            }
        }

        authViewModel.answerQuestionResult.observe(activity) { result ->
            if (result.status == true) {
                val intent = Intent(activity, BusinessTypeMediaActivity::class.java)
                startActivity(intent)
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

    private fun sentAnswers() {
        val answerJson = printSelectedAnswers()
        println("ASdfhasdjkghasj   answerJson ${answerJson}")
        val requestBody = answerJson?.toRequestBody("application/json".toMediaTypeOrNull())
        authViewModel.answerQuestion(requestBody!!)
    }

    private fun getQuestionsAnswerList() {
        authViewModel.getQuestionAnswer(selectedBusinessId,selectedSubBusinessId)
    }



    private fun areAllQuestionsAnswered(): Boolean {
        return questionAnswerList?.all { selectedAnswers.containsKey(it.id) } ?: false
    }

    private fun printSelectedAnswers(): String? {
        val gson = Gson()
        val json = gson.toJson(selectedAnswers.map { (question, answer) ->
            mapOf("questionID" to question, "answer" to answer)
        })
        return json
    }

    private fun onAnswerSelected(questionId: String, answer: String) {
        selectedAnswers[questionId] = answer
    }
}
