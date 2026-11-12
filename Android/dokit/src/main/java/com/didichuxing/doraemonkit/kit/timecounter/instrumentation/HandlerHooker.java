package com.didichuxing.doraemonkit.kit.timecounter.instrumentation;

import android.app.Application;
/**
 * @deprecated ActivityThread/Instrumentation are private implementation
 * details and cannot be safely hooked on modern Android releases. Activity
 * timing is now collected by {@code Application.ActivityLifecycleCallbacks},
 * including the public API 29 pre/post callbacks.
 */
@Deprecated
public final class HandlerHooker {
    private HandlerHooker() {
    }

    /**
     * Kept as a source/binary compatibility shim for integrations that called
     * the old helper directly. It intentionally performs no operation.
     */
    public static void doHook(Application app) {
        // No hidden API access here. See DoKitActivityLifecycleCallbacks.
    }

    static boolean isHookSucceed() {
        return false;
    }
}
