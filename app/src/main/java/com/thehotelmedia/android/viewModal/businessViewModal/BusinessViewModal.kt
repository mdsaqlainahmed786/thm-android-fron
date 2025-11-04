package com.thehotelmedia.android.viewModal.businessViewModal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.thehotelmedia.android.repository.BusinessRepo

class BusinessViewModal (private val businessRepo: BusinessRepo): ViewModel(){
    private val toastMessageLiveData = MutableLiveData<String>()
    val toast: LiveData<String> = toastMessageLiveData
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    private val tag = "BUSINESS_VIEW_MODEL"



}