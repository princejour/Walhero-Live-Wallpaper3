import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.walhero.livewallpaper"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/walhero-release.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")?.takeIf { it.isNotEmpty() } ?: "walheropassword"
      keyAlias = System.getenv("KEY_ALIAS")?.takeIf { it.isNotEmpty() } ?: "walhero"
      keyPassword = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotEmpty() } ?: "walheropassword"
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices {
  missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN
}


// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.play.services.ads)
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  // implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("generateReleaseKeystore") {
  doLast {
    val keystoreFile = file("${rootDir}/walhero-release.jks")
    if (!keystoreFile.exists()) {
      println("Generating release keystore at: ${keystoreFile.absolutePath}")
      val process = ProcessBuilder(
        "keytool", "-genkey", "-v",
        "-keystore", keystoreFile.absolutePath,
        "-storepass", "walheropassword",
        "-alias", "walhero",
        "-keypass", "walheropassword",
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-dname", "CN=Walhero, O=Walhero, C=US"
      ).redirectErrorStream(true).start()
      val output = process.inputStream.bufferedReader().use { it.readText() }
      val exitCode = process.waitFor()
      println("Keytool Output:\n$output")
      println("Keytool Exit Code: $exitCode")
    } else {
      println("Keystore already exists at: ${keystoreFile.absolutePath}")
    }
  }
}

tasks.register("printKeystoreDetails") {
  doLast {
    val keystoreFile = file("${rootDir}/walhero-release.jks")
    if (keystoreFile.exists()) {
      println("--- Keystore Details ---")
      println("Keystore Path: ${keystoreFile.absolutePath}")
      val process = ProcessBuilder(
        "keytool", "-list", "-v",
        "-keystore", keystoreFile.absolutePath,
        "-storepass", "walheropassword",
        "-alias", "walhero"
      ).redirectErrorStream(true).start()
      val output = process.inputStream.bufferedReader().use { it.readText() }
      val exitCode = process.waitFor()
      println("Keytool Output:\n$output")
      println("Keytool Exit Code: $exitCode")
    } else {
      println("Error: Keystore file does not exist at ${keystoreFile.absolutePath}")
    }
  }
}

tasks.register("copyReleaseApk") {
  doLast {
    val releaseApk = file("build/outputs/apk/release/app-release.apk")
    if (releaseApk.exists()) {
      val destFile = file("${rootDir}/app-release-signed.apk")
      releaseApk.copyTo(destFile, overwrite = true)
      println("Copied signed release APK to: ${destFile.absolutePath}")
    } else {
      // Let's search for any APK in build/outputs/ to make sure we find it
      val outputsDir = file("build/outputs")
      if (outputsDir.exists()) {
        outputsDir.walkTopDown().forEach { f ->
          if (f.extension == "apk") {
            println("Found APK at: ${f.absolutePath}")
            val destFile = file("${rootDir}/app-release-signed.apk")
            f.copyTo(destFile, overwrite = true)
            println("Copied ${f.name} to: ${destFile.absolutePath}")
            return@doLast
          }
        }
      }
      println("Error: No APK found in build/outputs/")
    }
  }
}

