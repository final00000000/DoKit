package com.didichuxing.doraemonkit.kit.loginfo.helper;

import android.os.Build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogcatHelper {
    private static final String TAG = "LogcatHelper";
    public static final String BUFFER_MAIN = "main";
    public static final String BUFFER_EVENTS = "events";
    public static final String BUFFER_RADIO = "radio";


    public static Process getLogcatProcess(String buffer) throws IOException {

        List<String> args = getLogcatArgs(buffer);

        return RuntimeHelper.exec(args);
    }

    private static List<String> getLogcatArgs(String buffer) {
        List<String> args = new ArrayList<>(Arrays.asList("logcat", "-v", "time"));

        // Android 7.0+ allows an app to scope logcat to its own process.  Reading
        // the complete device buffer is increasingly restricted on newer releases
        // and is also needlessly expensive for the in-app log viewer.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            args.add("--pid=" + android.os.Process.myPid());
        }

        // for some reason, adding -b main excludes log output from AndroidRuntime runtime exceptions,
        // whereas just leaving it blank keeps them in.  So do not specify the buffer if it is "main"
        if (!buffer.equals(BUFFER_MAIN)) {
            args.add("-b");
            args.add(buffer);
        }

        return args;
    }

    public static String getLastLogLine(String buffer) {
        Process dumpLogcatProcess = null;
        String result = null;
        try {

            List<String> args = getLogcatArgs(buffer);
            // We only need a marker for recording mode.  Asking logcat for the
            // complete buffer can be very expensive on a busy device.
            args.add("-t");
            args.add("1");
            args.add("-d");

            dumpLogcatProcess = RuntimeHelper.exec(args);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    dumpLogcatProcess.getInputStream()), 8192)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result = line;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (dumpLogcatProcess != null) {
                RuntimeHelper.destroy(dumpLogcatProcess);
            }

        }

        return result;
    }
}
