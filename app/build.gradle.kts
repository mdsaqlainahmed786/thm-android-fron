plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    id ("kotlin-parcelize")
}

android {

    namespace = "com.thehotelmedia.android"
    compileSdk = 35

    defaultConfig {
        renderscriptTargetApi = 34
        renderscriptNdkModeEnabled = true
        applicationId = "com.thehotelmedia.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 133
        versionName = "1.2.3"
        resourceConfigurations.addAll(listOf("en", "hi","gu","kn","mr","te")) // List of supported languages
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    bundle {
        language {
            enableSplit = false // Ensures all language resources are included
        }
    }


    signingConfigs {
        create("release") {
            storeFile = file("../KeyStore.jks")
            storePassword = "Appcrunk"
            keyAlias = "key0"
            keyPassword = "Appcrunk"
        }
    }

    buildTypes {

        debug {
            buildConfigField("String","ADMIN_DOMAIN","\"https://admin.thehotelmedia.com\"")
            buildConfigField("String","SHARE_DEEP_LINK_HOST","\"https://thehotelmedia.com\"")
            buildConfigField("String","DOMAIN","\"https://api.thehotelmedia.com\"")
            buildConfigField("String","BASE_URL","\"https://api.thehotelmedia.com/api/v1/\"")
            buildConfigField("String", "RAZORPAY_API_KEY", "\"rzp_test_IXF6sTTP8dPZXN\"")
            buildConfigField("String", "MAPS_API_KEY", "\"AIzaSyCoDc7bhRp94TgvpG00jafqzyqo2ljh2IM\"")
            manifestPlaceholders["razorpayApiKey"] = "rzp_test_IXF6sTTP8dPZXN"
            manifestPlaceholders["mapsApiKey"] = "AIzaSyCoDc7bhRp94TgvpG00jafqzyqo2ljh2IM"
            manifestPlaceholders["DEEP_LINK_HOST"] = "thehotelmedia.com"
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        } 

        release {
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String","ADMIN_DOMAIN","\"https://admin.thehotelmedia.com\"")
            buildConfigField("String","DOMAIN","\"https://api.thehotelmedia.com\"")
            buildConfigField("String","SHARE_DEEP_LINK_HOST","\"https://thehotelmedia.com\"")
            buildConfigField("String","BASE_URL","\"https://api.thehotelmedia.com/api/v1/\"")
            buildConfigField("String", "RAZORPAY_API_KEY", "\"rzp_live_oItyf902ER4IXW\"")
            buildConfigField("String", "MAPS_API_KEY", "\"AIzaSyCoDc7bhRp94TgvpG00jafqzyqo2ljh2IM\"")
            manifestPlaceholders["razorpayApiKey"] = "rzp_live_oItyf902ER4IXW"
            manifestPlaceholders["mapsApiKey"] = "AIzaSyCoDc7bhRp94TgvpG00jafqzyqo2ljh2IM"
            manifestPlaceholders["DEEP_LINK_HOST"] = "thehotelmedia.com"
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        dataBinding = true
        viewBinding = true
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)
    implementation("androidx.fragment:fragment-ktx:1.8.5")  // or latest version
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.media3.common)

    implementation("androidx.media3:media3-exoplayer:1.3.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.3.0")
    implementation("androidx.media3:media3-ui:1.3.0")
    // Removed android-video-trimmer and ffmpeg-kit; using a lightweight pass-through flow instead

    implementation(libs.androidx.runtime.saved.instance.state)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.emoji2.emojipicker)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.annotations)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


//  sdp
    implementation (libs.sdp.android)

//  glide
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4) // or the latest version
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1") // for annotation processing
    implementation ("com.github.bumptech.glide:okhttp3-integration:4.15.1") // Adjust the version as necessary

    implementation (libs.blurry)



//  coroutines
    implementation (libs.kotlinx.coroutines.core)
    implementation (libs.kotlinx.coroutines.android)


//  dotIndicator
    implementation(libs.dotsindicator)

//  imageCropper
    implementation (libs.ucrop)

//  For recyclerView
    implementation(libs.google.flexbox)

    implementation ("androidx.viewpager2:viewpager2:1.1.0")
    implementation ("com.github.Dimezis:BlurView:version-2.0.3")


    implementation ("com.hbb20:ccp:2.5.0")

    //  retrofit
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.okhttp3:okhttp:4.10.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.5")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okio:okio:3.4.0")

    //VideoTrimmer
    //PhotoEditor
    implementation ("com.burhanrashid52:photoeditor:2.0.0")

    //for graph
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

    //  retrofit
    implementation (libs.retrofit2.retrofit)
    implementation (libs.okhttp3.okhttp)
    implementation (libs.okhttp3.logging.interceptor)
    implementation (libs.squareup.converter.gson)
    implementation (libs.squareup.okio)

//  viewModelAndLiveData
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")

//  shimmerEffect
    implementation ("com.facebook.shimmer:shimmer:0.5.0")

//  razorPay
    implementation ("com.razorpay:checkout:1.6.33")

    //  googleMaps
    implementation ("com.google.android.gms:play-services-maps:19.0.0")
    implementation ("com.google.android.gms:play-services-location:21.3.0")
    implementation ("com.google.android.gms:play-services-places:17.1.0")
    implementation ("com.google.android.libraries.places:places:4.1.0")


    implementation ("com.github.Dhaval2404:ColorPicker:2.3")
    implementation ("com.google.code.gson:gson:2.10.1")



    //  For Firebase
    implementation(libs.firebase.auth)
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-core:21.1.1")
    implementation ("com.google.android.gms:play-services-auth:21.2.0")
    implementation ("com.google.firebase:firebase-messaging:24.1.0")


    implementation ("com.airbnb.android:lottie:6.0.0")

    //  pagination
    implementation ("androidx.paging:paging-runtime-ktx:3.3.4")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("io.coil-kt:coil:2.3.0")

    implementation ("io.socket:socket.io-client:2.1.0")
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest) // Use the latest version



    implementation ("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation ("com.google.android.exoplayer:exoplayer-hls:2.19.1")
    implementation ("com.google.android.exoplayer:exoplayer-ui:2.19.1")  // For UI components like SimpleExoPlayerView


    // This dependency is downloaded from the Google’s Maven repository.
    // So, make sure you also include that repository in your project's build.gradle file.
    implementation("com.google.android.play:app-update:2.1.0")

    // For Kotlin users also import the Kotlin extensions library for Play In-App Update:
    implementation("com.google.android.play:app-update-ktx:2.1.0")


    implementation("com.android.billingclient:billing:7.1.1")

    // PDF viewing is handled via WebView (built-in, no external dependency needed)


    implementation ("androidx.work:work-runtime-ktx:2.9.0")
    implementation ("androidx.work:work-runtime:2.9.0")



}