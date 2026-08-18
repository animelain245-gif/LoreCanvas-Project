plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(project(":core-domain"))
    implementation(project(":core-events"))
    implementation(project(":core-repository"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
