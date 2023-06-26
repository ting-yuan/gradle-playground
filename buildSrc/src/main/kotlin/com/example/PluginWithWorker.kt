package com.example

import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor

class PluginWithWorker @Inject constructor(val workerExecutor: WorkerExecutor) : Plugin<Project> {

  override fun apply(project: Project) {
    val kspAADepCfgName = "KSPAADependency"
    val kspAADepCfg = project.configurations.maybeCreate(kspAADepCfgName).apply {
      isTransitive = false
    }
    // Chosen arbitrarily; Get something to download and set to classpath.
    project.dependencies.add(kspAADepCfgName, "com.google.devtools.ksp:symbol-processing-api:1.8.22-1.0.11")
    project.task("hello") {
      it.doLast {
        // Uncomment to see classloaders in this context.
        // clinfo(this.javaClass.classLoader, "loader of PluginWithWorker; no isolation")
        workerExecutor.classLoaderIsolation {
          it.classpath.setFrom(kspAADepCfg)
        }.submit(KspAAWorkerAction::class.java) {
          it.name.value("classLoaderIsolation")
        }
      }
    }
  }
}

interface KspAAWorkParameters : WorkParameters {
  val name: Property<String>
}

abstract class KspAAWorkerAction : WorkAction<KspAAWorkParameters> {
  override fun execute() {
    clinfo(this.javaClass.classLoader, parameters.name.get())
  }
}

fun clinfo(classloader: ClassLoader, name: String) {
  val fqn = "org/jetbrains/kotlin/com/intellij/openapi/application/Application.class"
  println("WHERE IS Application: ${classloader.getResource(fqn)?.path}")
  var cl: ClassLoader? = classloader
  while (cl != null) {
    println("CLINFO: $name: $cl")
    if (cl is java.net.URLClassLoader) {
      for (url in cl.urLs) {
        println("  ${url.path}")
      }
    }
    cl = cl.parent
  }
}
