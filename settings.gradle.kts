rootProject.name = "CloudstreamPlugins"

// This file sets what projects are included. All new projects should get automatically included unless specified in "disabled" variable.
val disabled = listOf<String>()

fun includeSubprojects(dir: File, basePath: String = "") {
    if (dir.name != "build" && dir.name != "src") {
        val buildFile = File(dir, "build.gradle.kts")
        if (buildFile.exists()) {
            val projectPath = if (basePath.isEmpty()) ":${dir.name}" else "$basePath:${dir.name}"
            include(projectPath)
        }

        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                val newBasePath =
                    if (basePath.isEmpty()) ":${dir.name}" else "$basePath:${dir.name}"
                includeSubprojects(child, newBasePath)
            }
        }
    }
}

File(rootDir, ".").eachDir { dir ->
    if (!disabled.contains(dir.name)) {
        includeSubprojects(dir)
    }
}

fun File.eachDir(block: (File) -> Unit) {
    listFiles()?.filter { it.isDirectory }?.forEach { block(it) }
}

// To only include a single project, comment out the previous lines (except the first one), and include your plugin like so:
// include("PluginName")
