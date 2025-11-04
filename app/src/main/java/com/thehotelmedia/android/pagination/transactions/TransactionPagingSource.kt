package com.thehotelmedia.android.pagination.transactions

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.thehotelmedia.android.modals.transactions.TransactionData
import com.thehotelmedia.android.repository.IndividualRepo
import retrofit2.HttpException
import java.io.IOException



class TransactionPagingSource(
    private val repository: IndividualRepo
) : PagingSource<Int, TransactionData>() {
    private val tag = "PAGING_ACTIVE_FOLLOWER"
    private var maxPageLimit: Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TransactionData> {

        try {

            val nextPageNumber = params.key ?: 1

            if (maxPageLimit != null && nextPageNumber > maxPageLimit!!) {

                return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)

            }

            val response = repository
                .getTransactionData( nextPageNumber, 20)

            Log.d(tag + "KEY", nextPageNumber.toString())
            return if (response.isSuccessful) {
                Log.d(tag, "Response code: ${response.code()}")
                println("sdajfksajfka    ${response.body()}")

                val services = response.body()?.transactionData ?: emptyList()

                val nextKey = if (services.isEmpty()) null else nextPageNumber + 1

                if (maxPageLimit == null) {
                    maxPageLimit = response.body()?.totalPages
                }
                Log.d(tag, "Response: $nextKey")
                LoadResult.Page(services, null, nextKey)

            } else {
                Log.d(tag, "HTTP error: ${response.code()}")
                LoadResult.Error(Throwable("$tag error : ${response.code()}"))
            }


        } catch (e: IOException) {
            Log.e(tag, "IO Exception: ${e.message}")
            return LoadResult.Error(e)
        } catch (e: HttpException) {
            Log.e(tag, "HTTP Exception: ${e.message}")
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, TransactionData>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}