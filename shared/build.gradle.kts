import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode.BITCODE
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("maven-publish")
    id("kover")
}

group = commonlibs.versions.library.group.get()
version = commonlibs.versions.library.version.get()

var androidTarget: String = ""

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    val android = android {
        publishLibraryVariants("release")
    }
    androidTarget = android.name
    //androidTarget = "release"
    val xcf = XCFramework()
    val iosX64 = iosX64()
    val iosArm64 = iosArm64()
    val iosSim = iosSimulatorArm64()
    configure(listOf(iosX64, iosArm64, iosSim)) {
        binaries {
            framework {
                //Any dependecy you add for ios should be added here using export()
                export(commonlibs.kotlin.stdlib)
                xcf.add(this)
            }
        }
    }

    targets.withType<KotlinNativeTarget> {
        binaries.all {
            freeCompilerArgs += listOf("-Xgc=cms")
        }
    }

    cocoapods {
        //val iosDefinitions = commonlibs.versions.ios
        val iosDefinitions = commonlibs.versions.ios
        name = iosDefinitions.basename.get()
        summary = iosDefinitions.summary.get()
        homepage = iosDefinitions.homepage.get()
        authors = iosDefinitions.authors.get()
        version = commonlibs.versions.library.version.get()
        ios.deploymentTarget = iosDefinitions.deployment.target.get()
        framework {
            baseName = iosDefinitions.basename.get()
            isStatic = false
            transitiveExport = true
            embedBitcode(BITCODE)
        }
        specRepos {
            url("https://github.com/Dzertak/KMMLibrary.git") //use your repo here
        }
        publishDir = rootProject.file("./")
    }

//    cocoapods {
//        summary = "Some description for the Shared Module"
//        homepage = "Link to the Shared Module homepage"
//        version = "1.0"
//        ios.deploymentTarget = "14.1"
//        framework {
//            baseName = "shared"
//        }
//    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                //put your multiplatform dependencies here
                api(commonlibs.kotlin.stdlib)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val androidMain by getting {
            dependencies {
                //Add your specific android dependencies here
            }
        }
        val androidUnitTest by getting {
            dependsOn(androidMain)
            dependsOn(commonMain)
            dependencies {
                implementation(kotlin("test-junit"))
                //you should add the android junit here
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                //Add any ios specific dependencies here, remember to also add them to the export block
            }
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

android {
    //namespace = "com.dzertak.kmmlibrary"
    namespace = commonlibs.versions.library.group.get()
    //compileSdk = commonlibs.versions.compileSdk.get() as Int
    compileSdk = 33
    defaultConfig {
        minSdk = 26
    }
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    beforeEvaluate {
        libraryVariants.all {
            compileOptions {
                // Flag to enable support for the new language APIs
                isCoreLibraryDesugaringEnabled = true
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
    dependencies {
        //coreLibraryDesugaring(commonlibs.versions.compileSdk.get() as String)//same as with compileSdk
        coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")//same as with compileSdk
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    buildToolsVersion = "33.0.1"
    compileOptions {
        sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
        targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
    }
}

fun readProperties(propertiesFile: File) = Properties().apply {
    propertiesFile.inputStream().use { fis ->
        load(fis)
    }
}

publishing {
    //val properties = readProperties(project.rootProject.file("local.properties"))
    repositories {
        maven {
            name = "KMMLibrary"
            url = uri("https://maven.pkg.github.com/dzertak/KMMLibrary")
            credentials {
                username = System.getenv("USERNAME") //?: properties.getProperty("GITHUB_ID")
                password = System.getenv("PASSWORD") //?:properties.getProperty("GITHUB_PACKAGES_TOKEN")
            }
        }
    }
    val thePublications = listOf(androidTarget) + "kotlinMultiplatform"
    publications {
        matching { it.name in thePublications }.all {
            val targetPublication = this@all
            tasks.withType<AbstractPublishToMaven>()
                .matching { it.publication == targetPublication }
                .configureEach { onlyIf { findProperty("isMainHost") == "true" } }
        }
        matching { it.name.contains("ios", true) }.all {
            val targetPublication = this@all
            tasks.withType<AbstractPublishToMaven>()
                .matching { it.publication == targetPublication }
                .forEach { it.enabled = false }
        }
    }
}

afterEvaluate {
    tasks.named("podPublishDebugXCFramework") {
        enabled = false
    }
    tasks.named("podSpecDebug") {
        enabled = false
    }
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }
    tasks.withType<AbstractTestTask>().configureEach {
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events("started", "skipped", "passed", "failed")
            showStandardStreams = true
        }
    }
}

val buildIdAttribute = Attribute.of("buildIdAttribute", String::class.java)
configurations.forEach {
    it.attributes {
        attribute(buildIdAttribute, it.name)
    }
}

val moveIosPodToRoot by tasks.registering {
    group = "com.dzertak.kmmlibrary"
    doLast {
        val releaseDir = rootProject.file(
            "./release"
        )
        releaseDir.copyRecursively(
            rootProject.file("./"),
            true
        )
        releaseDir.deleteRecursively()
    }
}

tasks.named("podPublishReleaseXCFramework") {
    finalizedBy(moveIosPodToRoot)
}

val publishPlatforms by tasks.registering {
    group = "com.dzertak.kmmlibrary"
    dependsOn(
        //tasks.named("publishAndroidReleasePublicationToGithubRepository"),
        tasks.named("podPublishReleaseXCFramework")
    )
    doLast {
        exec { commandLine = listOf("git", "add", "-A") }
        exec { commandLine = listOf("git", "commit", "-m", "iOS binary lib for version ${commonlibs.versions.library.version.get()}") }
        exec { commandLine = listOf("git", "push", "-u", "origin", "master") }
        exec { commandLine = listOf("git", "tag", commonlibs.versions.library.version.get()) }
        exec { commandLine = listOf("git", "push", "--tags") }
        println("version ${commonlibs.versions.library.version.get()} built and published")
    }
}

val compilePlatforms by tasks.registering {
    group = "com.dzertak.kmmlibrary"
    dependsOn(
        tasks.named("compileKotlinIosArm64"),
        tasks.named("compileKotlinIosX64"),
        tasks.named("compileKotlinIosSimulatorArm64"),
        tasks.named("compileReleaseKotlinAndroid")
    )
    doLast {
        println("Finished compilation")
    }
}