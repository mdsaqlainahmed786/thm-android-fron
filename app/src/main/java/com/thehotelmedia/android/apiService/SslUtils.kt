package com.thehotelmedia.android.apiService


import android.content.Context
import com.thehotelmedia.android.BuildConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object SslUtils {

    /**
     * Returns an OkHttpClient.Builder based on the build type (debug or release).
     * For release builds, SSL is configured with a custom certificate.
     */
    fun getOkHttpClientBuilder(context: Context): OkHttpClient.Builder {
        return if (BuildConfig.DEBUG) {
            OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)

//        return try {
//                // Load the certificate from resources
//                val certificateInputStream: InputStream = context.resources.openRawResource(R.raw.certificate)
//                val certificateFactory = CertificateFactory.getInstance("X.509")
//                val certificate = certificateFactory.generateCertificate(certificateInputStream)
//
//                // Create a KeyStore and add the certificate to it
//                val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
//                    load(null, null)
//                    setCertificateEntry("cloudflare_origin", certificate)
//                }
//
//                // Initialize TrustManagerFactory with the KeyStore
//                val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
//                    init(keyStore)
//                }
//
//                // Get the X509TrustManager from the TrustManagerFactory
//                val x509TrustManager = trustManagerFactory.trustManagers
//                    .filterIsInstance<X509TrustManager>()
//                    .firstOrNull()
//                    ?: throw IllegalStateException("No X509TrustManager found")
//
//                // Initialize SSLContext with the X509TrustManager
//                val sslContext = SSLContext.getInstance("TLS").apply {
//                    init(null, arrayOf(x509TrustManager), null)
//                }
//
//                // Create an OkHttpClient with SSL configuration
//                OkHttpClient.Builder()
//                    .sslSocketFactory(sslContext.socketFactory, x509TrustManager)
//                    .connectTimeout(1, TimeUnit.MINUTES)
//                    .readTimeout(60, TimeUnit.SECONDS)
//                    .writeTimeout(30, TimeUnit.SECONDS)
//            } catch (e: Exception) {
//                throw RuntimeException("Failed to configure SSL", e)
//            }


        } else {

            OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)

        }
    }
}