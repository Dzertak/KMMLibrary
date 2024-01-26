pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("commonlibs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "KMMLibrary"
include(":shared")