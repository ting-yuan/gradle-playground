plugins {
    kotlin("jvm") version "1.8.22"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())

    // This is fine. kotlin-gradle-plugin-api doesn't depend on kotlin-compiler-embeddable.
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:1.8.22")

    // NOT OK: This pulls kotlin-compiler-embeddable into classpath.
    // Would KSP 1.x, which needs "implementation", be switched to use "compileOnly"?
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.22")

    // Uncommenting this is fine.
    // compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.8.22")
}


