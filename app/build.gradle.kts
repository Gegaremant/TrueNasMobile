import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

// ── Secrets: root .env ─────────────────────────────────────────────────────
// Every secret this build needs lives in the git-ignored .env at the repo
// root, so there is exactly one place to rotate them. Resolution order for
// each key (first hit wins):
//   1. real environment variable  — what CI injects
//   2. Gradle property            — ~/.gradle/gradle.properties (never committed)
//   3. root .env                  — local development
// A key that is still missing is simply absent; nothing is ever printed.
val rootEnvFile = rootProject.file(".env")
val rootEnv: Map<String, String> =
    if (rootEnvFile.isFile) {
        rootEnvFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
            .associate { line ->
                val idx = line.indexOf('=')
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                    .removeSurrounding("\"")
                    .removeSurrounding("'")
                key to value
            }
    } else {
        emptyMap()
    }

fun secret(name: String): String? =
    System.getenv(name)
        ?: providers.gradleProperty(name).orNull
        ?: rootEnv[name]

// ── Release signing ────────────────────────────────────────────────────────
// Nothing here is hardcoded and nothing is committed: either point the build
// at a keystore with KEYSTORE_PATH, or hand it over as KEYSTORE_BASE64
// (the usual CI shape). KEYSTORE_ALIAS + KEYSTORE_PASSWORD are always needed.
// When they are absent the release build still runs, but loudly warns and
// produces an UNSIGNED artifact rather than silently signing with a debug key.
// NOTE: these are deliberately NOT named keyAlias / storePassword /
// keyPassword. Inside `signingConfigs { create("release") { … } }` the
// unqualified names resolve to the SigningConfig's own properties, so
// `keyAlias = keyAlias` would silently assign the (still null) property to
// itself and the build would fail with "missing required property keyAlias".
val releaseKeystorePath: String? = secret("KEYSTORE_PATH")
val releaseKeystorePassword: String? = secret("KEYSTORE_PASSWORD")
val releaseKeyAlias: String? = secret("KEYSTORE_ALIAS")
val releaseKeystoreBase64: String? = secret("KEYSTORE_BASE64")

val hasReleaseSigning = releaseKeystorePassword != null && releaseKeyAlias != null &&
    (releaseKeystorePath != null || releaseKeystoreBase64 != null)

// Target for a keystore handed to us as base64 (CI). Kept in the build dir,
// which is git-ignored, so the keystore never lands in the repository.
val decodedKeystoreFile: File =
    rootProject.layout.buildDirectory.get().asFile.resolve("release-keystore.jks")

if (hasReleaseSigning && releaseKeystoreBase64 != null) {
    val decoded = decodedKeystoreFile
    if (!decoded.exists() || decoded.length() == 0L) {
        decoded.parentFile.mkdirs()
        decoded.writeBytes(Base64.getMimeDecoder().decode(releaseKeystoreBase64.trim()))
    }
}

val resolvedKeystoreFile: File? = when {
    releaseKeystorePath != null -> file(releaseKeystorePath)
    hasReleaseSigning && releaseKeystoreBase64 != null -> decodedKeystoreFile
    else -> null
}

// True when this invocation asks for an Android App Bundle rather than APKs.
// An AAB must not be combined with a manual ABI split - see `splits` below.
val buildingBundle: Boolean = gradle.startParameter.taskNames.any { task ->
    task.substringAfterLast(':').startsWith("bundle", ignoreCase = true) ||
        task.contains("Bundle", ignoreCase = true)
}

android {
    namespace = "com.gegaremant.truenasmobile"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
        targetSdk = 37
        versionCode = 10006
        versionName = "1.0.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    android.buildFeatures.buildConfig = true
    flavorDimensions.add("store")

    productFlavors {
        create("playstore") {
            dimension = "store"
            applicationId = "com.gegaremant.truenasmobile.app"
            buildConfigField("Boolean", "IS_PLAYSTORE_BUILD", "true")
        }
        create("github") {
            dimension = "store"
            applicationId = "com.gegaremant.truenasmobile"
            buildConfigField("Boolean", "IS_PLAYSTORE_BUILD", "false")
        }
    }

    signingConfigs {
        if (hasReleaseSigning && resolvedKeystoreFile != null) {
            create("release") {
                storeFile = resolvedKeystoreFile
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeystorePassword
                // AGP drops v1/v2 on its own once minSdk >= 24 (API 24 reads
                // v3 signatures), so a v3-only APK is still installable on the
                // Android 10 floor. Nothing to pin here.
                // v4 is off because it needs a separate .idsig sidecar file.
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "WARNING: no release keystore configured " +
                        "(KEYSTORE_PATH or KEYSTORE_BASE64 + KEYSTORE_ALIAS + KEYSTORE_PASSWORD). " +
                        "Release artifacts will be UNSIGNED - do not publish these."
                )
            }
        }
    }
    splits {
        abi {
            // The Play Store delivers an AAB and splits ABIs itself. AGP
            // refuses to build a bundle while a manual ABI split is active
            // ("Multiple shrunk-resources files found"), so the split is only
            // switched on for APK builds. Because of that, build the two
            // artifacts with separate commands:
            //     ./gradlew assembleGithubRelease     -> per-ABI APKs
            //     ./gradlew bundlePlaystoreRelease    -> one AAB
            isEnable = !buildingBundle
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }
    buildFeatures {
        compose = true
    }
    ksp {
        arg("appfunctions:aggregateAppFunctions", "true")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        // `context.getString(...)` inside a composable re-reads the resource on
        // every recomposition instead of going through stringResource. It is a
        // performance and recomposition-churn hint, not a crash - 88 call sites
        // across ~30 screens, all predating the 1.0.3 work.
        //
        // Kept visible as a warning rather than disabled, and rather than being
        // refactored in the same commit as a crash fix: 88 blind edits to
        // screens nobody can run here is how the next regression gets shipped.
        // Tracked as TODO 32.
        warning += "LocalContextGetResourceValueCall"
        // NewApi stays fatal. It is the check that would have caught the
        // dynamicLightColorScheme crash on Android 10 and 11 the moment minSdk
        // moved to 29 - and it is fatal now because lint actually runs in CI.
        abortOnError = true
        checkDependencies = false
    }
    testOptions {
        unitTests {
            // Robolectric needs the merged resources and manifest to inflate
            // anything, and it needs the resources of the variant under test
            // rather than a stripped jar.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

// The artifact checks in ReleaseArtifactTest only mean anything against a built
// APK. Ordering them after the build - when both tasks happen to be in the same
// invocation - makes `./gradlew assembleGithubRelease test` a real release
// verification, without making every local test run pay for a release build.
tasks.matching { it.name == "testGithubDebugUnitTest" }.configureEach {
    mustRunAfter("assembleGithubRelease")
}

tasks.register("verifyRelease") {
    group = "verification"
    description = "Builds the release APKs and runs the full unit test suite against them."
    // assembleGithubRelease already pulls in lintVitalGithubRelease.
    dependsOn("assembleGithubRelease", "testGithubDebugUnitTest")
}

dependencies {
    implementation(libs.androidx.appfunctions)
    ksp(libs.androidx.appfunctions.compiler)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.compose.material3.window.size.class1)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.preview)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.coil.svg)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.graphics.shapes)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.material3)
    implementation(libs.jeziellago.compose.markdown)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.lifecycle.service)
    // Local JVM tests. The source-scanning invariants need nothing, but the
    // navigation tests build real NavControllers and the Compose ones walk a
    // real composition tree - which is the only way to catch an error that
    // exists solely once composition happens.
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.lifecycle.runtime.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    implementation(libs.mpandroidchart)
    implementation(libs.moshi.kotlin)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
