package com.thehotelmedia.android.viewModal.authViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehotelmedia.android.customClasses.Constants.N_A
import com.thehotelmedia.android.modals.Business.businessType.BusinessTypeModal
import com.thehotelmedia.android.modals.Business.subBusinessType.SubBusinessTypeModal
import com.thehotelmedia.android.modals.authentication.business.BusinessSubscriptionPlans.BusinessSubscriptionPlansModal
import com.thehotelmedia.android.modals.authentication.business.answers.AnswerModal
import com.thehotelmedia.android.modals.authentication.business.businessSignUp.BusinessSignUpModal
import com.thehotelmedia.android.modals.authentication.business.buySubscription.BuySubscriptionModal
import com.thehotelmedia.android.modals.authentication.business.questionAnswer.QuestionAnswerModal
import com.thehotelmedia.android.modals.authentication.business.subscriptionCheckOut.SubscriptionCheckOutModal
import com.thehotelmedia.android.modals.authentication.business.supportingDocuments.DocumentsModal
import com.thehotelmedia.android.modals.authentication.business.uploadPropertyPictureModal.UploadPropertyPictureModal
import com.thehotelmedia.android.modals.authentication.business.verifyEmail.BusinessVerifyEmailModal
import com.thehotelmedia.android.modals.authentication.forgetPassword.forgetPassword.ForgetPasswordModal
import com.thehotelmedia.android.modals.authentication.individual.signUp.IndividualSignUpModal
import com.thehotelmedia.android.modals.authentication.individual.uploadProfilePic.IndividualProfilePicModal
import com.thehotelmedia.android.modals.authentication.individual.verifyEmail.IndividualVerifyEmailModal
import com.thehotelmedia.android.modals.authentication.logOut.LogOutModal
import com.thehotelmedia.android.modals.authentication.login.LoginModal
import com.thehotelmedia.android.modals.getProfessions.GetProfessionModal
import com.thehotelmedia.android.repository.AuthRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody

class AuthViewModel (private val authRepo: AuthRepo): ViewModel(){
    private val tag = "AUTH_VIEW_MODEL"

    private val toastMessageLiveData = MutableLiveData<String>()
    val toast: LiveData<String> = toastMessageLiveData

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _individualSignUpResult = MutableLiveData<IndividualSignUpModal>()
    val individualSignUpResult: LiveData<IndividualSignUpModal> = _individualSignUpResult

    fun individualSignUp(accountType: String, email: String, dialCode: String, phoneNumber: String, fullName: String, password: String, profession: String, lat: Double, lng: Double,language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.individualSignUp(accountType, email,dialCode,phoneNumber,fullName,password,profession,lat,lng,language)
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _individualSignUpResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    Log.wtf(tag + "ELSE", response.toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }



    private val _individualVerifyEmailResult = MutableLiveData<IndividualVerifyEmailModal>()
    val individualVerifyEmailResult: LiveData<IndividualVerifyEmailModal> = _individualVerifyEmailResult

    fun individualVerifyEmail(email: String, otp: String, deviceID: String, notificationToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.individualVerifyEmail(email, otp,deviceID,notificationToken)
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _individualVerifyEmailResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //UpdateIndividualProfilePic
    private val _updateIndividualProfilePicResult = MutableLiveData<IndividualProfilePicModal>()
    val updateIndividualProfilePicResult: LiveData<IndividualProfilePicModal> = _updateIndividualProfilePicResult
    fun updateIndividualProfilePic( profilePic: MultipartBody.Part) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.updateIndividualProfilePic(profilePic)
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _updateIndividualProfilePicResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }




    private val _termsAndConditionsResult = MutableLiveData<IndividualVerifyEmailModal>()
    val termsAndConditionsResult: LiveData<IndividualVerifyEmailModal> = _termsAndConditionsResult

    fun termsAndConditions(acceptedTerms: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.termsAndConditions(acceptedTerms)
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _termsAndConditionsResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //Login
    private val _loginResult = MutableLiveData<LoginModal>()
    val loginResult: LiveData<LoginModal> = _loginResult

    fun login(email: String, password: String, deviceID: String, notificationToken: String, lat: Double, lng: Double, language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.login(email, password,deviceID,notificationToken,lat, lng,language)
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _loginResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //SocialLogin
    private val _socialLoginResult = MutableLiveData<LoginModal>()
    val socialLoginResult: LiveData<LoginModal> = _socialLoginResult

    fun socialLogin(socialType: String, token: String, dialCode: String, phoneNumber: String, deviceID: String, notificationToken: String, lat: Double, lng: Double, language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.socialLogin(socialType, token,dialCode,phoneNumber,deviceID,notificationToken,lat,lng,language)
                println("asdjlkdsajkha   $response")
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _socialLoginResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    fun verifyOtpLogin(dialCode: String, phoneNumber: String, deviceID: String, notificationToken: String, lat: Double, lng: Double, language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.verifyOtpLogin(dialCode, phoneNumber, deviceID, notificationToken, lat, lng, language)
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _socialLoginResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //ReSend-Otp
    private val _reSendOtpResult = MutableLiveData<LoginModal>()
    val reSendOtpResult: LiveData<LoginModal> = _reSendOtpResult

    fun reSendOtp(email: String,type: String,) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.reSendOtp(email,type)
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _reSendOtpResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    private val _businessVerifyEmailResult = MutableLiveData<BusinessVerifyEmailModal>()
    val businessVerifyEmailResult: LiveData<BusinessVerifyEmailModal> = _businessVerifyEmailResult

    fun businessVerifyEmail(email: String, otp: String, deviceID: String, notificationToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.businessVerifyEmail(email, otp,deviceID,notificationToken)
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _businessVerifyEmailResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    private val _businessTypeResult = MutableLiveData<BusinessTypeModal>()
    val businessTypeResult: LiveData<BusinessTypeModal> = _businessTypeResult
    fun getBusinessType() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.getBusinessType()
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _businessTypeResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    private val _subBusinessResult = MutableLiveData<SubBusinessTypeModal>()
    val subBusinessResult: LiveData<SubBusinessTypeModal> = _subBusinessResult
    fun getSubBusinessType(businessId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.getSubBusinessType(businessId)
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _subBusinessResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    private val _professionResult = MutableLiveData<GetProfessionModal>()
    val professionResult: LiveData<GetProfessionModal> = _professionResult
    fun getProfession() {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = authRepo.getProfession()
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _professionResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
//                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
//                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
//                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }




    private val _businessSignUpResult = MutableLiveData<BusinessSignUpModal>()
    val businessSignUpResult: LiveData<BusinessSignUpModal> = _businessSignUpResult
    fun businessSignUp(email: String, fullName: String, password: String, dialCode: String, phoneNumber: String, businessName: String, businessEmail: String,
                       businessDialCode: String, businessPhoneNumber: String, businessType: String, businessSubType: String, businessDescription: String,
                       businessWebsite: String, gstn: String, street: String, city: String, state: String, country: String, zipCode: String, lat: String, lng: String,  accountType: String, placeID: String,) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.businessSignUp(email,fullName, password, dialCode, phoneNumber, businessName, businessEmail, businessDialCode,
                    businessPhoneNumber, businessType, businessSubType, businessDescription, businessWebsite, gstn, street, city, state, country, zipCode, lat, lng,accountType,placeID)
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _businessSignUpResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }
            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    private val _questionAnswerResult = MutableLiveData<QuestionAnswerModal>()
    val questionAnswerResult: LiveData<QuestionAnswerModal> = _questionAnswerResult
    fun getQuestionAnswer(businessTypeID: String, businessSubtypeID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.getQuestionAnswer(businessTypeID,businessSubtypeID)
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _questionAnswerResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }
            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //MaterialRequirementForms
    private val _answerQuestionResult = MutableLiveData<AnswerModal>()
    val answerQuestionResult: LiveData<AnswerModal> = _answerQuestionResult
    fun answerQuestion(requestBody: RequestBody) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.answerQuestion(requestBody)

                println("ASdfhasdjkghasj  response  ${response}")
                println("ASdfhasdjkghasj   response.body() ${response.body()}")
                println("ASdfhasdjkghasj   requestBody ${requestBody}")
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message.toString())
                    _answerQuestionResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }
            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //SupportingDocuments
    private val _supportingDocumentsResult = MutableLiveData<DocumentsModal>()
    val supportingDocumentsResult: LiveData<DocumentsModal> = _supportingDocumentsResult
    fun supportingDocuments( businessRegistration: MultipartBody.Part?,addressProof: MultipartBody.Part?) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.supportingDocuments(businessRegistration,addressProof)
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _supportingDocumentsResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    private val _getSubscriptionPlansResult = MutableLiveData<BusinessSubscriptionPlansModal>()
    val getSubscriptionPlansResult: LiveData<BusinessSubscriptionPlansModal> = _getSubscriptionPlansResult
    fun getSubscriptionPlans() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.getSubscriptionPlans()
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _getSubscriptionPlansResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    private val _subscriptionCheckOutResult = MutableLiveData<SubscriptionCheckOutModal>()
    val subscriptionCheckOutResult: LiveData<SubscriptionCheckOutModal> = _subscriptionCheckOutResult
    fun subscriptionCheckOut(subscriptionPlanID: String,promoCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.subscriptionCheckOut(subscriptionPlanID,promoCode)
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _subscriptionCheckOutResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    private val _buySubscriptionResult = MutableLiveData<BuySubscriptionModal>()
    val buySubscriptionResult: LiveData<BuySubscriptionModal> = _buySubscriptionResult
    fun buySubscription(orderID: String,paymentID: String,signature: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.buySubscription(orderID,paymentID,signature)
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _buySubscriptionResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    private val _subscriptionVerifyByGoogleResult = MutableLiveData<BuySubscriptionModal>()
    val subscriptionVerifyByGoogleResult: LiveData<BuySubscriptionModal> = _subscriptionVerifyByGoogleResult
    fun subscriptionVerifyByGoogle(purchaseToken: String,subscriptionId: String,orderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.subscriptionVerifyByGoogle(purchaseToken,subscriptionId,orderId)
                println("afjlksajfkjasdkl   response $response")
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _subscriptionVerifyByGoogleResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())

                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //LogOut
    private val _logOutResult = MutableLiveData<LogOutModal>()
    val logOutResult: LiveData<LogOutModal> = _logOutResult
    fun logOut() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.logOut()
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _logOutResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }



    private val _forgotPasswordResult = MutableLiveData<ForgetPasswordModal>()
    val forgotPasswordResult: LiveData<ForgetPasswordModal> = _forgotPasswordResult
    fun forgotPassword(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.forgotPassword(email)
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _forgotPasswordResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    private val _verifyForgotPasswordResult = MutableLiveData<ForgetPasswordModal>()
    val verifyForgotPasswordResult: LiveData<ForgetPasswordModal> = _verifyForgotPasswordResult
    fun verifyForgotPassword(email: String,otp: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.verifyForgotPassword(email,otp)
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _verifyForgotPasswordResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    private val _changePasswordResult = MutableLiveData<ForgetPasswordModal>()
    val changePasswordResult: LiveData<ForgetPasswordModal> = _changePasswordResult
    fun changePassword(password: String,resetToken: String,email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.changePassword(password,resetToken,email)
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _changePasswordResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


// Edit Profession
    private val _editProfessionResult = MutableLiveData<IndividualVerifyEmailModal>()
    val editProfessionResult: LiveData<IndividualVerifyEmailModal> = _editProfessionResult

    fun editProfession(profession: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.editProfession(profession)
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _editProfessionResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }




    //UploadPropertyPicture
    private val _uploadPropertyPictureResult = MutableLiveData<UploadPropertyPictureModal>()
    val uploadPropertyPictureResult: LiveData<UploadPropertyPictureModal> = _uploadPropertyPictureResult
    fun uploadPropertyPicture(imageList: List<MultipartBody.Part>) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.uploadPropertyPicture(imageList)
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _uploadPropertyPictureResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //DeleteAccount
    private val _deleteAccountResult = MutableLiveData<LogOutModal>()
    val deleteAccountResult: LiveData<LogOutModal> = _deleteAccountResult
    fun deleteAccount() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.deleteAccount()
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _deleteAccountResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //DeactivateAccount
    private val _deactivateAccountResult = MutableLiveData<LogOutModal>()
    val deactivateAccountResult: LiveData<LogOutModal> = _deactivateAccountResult
    fun deactivateAccount() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = authRepo.deactivateAccount()
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _deactivateAccountResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }



}