pluginManagement {
    val properties = java.util.Properties()
    file("local.properties").takeIf { it.isFile }?.inputStream()?.use {
        properties.load(java.io.InputStreamReader(it, Charsets.UTF_8))
    }

    extra["muditaUsername"] = properties.getProperty("mudita_repo_username")
        ?: System.getenv("ARTIFACTORY_USERNAME")
    extra["muditaPassword"] = properties.getProperty("mudita_repo_password")
        ?: System.getenv("ARTIFACTORY_PASSWORD")
    extra["muditaRepoUrl"] = properties.getProperty("mudita_nexus_repo_url")

    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven {
            url = uri(extra["muditaRepoUrl"].toString())
            credentials {
                username = extra["muditaUsername"].toString()
                password = extra["muditaPassword"].toString()
            }
        }
    }
}

plugins {
    val sentryPlugin = "0.0.43"
    id("com.mudita.sentry.plugins.release") version sentryPlugin apply false
}

include(":app")
include(":MapJava")
include(":MapApi")

include(":pagination")
project(":pagination").projectDir = File(rootProject.projectDir, "compose/pagination")

include(":data")
project(":data").projectDir = File(rootProject.projectDir, "compose/data")

include(":common")
project(":common").projectDir = File(rootProject.projectDir, "compose/common")

File(rootProject.projectDir, "compose/screens")
    .listFiles()
    ?.forEach { moduleFile ->
        val moduleName = ":${moduleFile.name}"
        include(moduleName)
        project(moduleName).projectDir = moduleFile
    }

include(":frontitude")
