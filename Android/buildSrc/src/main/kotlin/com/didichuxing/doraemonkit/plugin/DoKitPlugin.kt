package com.didichuxing.doraemonkit.plugin

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.didichuxing.doraemonkit.plugin.extension.DoKitExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class DoKitPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("dokit", DoKitExtension::class.java)

        project.pluginManager.withPlugin("com.android.application") {
            configureAndroid(project, extension, ScopedArtifacts.Scope.ALL)
        }
        project.pluginManager.withPlugin("com.android.dynamic-feature") {
            configureAndroid(project, extension, ScopedArtifacts.Scope.ALL)
        }
        project.pluginManager.withPlugin("com.android.library") {
            configureAndroid(project, extension, ScopedArtifacts.Scope.PROJECT)
        }
    }

    @Suppress("UnstableApiUsage")
    private fun configureAndroid(
        project: Project,
        extension: DoKitExtension,
        scope: ScopedArtifacts.Scope
    ) {
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants(androidComponents.selector().all()) { variant ->
            if (variant.name.contains("release", ignoreCase = true)) {
                return@onVariants
            }

            val taskName = "transform${variant.name.replaceFirstChar(Char::uppercaseChar)}WithDoKit"
            val transformTask = project.tasks.register(taskName, DoKitTransformTask::class.java) { task ->
                task.group = "dokit"
                task.description = "Instruments ${variant.name} classes with DoKit"
                task.variantName.set(variant.name)
                task.projectPath.set(project.path)
                task.mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                task.extensionOptions.set(project.provider { extension.asOptions() })
                task.stackEnterMethods.set(project.provider { extension.slowMethod.stackMethod.enterMethods })
                task.stackBlacklist.set(project.provider { extension.slowMethod.stackMethod.methodBlacklist })
                task.normalPackageNames.set(project.provider { extension.slowMethod.normalMethod.packageNames })
                task.normalBlacklist.set(project.provider { extension.slowMethod.normalMethod.methodBlacklist })
                task.pluginSwitch.set(project.booleanProperty("DOKIT_PLUGIN_SWITCH", true))
                task.logSwitch.set(project.booleanProperty("DOKIT_LOG_SWITCH", false))
                task.slowMethodSwitch.set(project.booleanProperty("DOKIT_METHOD_SWITCH", false))
                task.slowMethodStrategy.set(project.intProperty("DOKIT_METHOD_STRATEGY", 0))
                task.methodStackLevel.set(project.intProperty("DOKIT_METHOD_STACK_LEVEL", 5))
                task.webViewClassName.set(project.stringProperty("DOKIT_WEBVIEW_CLASS_NAME", ""))
                task.thirdLibInfoSwitch.set(project.booleanProperty("DOKIT_THIRD_LIB_SWITCH", true))
            }

            variant.artifacts.forScope(scope)
                .use(transformTask)
                .toTransform(
                    ScopedArtifact.CLASSES,
                    DoKitTransformTask::inputJars,
                    DoKitTransformTask::inputDirectories,
                    DoKitTransformTask::outputJar
                )
        }
    }

    private fun DoKitExtension.asOptions(): Map<String, String> = mapOf(
        "pluginEnable" to pluginEnable.toString(),
        "logEnable" to logEnable.toString(),
        "thirdLibEnable" to thirdLibEnable.toString(),
        "gpsEnable" to gpsEnable.toString(),
        "gpsLocal" to gps.local.toString(),
        "gpsBaidu" to gps.baidu.toString(),
        "gpsTencent" to gps.tencent.toString(),
        "gpsAmap" to gps.amap.toString(),
        "gpsDidi" to gps.didi.toString(),
        "networkEnable" to networkEnable.toString(),
        "networkOkHttp" to network.okHttp.toString(),
        "networkUrlConnect" to network.urlConnect.toString(),
        "networkDidiHttp" to network.didiHttp.toString(),
        "networkDidiSocket" to network.didiSocket.toString(),
        "networkDidiDjSocket" to network.didiDjSocket.toString(),
        "bigImageEnable" to bigImageEnable.toString(),
        "bigImageGlide" to bigImage.glide.toString(),
        "bigImagePicasso" to bigImage.picasso.toString(),
        "bigImageFresco" to bigImage.fresco.toString(),
        "bigImageImageLoader" to bigImage.imageLoader.toString(),
        "bigImageCoil" to bigImage.coil.toString(),
        "webViewEnable" to webViewEnable.toString(),
        "webViewNetwork" to webView.network.toString(),
        "webViewDoKit" to webView.dokitWeb.toString(),
        "webViewVConsole" to webView.vConsole.toString(),
        "slowMethodEnable" to slowMethodEnable.toString(),
        "stackThreshold" to slowMethod.stackMethod.thresholdTime.toString(),
        "normalThreshold" to slowMethod.normalMethod.thresholdTime.toString()
    )

    private fun Project.booleanProperty(name: String, default: Boolean) =
        providers.gradleProperty(name).map(String::toBoolean).orElse(default)

    private fun Project.intProperty(name: String, default: Int) =
        providers.gradleProperty(name).map(String::toInt).orElse(default)

    private fun Project.stringProperty(name: String, default: String) =
        providers.gradleProperty(name).orElse(default)
}
