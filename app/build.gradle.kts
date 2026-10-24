import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

fun releaseProperty(name: String): String? =
    providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orNull
        ?.takeIf { it.isNotBlank() }

fun releaseStoreFileOutsideRepo(path: String): File {
    val candidate = File(path)
    if (!candidate.isAbsolute) {
        throw GradleException("MYGOLFSWING_RELEASE_STORE_FILE must be an absolute path outside the repository")
    }
    val normalized = candidate.toPath().toAbsolutePath().normalize()
    val repoRoot = rootProject.projectDir.toPath().toAbsolutePath().normalize()
    if (normalized.startsWith(repoRoot)) {
        throw GradleException("MYGOLFSWING_RELEASE_STORE_FILE must point outside the repository")
    }
    return normalized.toFile()
}

val releaseVersionCode = releaseProperty("MYGOLFSWING_VERSION_CODE")?.let { raw ->
    raw.toIntOrNull()?.takeIf { it > 0 }
        ?: throw GradleException("MYGOLFSWING_VERSION_CODE must be a positive integer, got '$raw'")
} ?: 2
val releaseVersionName = releaseProperty("MYGOLFSWING_VERSION_NAME") ?: "2.5"

val releaseStoreFile = releaseProperty("MYGOLFSWING_RELEASE_STORE_FILE")
val releaseStorePassword = releaseProperty("MYGOLFSWING_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseProperty("MYGOLFSWING_RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseProperty("MYGOLFSWING_RELEASE_KEY_PASSWORD")
val releaseSigningValues = mapOf(
    "MYGOLFSWING_RELEASE_STORE_FILE" to releaseStoreFile,
    "MYGOLFSWING_RELEASE_STORE_PASSWORD" to releaseStorePassword,
    "MYGOLFSWING_RELEASE_KEY_ALIAS" to releaseKeyAlias,
    "MYGOLFSWING_RELEASE_KEY_PASSWORD" to releaseKeyPassword,
)
val hasAnyReleaseSigningValue = releaseSigningValues.values.any { !it.isNullOrBlank() }
val missingReleaseSigningValues = releaseSigningValues.filterValues { it.isNullOrBlank() }.keys
val hasReleaseSigningConfig = hasAnyReleaseSigningValue && missingReleaseSigningValues.isEmpty()

if (hasAnyReleaseSigningValue && missingReleaseSigningValues.isNotEmpty()) {
    throw GradleException(
        "Incomplete release signing config. Missing: ${missingReleaseSigningValues.joinToString()}"
    )
}

android {
    namespace = "com.mygolfswing.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.mygolfswing.app"
        minSdk = 26
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = releaseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = releaseStoreFileOutsideRepo(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
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
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/DebugProbesKt.bin"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.database)
    implementation(libs.androidx.media3.ui)
    implementation(libs.coil.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.google.play.billing)
    implementation(libs.google.mlkit.pose.detection)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.zxing.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
