import org.gradle.api.tasks.Copy

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val catalogueAssetsDir = layout.buildDirectory.dir("generated/catalogueAssets")
val prepareCatalogueAsset = tasks.register<Copy>("prepareCatalogueAsset") {
    from(rootProject.layout.projectDirectory.file("data/economics-reading-catalogue.json"))
    into(catalogueAssetsDir)
}

android {
    namespace = "com.wp73.econbooklist"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wp73.econbooklist"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    sourceSets.getByName("main").assets.srcDir(catalogueAssetsDir.get().asFile)
}

tasks.named("preBuild").configure {
    dependsOn(prepareCatalogueAsset)
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
