// neuralstrand — the NeuralStrand primitive as an Android library module.
// Consumable by any future app (Launcher, Voice, AI, ...) without those
// apps needing to know anything about HELIX Core; this module has zero
// dependency on Core today (see PlaceholderHelixState.kt) by deliberate
// decision, not oversight.

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.helix.ui.neuralstrand"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)

    // ---------------------------------------------------------------------
    // NOT YET ADDED: a dependency on HELIX Core (helix-platform).
    //
    // Per the explicit build decision for this pass, NeuralStrand is wired
    // to PlaceholderHelixState (this module, local, clearly labeled) rather
    // than to real Core events. HELIX Core is also not currently published
    // to any artifact repository this module could resolve against. Adding
    // a dependency line here without either of those being true would
    // either fail to resolve or silently pretend an integration exists that
    // doesn't. When Core is published and the event-wiring decision is
    // made, this comment is where that dependency goes.
    // ---------------------------------------------------------------------

    testImplementation(libs.junit.jupiter)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
