plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(commonlibs.plugins.multiplatform).apply(false)
    alias(commonlibs.plugins.kotlin.android).apply(false)
    alias(commonlibs.plugins.android.lib).apply(false)
    alias(commonlibs.plugins.android.app).apply(false)
    alias(commonlibs.plugins.kover).apply(false)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

subprojects {
    afterEvaluate {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_17.toString()
            }
        }
    }
}