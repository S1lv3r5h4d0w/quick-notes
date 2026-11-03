plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.manninghouse.notes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.manninghouse.notes"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // Fixed key so every new build installs as an update (keeps your notes).
    signingConfigs {
        create("fixed") {
            storeFile = file("../signing/notes.keystore")
            storePassword = "quicknotes"
            keyAlias = "notes"
            keyPassword = "quicknotes"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixed")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
