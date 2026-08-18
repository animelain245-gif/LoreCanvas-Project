plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(project(":core-common"))
    implementation(project(":core-domain"))
    implementation(project(":core-validation"))
    implementation(project(":core-events"))
    implementation(project(":core-commands"))
    implementation(project(":core-storage"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
