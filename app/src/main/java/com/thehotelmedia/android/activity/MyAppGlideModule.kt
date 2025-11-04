import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.thehotelmedia.android.R
import okhttp3.OkHttpClient
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLContext
import java.io.InputStream
import java.security.cert.CertificateFactory
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.io.InputStreamReader

@GlideModule
class MyAppGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val memoryCacheSizeBytes = 1024 * 1024 * 100 // 100 MB
        builder.setMemoryCache(LruResourceCache(memoryCacheSizeBytes.toLong()))

        val diskCacheSizeBytes = 1024 * 1024 * 200 // 200 MB
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, diskCacheSizeBytes.toLong()))
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val sslSocketFactory = createSslSocketFactory(context)
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(sslSocketFactory, getTrustManager(context))
            .hostnameVerifier { _, _ -> true } // Disable hostname verification for testing
            .build()

        // Register OkHttpUrlLoader with Glide
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(okHttpClient))
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }

    // Create an SSLSocketFactory with a custom TrustManager
    private fun createSslSocketFactory(context: Context): javax.net.ssl.SSLSocketFactory {
        val trustManager = getTrustManager(context)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), java.security.SecureRandom())
        return sslContext.socketFactory
    }

    // Get a TrustManager that trusts the certificate you added to the res/raw folder
    private fun getTrustManager(context: Context): X509TrustManager {
        val cf = CertificateFactory.getInstance("X.509")
        val certInputStream: InputStream = context.resources.openRawResource(R.raw.certificate) // Your cert file in res/raw/
        val cert = cf.generateCertificate(certInputStream) as X509Certificate

        // Create a KeyStore and add the certificate
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setCertificateEntry("certificate", cert)

        // Create a TrustManager that trusts the certificate
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)

        val trustManagers = trustManagerFactory.trustManagers
        return trustManagers[0] as X509TrustManager
    }
}
