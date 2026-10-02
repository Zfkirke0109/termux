package com.termux.app.terminal;

import android.content.Context;
import android.os.Build;
import android.system.Os;
import android.system.OsConstants;
import com.termux.shared.shell.ShellUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Own-UID commands only. Call on BACKGROUND, never on the main thread. */
public final class MobileCommandRunner {
    public static final ExecutorService BACKGROUND = Executors.newSingleThreadExecutor();
    private static final ExecutorService OUTPUT = Executors.newSingleThreadExecutor();
    private static final ScheduledExecutorService DEADLINES = Executors.newSingleThreadScheduledExecutor();
    private static final int OUTPUT_LIMIT = 65536;
    private MobileCommandRunner() {}

    public static final class Result {
        public final int exitCode;
        public final String output;
        public final boolean timedOut, truncated;
        Result(int code, String output, boolean timedOut, boolean truncated) {
            exitCode = code; this.output = output; this.timedOut = timedOut; this.truncated = truncated;
        }
    }

    public static Result run(Context context, List<String> arguments) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(arguments);
        builder.directory(new java.io.File(TermuxConstants.TERMUX_HOME_DIR_PATH));
        Map<String, String> environment = builder.environment();
        environment.clear();
        environment.putAll(new TermuxShellEnvironment().getEnvironment(context, false));
        builder.redirectErrorStream(true);
        Process process = builder.start();
        AtomicBoolean timedOut = new AtomicBoolean(), truncated = new AtomicBoolean();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        InputStream stream = process.getInputStream();
        ScheduledFuture<?> deadline = DEADLINES.schedule(() -> {
            timedOut.set(true);
            try {
                process.exitValue(); // If it exited, only close a pipe inherited by descendants.
            } catch (IllegalThreadStateException running) {
                if (Build.VERSION.SDK_INT >= 26) process.destroyForcibly();
                else {
                    int pid = ShellUtils.getPid(process);
                    if (pid > 0) {
                        try { Os.kill(pid, OsConstants.SIGKILL); } catch (Exception ignored) {}
                    }
                    process.destroy();
                }
            }
            try { stream.close(); } catch (IOException ignored) {}
        }, 8, TimeUnit.SECONDS);
        Future<?> reader = OUTPUT.submit(() -> {
            byte[] buffer = new byte[4096];
            try {
                int length;
                while ((length = stream.read(buffer)) != -1) {
                    int keep = Math.min(length, OUTPUT_LIMIT - bytes.size());
                    if (keep > 0) bytes.write(buffer, 0, keep);
                    if (keep != length) truncated.set(true);
                }
            } catch (IOException ignored) { /* watchdog or process exit */ }
        });
        try {
            process.getOutputStream().close();
            int code = process.waitFor();
            // Keep the deadline armed while descendants may still hold the pipe open.
            reader.get(9, TimeUnit.SECONDS);
            return new Result(code, bytes.toString("UTF-8"), timedOut.get(), truncated.get());
        } finally {
            deadline.cancel(false);
            process.destroy();
            stream.close();
            reader.cancel(true);
        }
    }
}
