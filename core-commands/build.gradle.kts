plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(project(":core-common"))
    implementation(project(":core-domain"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
