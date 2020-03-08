import java.util.*

val loadProperties: (propertiesFile: File) -> Properties? by extra { propertiesFile ->
    propertiesFile.takeIf { it.exists() && it.isFile }?.let {
        it.inputStream().use {
            Properties().apply { load(it) }
        }
    }
}

val localProperties: Properties? by extra {
    loadProperties(File("${buildscript.sourceFile!!.parent}/../local.properties"))
}

val getBoolProperty: (name: String, default: Boolean?) -> Boolean by extra { name, default ->
    // local property always wins
    tryGetFromLocalProperties(name)?.toBoolean()
        ?: (if (extra.has(name)) extra[name]?.toString()?.toBoolean() else null)
        ?: default
        ?: error("Property $name is not found ${gradle} ${extra.properties}")
}


val tryGetFromLocalProperties: (name: String) -> String? by extra { name ->
    localProperties?.getProperty(name)
}

val getIncludeAndroidModules: () -> Boolean  by extra {
    { getBoolProperty.invoke("includeAndroidModules", null)}
}
val getIncludeIosModules: () -> Boolean by extra {
    { getBoolProperty("includeIosModules", null) && gradle.startParameter.taskNames.none { it.contains("beforePush") } }
}


