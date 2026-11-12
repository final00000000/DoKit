package com.didichuxing.doraemonkit.plugin

import com.didichuxing.doraemonkit.plugin.transform.TransformContext
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.io.File

fun MethodNode.isGetSetMethod(): Boolean {
    var constructorCalls = 0
    for (insn in instructions) {
        val opcode = insn.opcode
        if (opcode == -1) continue
        if (opcode != GETFIELD && opcode != GETSTATIC && opcode != H_GETFIELD && opcode != H_GETSTATIC &&
            opcode != RETURN && opcode != ARETURN && opcode != DRETURN && opcode != FRETURN &&
            opcode != LRETURN && opcode != IRETURN && opcode != PUTFIELD && opcode != PUTSTATIC &&
            opcode != H_PUTFIELD && opcode != H_PUTSTATIC && opcode > SALOAD
        ) {
            if (name == "<init>" && opcode == INVOKESPECIAL && ++constructorCalls <= 1) continue
            return false
        }
    }
    return true
}

fun MethodNode.isSingleMethod(): Boolean = instructions.none {
    it.opcode in INVOKEVIRTUAL..INVOKEDYNAMIC
}

fun MethodNode.isEmptyMethod(): Boolean = instructions.none { it.opcode != -1 }

fun MethodNode.isMainMethod(className: String): Boolean {
    val main = name == "main" && desc == "([Ljava/lang/String;)V"
    if (main) "====isMainMethod====$className  $name   $desc   $access".println()
    return main
}

fun InsnList.getMethodExitInsnNodes(): Sequence<AbstractInsnNode> = iterator().asSequence().filter {
    it.opcode == RETURN || it.opcode == IRETURN || it.opcode == FRETURN || it.opcode == ARETURN ||
        it.opcode == LRETURN || it.opcode == DRETURN || it.opcode == ATHROW
}

fun TransformContext.isRelease(): Boolean = name.contains("release", ignoreCase = true)

fun String.println() {
    if (DoKitExtUtil.dokitLogSwitchOpen()) kotlin.io.println("[dokit plugin]===>$this")
}

fun File.lastPath(): String = name

fun <T> Iterator<T>.asIterable(): Iterable<T> = Iterable { this }

val MethodInsnNode.ownerClassName: String
    get() = owner.replace('/', '.')

val ClassNode.formatSuperName: String
    get() = superName?.replace('/', '.') ?: ""
