plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
}

subprojects {
    if (name != "detekt-rules") {
        apply(plugin = "dev.detekt")
        configure<dev.detekt.gradle.extensions.DetektExtension> {
            config.setFrom(rootProject.files("config/detekt/detekt.yml"))
            buildUponDefaultConfig = true
        }
        dependencies {
            "detektPlugins"(project(":detekt-rules"))
        }
    }
}
