package plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.kotlin.dsl.extra
import java.io.File
import java.util.Properties

/**
 * Load certain properties from local environment as merge them to projects `extra`s.
 *
 * Apply this plugin to root project.
 */
class LocalPropertiesLoader : Plugin<Project> {

    private val propertiesToLoad = listOf(
        "mozilla.build.kotlin.warnings.as.error"
    )

    override fun apply(project: Project) {
        val file = project.rootProject.file(PROPERTIES_FILE)
        if (file.exists()) {
            file.loadPropertiesIntoExtras(project.extra)
        } else {
            notifyAboutFailedLoading(file)
        }
    }

    private fun File.loadPropertiesIntoExtras(extras: ExtraPropertiesExtension) {
        Properties()
            .apply { load(inputStream()) }
            .filter { propertiesToLoad.contains(it.key) }
            .forEach { (key, value) ->
                extras[key.toString()] = value

                logLoadedProperty(key, value)
            }
    }

    private fun notifyAboutFailedLoading(file: File) {
        println("Warning: Can't load local properties from `${file.absolutePath}`. File not found.")
    }

    @Suppress("ConstantConditionIf")
    private fun logLoadedProperty(key: Any, value: Any?) {
        if (!DEBUG) return

        println("Loaded property '$key' -> $value")
    }
}

private const val DEBUG = false
private const val PROPERTIES_FILE = "local.properties"
