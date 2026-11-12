package com.didichuxing.doraemonkit.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Compatibility helpers for Android 17's local-network permission.
 *
 * <p>DoKit is a debug toolkit and only opens LAN sockets when a user enters a
 * feature that needs them.  Keeping the check here lets the library remain
 * source/binary compatible with applications whose minSdk is much lower than
 * API 37.</p>
 */
public final class LocalNetworkPermission {
    public static final String ACCESS_LOCAL_NETWORK =
            "android.permission.ACCESS_LOCAL_NETWORK";
    public static final int ANDROID_17 = 37;

    private LocalNetworkPermission() {
    }

    /**
     * Returns whether a runtime grant is required for this process.
     */
    public static boolean isRequired(Context context) {
        if (context == null || Build.VERSION.SDK_INT < ANDROID_17) {
            return false;
        }
        return context.getApplicationInfo().targetSdkVersion >= ANDROID_17;
    }

    /**
     * Returns true on legacy devices and when the new permission is granted.
     */
    public static boolean isGranted(Context context) {
        if (!isRequired(context)) {
            return true;
        }
        try {
            return context.checkCallingOrSelfPermission(ACCESS_LOCAL_NETWORK)
                    == PackageManager.PERMISSION_GRANTED;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /**
     * Requests the permission from an Activity when needed.  The caller should
     * continue its operation from onRequestPermissionsResult.
     */
    public static boolean request(Activity activity, int requestCode) {
        if (activity == null || !isRequired(activity) || isGranted(activity)) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(new String[]{ACCESS_LOCAL_NETWORK}, requestCode);
        }
        return false;
    }
}
