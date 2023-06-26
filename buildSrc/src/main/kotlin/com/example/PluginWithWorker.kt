package com.example

import javax.inject.Inject
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class PluginWithWorker @Inject constructor(val workerExecutor: WorkerExecutor) : KotlinCompilerPluginSupportPlugin {
  val KSP_PLUGIN_ID = "com.google.devtools.ksp.symbol-processing"
  val KSP_COMPILER_PLUGIN_ID = "symbol-processing"
  val KSP_GROUP_ID = "com.google.devtools.ksp"
  val KSP_VERSION = "1.8.22-1.0.11"

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val kspAADepCfgName = "KSPAADependency"
    val kspAADepCfg = project.configurations.maybeCreate(kspAADepCfgName).apply {
      isTransitive = false
    }
    project.dependencies.add(kspAADepCfgName, "$KSP_GROUP_ID:symbol-processing-api:$KSP_VERSION")
    project.task("hello_" + kotlinCompilation.compilationName) {
      it.doLast {
        clinfo(this.javaClass.classLoader, "loader of PluginWithWorker; no isolation")
        workerExecutor.classLoaderIsolation {
          it.classpath.setFrom(kspAADepCfg)
        }.submit(KspAAWorkerAction::class.java) {
          it.name.value("classLoaderIsolation")
        }
      }
    }
    return project.provider { emptyList() }
  }

  override fun getCompilerPluginId() = KSP_PLUGIN_ID
  override fun getPluginArtifact(): SubpluginArtifact =
    SubpluginArtifact(
      groupId = KSP_GROUP_ID,
      artifactId = KSP_COMPILER_PLUGIN_ID,
      version = KSP_VERSION
    )

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
    return true
  }
}

interface KspAAWorkParameters : WorkParameters {
  val kspClasspath: ConfigurableFileCollection
  val name: Property<String>
}

abstract class KspAAWorkerAction : WorkAction<KspAAWorkParameters> {
  override fun execute() {
    clinfo(this.javaClass.classLoader, parameters.name.get())
  }
}

fun clinfo(classloader: ClassLoader, name: String) {
  println("CLINFO: $name: $classloader")
  val fqn = "org/jetbrains/kotlin/com/intellij/openapi/application/Application.class"
  println("WHERE IS Application: ${classloader.getResource(fqn)?.path}")
  var cl: ClassLoader? = classloader
  while (cl != null) {
    if (cl is java.net.URLClassLoader) {
      for (url in cl.urLs) {
        println("  ${url.path}")
      }
    }
    cl = cl.parent
  }
}
