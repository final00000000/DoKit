package com.didichuxing.doraemonkit.plugin.transform.classtransform

import com.didichuxing.doraemonkit.plugin.DoKitExtUtil
import com.didichuxing.doraemonkit.plugin.extension.DoKitExtension
import com.didichuxing.doraemonkit.plugin.isRelease
import com.didichuxing.doraemonkit.plugin.transform.ClassTransformer
import com.didichuxing.doraemonkit.plugin.transform.TransformContext
import com.didichuxing.doraemonkit.plugin.transform.className
import org.objectweb.asm.tree.ClassNode

/**
 * ================================================
 * 作    者：jint（金台）
 * 版    本：1.0
 * 创建日期：2021/5/12-18:06
 * 描    述：
 * 修订历史：
 * ================================================
 */

open class AbsClassTransformer : ClassTransformer {


    open fun onDoKitClassInterceptor(context: TransformContext, klass: ClassNode): Boolean {
        if (context.isRelease()) {
            return true
        }
        if (!DoKitExtUtil.dokitPluginSwitchOpen()) {
            return true
        }

        //过滤kotlin module-info
        if (klass.className == "module-info") {
            return true
        }
        return false
    }

    override fun transform(context: TransformContext, klass: ClassNode): ClassNode {
        if (onDoKitClassInterceptor(context, klass)) {
            return klass
        }

        if (DoKitExtUtil.ignorePackageNames(klass.className)) {
            return klass
        }

        transform(context.dokitExtension, context, klass)
        return klass
    }

    open fun transform(dokit: DoKitExtension, context: TransformContext, klass: ClassNode): ClassNode = klass
}
