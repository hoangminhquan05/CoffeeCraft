import java.util.Properties
import java.io.FileInputStream

fun getLocalProperty(key: String): String? {
    val properties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(FileInputStream(localPropertiesFile))
        return properties.getProperty(key)
    }
    return null
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)

}

android {
    namespace = "com.example.dacs3"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dacs3"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${getLocalProperty("CLOUDINARY_CLOUD_NAME") ?: ""}\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"${getLocalProperty("CLOUDINARY_API_KEY") ?: ""}\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"${getLocalProperty("CLOUDINARY_API_SECRET") ?: ""}\"")

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.recaptcha)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    implementation("com.google.firebase:firebase-analytics")

    implementation("com.google.firebase:firebase-auth:22.1.2")

    implementation("androidx.navigation:navigation-compose:2.7.6")

    implementation("androidx.compose.material:material-icons-extended:1.5.0")

//    implementation ("com.google.firebase:firebase-firestore:25.1.3")

//    implementation ("androidx.compose.material3:material3:<version>")
//    implementation ("androidx.compose.ui:ui:1.4.0")
//    implementation ("androidx.compose.material3:material3:1.0.0")
//    implementation ("androidx.compose.material:material:1.4.0")
    implementation ("io.coil-kt:coil-compose:2.4.0")

    // Image picker
//    implementation("io.github.dhaval2404:imagepicker:2.1")

    // Cloudinary SDK
    implementation("com.cloudinary:cloudinary-android:2.3.1")

    // Retrofit (optional - nếu muốn gọi Cloudinary API qua HTTP)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")

    implementation ("com.google.firebase:firebase-auth-ktx")
    implementation ("com.google.android.gms:play-services-auth:20.7.0")

    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

//    implementation ("com.google.firebase:firebase-messaging:24.1.1")
//    implementation ("androidx.work:work-runtime-ktx:2.7.1")
//    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
//
//    implementation ("com.google.auth:google-auth-library-oauth2-http:1.19.0")

    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    implementation ("androidx.compose.ui:ui-graphics:1.5.4")
    implementation ("androidx.compose.ui:ui-tooling:1.5.4")
    implementation ("com.google.accompanist:accompanist-permissions:0.32.0")

    implementation ("com.google.zxing:core:3.5.3")
}