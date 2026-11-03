plugins {
    alias(libs.plugins.android.library)
}
android {
    namespace = "com.droidpilot.runner"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(project(":core:automation"))
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
}
