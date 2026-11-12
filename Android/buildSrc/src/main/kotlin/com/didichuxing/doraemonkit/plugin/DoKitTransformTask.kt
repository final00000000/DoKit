package com.didichuxing.doraemonkit.plugin

import com.didichuxing.doraemonkit.plugin.extension.*
import com.didichuxing.doraemonkit.plugin.processor.DoKitComponentHandler
import com.didichuxing.doraemonkit.plugin.stack_method.MethodStackNodeUtil
import com.didichuxing.doraemonkit.plugin.thirdlib.ThirdLibInfo
import com.didichuxing.doraemonkit.plugin.transform.ClassTransformer
import com.didichuxing.doraemonkit.plugin.transform.DefaultDoKitTransformContext
import com.didichuxing.doraemonkit.plugin.transform.classtransform.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.zip.ZipFile
import javax.xml.parsers.SAXParserFactory

abstract class DoKitTransformTask : DefaultTask() {
    @get:Classpath
    abstract val inputJars: ListProperty<RegularFile>

    @get:Classpath
    abstract val inputDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val variantName: Property<String>

    @get:Input
    abstract val projectPath: Property<String>

    @get:Input
    abstract val extensionOptions: MapProperty<String, String>

    @get:Input
    abstract val stackEnterMethods: SetProperty<String>

    @get:Input
    abstract val stackBlacklist: SetProperty<String>

    @get:Input
    abstract val normalPackageNames: SetProperty<String>

    @get:Input
    abstract val normalBlacklist: SetProperty<String>

    @get:Input
    abstract val pluginSwitch: Property<Boolean>

    @get:Input
    abstract val logSwitch: Property<Boolean>

    @get:Input
    abstract val slowMethodSwitch: Property<Boolean>

    @get:Input
    abstract val slowMethodStrategy: Property<Int>

    @get:Input
    abstract val methodStackLevel: Property<Int>

    @get:Input
    abstract val webViewClassName: Property<String>

    @get:Input
    abstract val thirdLibInfoSwitch: Property<Boolean>

    @TaskAction
    fun transform() {
        val dokit = createExtension()
        configureRuntime(dokit)

        val context = DefaultDoKitTransformContext(
            name = variantName.get(),
            projectDir = project.projectDir,
            dokitExtension = dokit
        )
        val firstStage = File(temporaryDir, "classes-0.jar")
        transformInputs(
            inputJars.get().map(RegularFile::getAsFile),
            inputDirectories.get().map(Directory::getAsFile),
            firstStage,
            commonTransformers(),
            context
        )

        var current = firstStage
        if (DoKitExtUtil.dokitSlowMethodSwitchOpen() &&
            DoKitExtUtil.SLOW_METHOD_STRATEGY == SlowMethodExtension.STRATEGY_STACK
        ) {
            for (level in 1 until methodStackLevel.get()) {
                val next = File(temporaryDir, "classes-$level.jar")
                transformJar(current, next, listOf(MSDClassTransformer(level)), context)
                current = next
            }
        }

        val output = outputJar.get().asFile
        output.parentFile.mkdirs()
        Files.copy(current.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    private fun createExtension(): DoKitExtension {
        val options = extensionOptions.get()
        return DoKitExtension(
            pluginEnable = options.boolean("pluginEnable"),
            logEnable = options.boolean("logEnable"),
            thirdLibEnable = options.boolean("thirdLibEnable"),
            gpsEnable = options.boolean("gpsEnable"),
            gps = GpsExtension(
                local = options.boolean("gpsLocal"),
                baidu = options.boolean("gpsBaidu"),
                tencent = options.boolean("gpsTencent"),
                amap = options.boolean("gpsAmap"),
                didi = options.boolean("gpsDidi")
            ),
            networkEnable = options.boolean("networkEnable"),
            network = NetworkExtension(
                okHttp = options.boolean("networkOkHttp"),
                urlConnect = options.boolean("networkUrlConnect"),
                didiHttp = options.boolean("networkDidiHttp"),
                didiSocket = options.boolean("networkDidiSocket"),
                didiDjSocket = options.boolean("networkDidiDjSocket")
            ),
            bigImageEnable = options.boolean("bigImageEnable"),
            bigImage = BigImageExtension(
                glide = options.boolean("bigImageGlide"),
                picasso = options.boolean("bigImagePicasso"),
                fresco = options.boolean("bigImageFresco"),
                imageLoader = options.boolean("bigImageImageLoader"),
                coil = options.boolean("bigImageCoil")
            ),
            webViewEnable = options.boolean("webViewEnable"),
            webView = WebViewExtension(
                network = options.boolean("webViewNetwork"),
                dokitWeb = options.boolean("webViewDoKit"),
                vConsole = options.boolean("webViewVConsole")
            ),
            slowMethodEnable = options.boolean("slowMethodEnable"),
            slowMethod = SlowMethodExtension(
                stackMethod = SlowMethodExtension.StackMethodExt(
                    thresholdTime = options.int("stackThreshold"),
                    enterMethods = stackEnterMethods.get().toMutableSet(),
                    methodBlacklist = stackBlacklist.get().toMutableSet()
                ),
                normalMethod = SlowMethodExtension.NormalMethodExt(
                    thresholdTime = options.int("normalThreshold"),
                    packageNames = normalPackageNames.get().toMutableSet(),
                    methodBlacklist = normalBlacklist.get().toMutableSet()
                )
            )
        )
    }

    private fun configureRuntime(dokit: DoKitExtension) {
        DoKitExtUtil.DOKIT_PLUGIN_SWITCH = pluginSwitch.get() && dokit.pluginEnable
        DoKitExtUtil.DOKIT_LOG_SWITCH = logSwitch.get() && dokit.logEnable
        DoKitExtUtil.SLOW_METHOD_SWITCH = slowMethodSwitch.get() && dokit.slowMethodEnable
        DoKitExtUtil.SLOW_METHOD_STRATEGY = slowMethodStrategy.get()
        DoKitExtUtil.STACK_METHOD_LEVEL = methodStackLevel.get()
        DoKitExtUtil.WEBVIEW_CLASS_NAME = webViewClassName.get()
        DoKitExtUtil.THIRD_LIBINFO_SWITCH = thirdLibInfoSwitch.get() && dokit.thirdLibEnable

        MethodStackNodeUtil.METHOD_STACK_KEYS.clear()
        if (DoKitExtUtil.dokitSlowMethodSwitchOpen() &&
            DoKitExtUtil.SLOW_METHOD_STRATEGY == SlowMethodExtension.STRATEGY_STACK
        ) {
            repeat(methodStackLevel.get().coerceAtLeast(1)) {
                MethodStackNodeUtil.METHOD_STACK_KEYS.add(mutableSetOf())
            }
        }

        readManifestConfig(dokit)
        collectThirdLibraries()
    }

    private fun readManifestConfig(dokit: DoKitExtension) {
        val handler = DoKitComponentHandler()
        SAXParserFactory.newInstance().newSAXParser().parse(mergedManifest.get().asFile, handler)
        DoKitExtUtil.setAppPackageName(handler.appPackageName)
        DoKitExtUtil.setApplications(handler.applications)
        DoKitExtUtil.init(dokit)
    }

    private fun collectThirdLibraries() {
        val jars = inputJars.get().map(RegularFile::getAsFile)
        DoKitExtUtil.DOKIT_GPS_MOCK_INCLUDE = jars.any {
            it.name.contains("dokit-gps-mock", ignoreCase = true) ||
                it.name.contains("dokitx-gps-mock", ignoreCase = true)
        }
        DoKitExtUtil.THIRD_LIB_INFOS.clear()
        if (DoKitExtUtil.THIRD_LIBINFO_SWITCH) {
            jars.sortedBy(File::getName).forEach {
                DoKitExtUtil.THIRD_LIB_INFOS.add(ThirdLibInfo(it.nameWithoutExtension, it.length()))
            }
        }
    }

    private fun commonTransformers(): List<ClassTransformer> = listOf(
        CommClassTransformer(),
        ThirdLibsClassTransformer(),
        Okhttp3ClassTransformer(),
        UrlConnectionTransformer(),
        WebViewClassTransformer(),
        GPSClassTransformer(),
        GPSAMapClassTransformer(),
        GPSBDClassTransformer(),
        GPSTencentClassTransformer(),
        BigImgClassTransformer(),
        GSMClassTransformer(),
        EnterMSClassTransformer()
    )

    private fun transformInputs(
        jars: List<File>,
        directories: List<File>,
        output: File,
        transformers: List<ClassTransformer>,
        context: DefaultDoKitTransformContext
    ) {
        output.parentFile.mkdirs()
        val written = mutableSetOf<String>()
        JarOutputStream(output.outputStream().buffered()).use { target ->
            jars.sortedBy(File::getAbsolutePath).forEach { jar ->
                ZipFile(jar).use { source ->
                    val entries = source.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (!entry.isDirectory && entry.name.endsWith(".class")) {
                            writeClass(target, written, entry.name, source.getInputStream(entry).readBytes(), transformers, context)
                        }
                    }
                }
            }
            directories.sortedBy(File::getAbsolutePath).forEach { directory ->
                directory.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { file ->
                    val name = file.relativeTo(directory).invariantSeparatorsPath
                    writeClass(target, written, name, file.readBytes(), transformers, context)
                }
            }
        }
    }

    private fun transformJar(
        input: File,
        output: File,
        transformers: List<ClassTransformer>,
        context: DefaultDoKitTransformContext
    ) = transformInputs(listOf(input), emptyList(), output, transformers, context)

    private fun writeClass(
        target: JarOutputStream,
        written: MutableSet<String>,
        name: String,
        bytes: ByteArray,
        transformers: List<ClassTransformer>,
        context: DefaultDoKitTransformContext
    ) {
        if (!written.add(name)) {
            logger.info("DoKit skipped duplicate class $name")
            return
        }
        val transformed = try {
            transformClass(bytes, transformers, context)
        } catch (error: Exception) {
            throw GradleException("DoKit failed to transform $name", error)
        }
        target.putNextEntry(JarEntry(name).apply { time = 0L })
        target.write(transformed)
        target.closeEntry()
    }

    private fun transformClass(
        bytecode: ByteArray,
        transformers: List<ClassTransformer>,
        context: DefaultDoKitTransformContext
    ): ByteArray {
        val initial = ClassNode().also { ClassReader(bytecode).accept(it, 0) }
        val transformed = transformers.fold(initial) { klass, transformer ->
            transformer.transform(context, klass)
        }
        return ClassWriter(ClassWriter.COMPUTE_MAXS).also(transformed::accept).toByteArray()
    }

    private fun Map<String, String>.boolean(name: String): Boolean =
        get(name)?.toBooleanStrictOrNull() ?: false

    private fun Map<String, String>.int(name: String): Int = get(name)?.toIntOrNull() ?: 0
}
