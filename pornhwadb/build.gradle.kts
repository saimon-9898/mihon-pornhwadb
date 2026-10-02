plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "eu.kanade.tachiyomi.extension.en.pornhwadb"
    compileSdk = 36

    defaultConfig {
        applicationId = "eu.kanade.tachiyomi.extension.en.pornhwadb"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    signingConfigs {
        create("repo") {
            storeFile = file("signing.jks")
            storePassword = "pornhwa2026"
            keyAlias = "pornhwadb"
            keyPassword = "pornhwa2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the committed key whose fingerprint the repo index publishes;
            // an unsigned release APK would be rejected by Mihon.
            signingConfig = signingConfigs.getByName("repo")
        }
    }
}

dependencies {
    // Provided by the Mihon host app at runtime; these are compile-time stubs only.
    compileOnly("com.github.mihonapp:tachiyomix:1.6.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
}