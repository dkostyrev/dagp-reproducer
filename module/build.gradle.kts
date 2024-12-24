import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ScopedArtifacts
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  id("com.autonomousapps.dependency-analysis") apply true
}

android {
  namespace = "com.dkostyrev.module"
  compileSdk = 34

  defaultConfig {
    minSdk = 26
  }

  buildTypes {
    release {
      isMinifyEnabled = false
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  kotlinOptions {
    jvmTarget = "11"
  }
}

abstract class SampleTransformTask : DefaultTask() {
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val jars: ListProperty<RegularFile>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val dirs: ListProperty<Directory>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction
  fun transform() {
    val outputFile = output.get().asFile
    ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
      jars.get().forEach { jar ->
        addFileToZip(jar.asFile, zipOut)
      }
      dirs.get().forEach { dir ->
        addDirectoryToZip(dir.asFile, zipOut, dir.asFile.path)
      }
    }

    println("Copying ${jars.get()} and ${dirs.get()} into ${output.get()}")
    println("Resulting jar file contents:")
    ZipFile(outputFile).use { zipFile ->
      zipFile.entries().asSequence().forEach {
        println(it.name)
      }
    }
  }

  private fun addFileToZip(file: File, zipOut: ZipOutputStream) {
    FileInputStream(file).use { fis ->
      val zipEntry = ZipEntry(file.name)
      zipOut.putNextEntry(zipEntry)
      fis.copyTo(zipOut)
      zipOut.closeEntry()
    }
  }

  private fun addDirectoryToZip(directory: File, zipOut: ZipOutputStream, basePath: String) {
    directory.walkTopDown().forEach { file ->
      if (file.isFile) {
        FileInputStream(file).use { fis ->
          val zipEntry = ZipEntry(file.relativeTo(File(basePath)).path)
          zipOut.putNextEntry(zipEntry)
          fis.copyTo(zipOut)
          zipOut.closeEntry()
        }
      }
    }
  }
}

androidComponents {
  beforeVariants {
    if (it.name != "release") {
      it.enable = false
    }
  }

  onVariants(selector().all()) {
    it.artifacts
      .forScope(ScopedArtifacts.Scope.PROJECT)
      .use(tasks.register<SampleTransformTask>("sampleTransformTask"))
      .toTransform(
        type = ScopedArtifact.CLASSES,
        inputJars = SampleTransformTask::jars,
        inputDirectories = SampleTransformTask::dirs,
        into = SampleTransformTask::output,
      )
  }
}

