pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "feed"
include("UserApp")
include("FeedApp")
include("FanOutApp")
include("GatewayApp")