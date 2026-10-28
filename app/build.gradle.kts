plugins {
    id("com.android.application")
}

// CI passes its run number so each new APK installs over the previous one.
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

// A private release key can be supplied through the environment (see README). Without one the
// committed debug key signs the release APK, which keeps the signature stable between builds.
val releaseKeystore = System.getenv("KMBP_KEYSTORE")?.let { file(it) }?.takeIf { it.isFile }

android {
    namespace = "io.github.nguyengiathong.kymonbinhphap"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.nguyengiathong.kymonbinhphap"
        minSdk = 24
        targetSdk = 36
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
        }
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("KMBP_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KMBP_KEY_ALIAS")
                keyPassword = System.getenv("KMBP_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName(if (releaseKeystore != null) "release" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
