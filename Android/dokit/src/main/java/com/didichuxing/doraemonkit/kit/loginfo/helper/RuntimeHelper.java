package com.didichuxing.doraemonkit.kit.loginfo.helper;

import com.didichuxing.doraemonkit.kit.loginfo.util.ArrayUtil;

import java.io.IOException;
import java.util.List;

/**
 * Helper functions for running processes.
 *
 * @author nolan
 */
public class RuntimeHelper {

    /**
     * Exec the arguments, using root if necessary.
     *
     * @param args
     */
    public static Process exec(List<String> args) throws IOException {
        return Runtime.getRuntime().exec(ArrayUtil.toArray(args, String.class));
    }

    public static void destroy(Process process) {
        if (process == null) {
            return;
        }
        // Closing the reader is not sufficient on recent releases: logcat can
        // remain alive while its pipe is being drained.  Request a graceful
        // shutdown first and use the public Java 8 hard-stop API when it is
        // available on the device.
        try {
            process.destroy();
        } catch (RuntimeException ignored) {
            // Best effort; a process which has already exited can throw here.
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            } catch (RuntimeException ignored) {
                // Some vendor process implementations do not expose the hard
                // stop operation even though the API level is high enough.
            }
        }
    }

}
