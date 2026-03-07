rootProject.name = "breadcost-app"

// Currently a monolith in root. As you scale, split into modules:
// include(":breadcost-core")
// include(":inventory-service")
// include(":order-service")
// include(":production-service")
// include(":finance-service")
// include(":reporting-service")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
