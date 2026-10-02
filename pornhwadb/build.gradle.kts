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
        versionCode = 2
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
    // Everything is compileOnly on purpose. Mihon already ships all of this, and an
    // extension that packaged its own copies shipped a second okhttp/okio into the host
    // process: class loading then failed and the extension registered zero sources.
    // Keiyoushi's extensions are ~20KB for this reason.
    compileOnly("com.github.mihonapp:tachiyomix:1.6.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    compileOnly("com.squareup.okhttp3:okhttp:5.4.0")
}