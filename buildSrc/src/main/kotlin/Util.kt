import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.project
import org.gradle.plugin.use.PluginDependenciesSpec

fun Dependency.notation(): String {
    return "$group:$name:$version"
}

fun DependencyHandlerScope.classpath(dependency: Dependency) {
    add(ScriptHandler.CLASSPATH_CONFIGURATION, dependency.notation())
}

fun DependencyHandlerScope.classpathAll(
    dependencyFirst: Dependency,
    dependencySecond: Dependency,
    vararg dependencyOther: Dependency
) {
    classpath(dependencyFirst)
    classpath(dependencySecond)
    dependencyOther.forEach(::classpath)
}

fun DependencyHandlerScope.implementation(dependency: Dependency) {
    add("implementation", dependency.notation())
}

fun DependencyHandlerScope.implementationProject(
    projectName: String
) {
    add("implementation", project(projectName))
}

fun DependencyHandlerScope.implementationProjects(
    projectNameFirst: String,
    projectNameSecond: String,
    vararg projectNameOther: String
) {
    implementationProject(projectNameFirst)
    implementationProject(projectNameSecond)
    projectNameOther.forEach(::implementationProject)
}

fun PluginDependenciesSpec.apply(plugin: Plugin) {
    id(plugin.name)
}

fun PluginDependenciesSpec.applyAll(
    firstPlugin: Plugin,
    secondPlugin: Plugin,
    vararg other: Plugin
) {
    apply(firstPlugin)
    apply(secondPlugin)
    other.forEach(::apply)
}
