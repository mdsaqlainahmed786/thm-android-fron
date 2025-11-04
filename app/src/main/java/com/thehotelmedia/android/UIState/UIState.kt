package com.thehotelmedia.android.UIState

enum class Status {
    SUCCESS,
    ERROR,
    LOADING
}

data class UIState<out T>(val stateStatus: Status, val stateData: T?, val stateMessage: String?) {
    companion object {
        fun <T> success(data: T?, message: String) =
            UIState(stateStatus = Status.SUCCESS, stateData = data, stateMessage = message)

        fun <T> loading(data: T? = null) =
            UIState(stateStatus = Status.LOADING, stateData = data, stateMessage = null)

        fun <T> error(data: T? = null, message: String) =
            UIState(stateStatus = Status.ERROR, stateData = data, stateMessage = message)
    }
}
