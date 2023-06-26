import com.example.PluginWithWorker

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

// Apply the plugin
apply<PluginWithWorker>()