package com.thehotelmedia.android.apiService

import android.content.Context
import com.thehotelmedia.android.BuildConfig
import com.thehotelmedia.android.interFaces.Application
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object Retrofit {

    private const val BASE_URL = BuildConfig.BASE_URL


    private val googleApiServices by lazy {
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    val googleApiService: Application by lazy {
        googleApiServices.create(Application::class.java)
    }


    private const val WEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/"
    val weatherApiService: Application by lazy {
        Retrofit.Builder()
            .baseUrl(WEATHER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Application::class.java)
    }





    fun apiService(context: Context): Retrofit {

        // Use the custom SSL configuration from SslUtils
        val httpClient = SslUtils.getOkHttpClientBuilder(context)
            .connectTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(context))
            .build()


//        val httpClient = OkHttpClient.Builder()
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .addInterceptor(AuthInterceptor(context))
//            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    fun authApiService(context: Context): Retrofit {

        // Use the custom SSL configuration from SslUtils
        val httpClient = SslUtils.getOkHttpClientBuilder(context)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()

//        val httpClient = OkHttpClient.Builder()
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }



}