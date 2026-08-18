plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(project(":core-common"))
    implementation(project(":core-events"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
