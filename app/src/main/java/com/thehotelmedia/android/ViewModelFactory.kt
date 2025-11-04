package com.thehotelmedia.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.repository.BusinessRepo
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel
import com.thehotelmedia.android.viewModal.businessViewModal.BusinessViewModal
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal


class ViewModelFactory(private val authRepo: AuthRepo? = null, private val individualRepo: IndividualRepo? = null, private val businessRepo: BusinessRepo? = null, ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                AuthViewModel::class.java -> {
                    if (authRepo != null) {
                        AuthViewModel(authRepo) as T
                    } else {
                        throw IllegalArgumentException(
                            "AuthRepo must be set before creating AuthViewModel.")
                    }
                }
                IndividualViewModal::class.java -> {
                    if (individualRepo != null) {
                        IndividualViewModal(individualRepo) as T
                    } else {
                        throw IllegalArgumentException(
                            "DashBoardRepo must be set before creating DashBoardViewModel.")
                    }
                }
                BusinessViewModal::class.java -> {
                    if (businessRepo != null) {
                        BusinessViewModal(businessRepo) as T
                    } else {
                        throw IllegalArgumentException(
                            "AccountRepo must be set before creating AccountViewModel.")
                    }
                }
                else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }