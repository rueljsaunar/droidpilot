plugins {
    alias(libs.plugins.android.library)
}
android {
    namespace = "com.droidpilot.backend.accessibility"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(project(":core:automation"))
    testImplementation(libs.junit)
}
