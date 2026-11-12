package com.didichuxing.doraemonkit.plugin.transform

import com.didichuxing.doraemonkit.plugin.extension.DoKitExtension
import org.objectweb.asm.tree.ClassNode
import java.io.File

interface TransformContext {
    val name: String
    val projectDir: File
    val dokitExtension: DoKitExtension
}

data class DefaultDoKitTransformContext(
    override val name: String,
    override val projectDir: File,
    override val dokitExtension: DoKitExtension
) : TransformContext

interface ClassTransformer {
    fun transform(context: TransformContext, klass: ClassNode): ClassNode = klass
}

val ClassNode.className: String
    get() = name.replace('/', '.')
