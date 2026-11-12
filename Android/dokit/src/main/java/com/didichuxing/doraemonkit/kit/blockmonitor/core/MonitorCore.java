package com.didichuxing.doraemonkit.kit.blockmonitor.core;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Printer;
import android.view.Choreographer;

import com.didichuxing.doraemonkit.kit.blockmonitor.bean.BlockInfo;

import java.util.ArrayList;

/**
 * @desc: 检测卡顿的日志类
 */
class MonitorCore implements Printer {
    /** 卡顿阈值。 */
    private static final long BLOCK_THRESHOLD_MILLIS = 200L;

    private long mStartTime = 0L;
    private long mStartThreadTime = 0L;
    private boolean mPrintingStarted = false;

    /**
     * Main-looper Printer output is an implementation detail. It remains the
     * compatibility path on old releases, while API 37+ uses the public
     * Choreographer frame callback below.
     */
    private boolean mFrameMonitoring;
    private long mLastFrameElapsed;
    private long mLastFrameWall;
    private long mLastFrameThread;

    private final StackSampler mStackSampler;

    private final Choreographer.FrameCallback mFrameCallback =
            new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    onFrame(frameTimeNanos);
                }
            };

    MonitorCore() {
        mStackSampler = new StackSampler();
        mStackSampler.init();
    }

    /**
     * Starts frame-gap based monitoring. Choreographer requires the main
     * thread, so calls from other threads are posted there.
     */
    void startFrameMonitor() {
        if (mFrameMonitoring) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(this::startFrameMonitor);
            return;
        }
        mFrameMonitoring = true;
        mLastFrameElapsed = 0L;
        mLastFrameWall = 0L;
        mLastFrameThread = 0L;
        mStackSampler.startDump();
        Choreographer.getInstance().postFrameCallback(mFrameCallback);
    }

    /** Stops frame-gap based monitoring and removes the pending callback. */
    void stopFrameMonitor() {
        if (!mFrameMonitoring) {
            return;
        }
        mFrameMonitoring = false;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Choreographer.getInstance().removeFrameCallback(mFrameCallback);
        } else {
            new Handler(Looper.getMainLooper()).post(() ->
                    Choreographer.getInstance().removeFrameCallback(mFrameCallback));
        }
        mStackSampler.stopDump();
        mLastFrameElapsed = 0L;
        mLastFrameWall = 0L;
        mLastFrameThread = 0L;
    }

    private void onFrame(long frameTimeNanos) {
        if (!mFrameMonitoring) {
            return;
        }

        final long nowElapsed = SystemClock.elapsedRealtime();
        final long nowWall = System.currentTimeMillis();
        final long nowThread = SystemClock.currentThreadTimeMillis();
        if (mLastFrameElapsed > 0L
                && nowElapsed - mLastFrameElapsed > BLOCK_THRESHOLD_MILLIS) {
            notifyFrameBlock(mLastFrameWall, nowWall, mLastFrameThread, nowThread);
        }
        mLastFrameElapsed = nowElapsed;
        mLastFrameWall = nowWall;
        mLastFrameThread = nowThread;

        if (mFrameMonitoring) {
            Choreographer.getInstance().postFrameCallback(mFrameCallback);
        }
    }

    private void notifyFrameBlock(long startWall, long endWall,
                                  long startThread, long endThread) {
        ArrayList<String> entries = mStackSampler.getThreadStackEntries(startWall, endWall);
        // A short (>200 ms) stall may finish before the first sampler tick.
        // Keep the event instead of dropping it solely because no sample was
        // captured; the current main-thread stack is still useful context.
        if (entries.isEmpty()) {
            StringBuilder stack = new StringBuilder();
            Thread mainThread = Looper.getMainLooper().getThread();
            for (StackTraceElement element : mainThread.getStackTrace()) {
                stack.append(element).append(BlockInfo.SEPARATOR);
            }
            if (stack.length() > 0) {
                entries.add(stack.toString());
            }
        }
        if (!entries.isEmpty()) {
            BlockInfo blockInfo = BlockInfo.newInstance()
                    .setMainThreadTimeCost(startWall, endWall, startThread, endThread)
                    .setThreadStackEntries(entries)
                    .flushString();
            BlockMonitorManager.getInstance().notifyBlockEvent(blockInfo);
        }
    }

    @Override
    public void println(String x) {
        if (!mPrintingStarted) {
            mStartTime = System.currentTimeMillis();
            mStartThreadTime = SystemClock.currentThreadTimeMillis();
            mPrintingStarted = true;
            mStackSampler.startDump();
        } else {
            final long endTime = System.currentTimeMillis();
            long endThreadTime = SystemClock.currentThreadTimeMillis();
            mPrintingStarted = false;
            if (isBlock(endTime)) {
                final ArrayList<String> entries = mStackSampler.getThreadStackEntries(mStartTime, endTime);
                if (entries.size() > 0) {
                    final BlockInfo blockInfo = BlockInfo.newInstance()
                            .setMainThreadTimeCost(mStartTime, endTime, mStartThreadTime, endThreadTime)
                            .setThreadStackEntries(entries)
                            .flushString();
                    BlockMonitorManager.getInstance().notifyBlockEvent(blockInfo);
                }
            }
            mStackSampler.stopDump();
        }
    }

    private boolean isBlock(long endTime) {
        return endTime - mStartTime > BLOCK_THRESHOLD_MILLIS;
    }

    void shutDown() {
        stopFrameMonitor();
        mPrintingStarted = false;
        mStackSampler.shutDown();
    }
}
