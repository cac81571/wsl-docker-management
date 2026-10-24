package dev.wsldocker.service;

import dev.wsldocker.core.AppException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ProcessRunner {
    public CommandResult run(List<String> command, Duration timeout) {
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new AppException("コマンドを起動できません: " + String.join(" ", command) + "\n" + e.getMessage(), e);
        }

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        Thread outThread = drain(process.getInputStream(), stdout);
        Thread errThread = drain(process.getErrorStream(), stderr);

        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new AppException("コマンドが中断されました: " + String.join(" ", command), e);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new AppException("コマンドがタイムアウトしました (" + timeout.toSeconds() + " 秒): "
                    + String.join(" ", command));
        }

        joinQuietly(outThread);
        joinQuietly(errThread);
        return new CommandResult(
                process.exitValue(),
                decode(stdout.toByteArray()),
                decode(stderr.toByteArray()),
                List.copyOf(command)
        );
    }

    public RunningProcess start(List<String> command, Consumer<String> onLine) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new AppException("コマンドを起動できません: " + String.join(" ", command) + "\n" + e.getMessage(), e);
        }

        Thread reader = new Thread(() -> {
            try (BufferedReader buffered = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = buffered.readLine()) != null) {
                    onLine.accept(line);
                }
            } catch (IOException ignored) {
                // キャンセルやプロセス終了で発生する
            }
        }, "wsl-docker-stream");
        reader.setDaemon(true);
        reader.start();
        return new RunningProcess(process);
    }

    private static Thread drain(InputStream in, ByteArrayOutputStream sink) {
        Thread thread = new Thread(() -> {
            try {
                in.transferTo(sink);
            } catch (IOException ignored) {
                // プロセス終了で発生する
            }
        }, "wsl-docker-drain");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void joinQuietly(Thread thread) {
        try {
            thread.join(2_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static String decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            return new String(bytes, StandardCharsets.UTF_16LE);
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
            return new String(bytes, StandardCharsets.UTF_16BE);
        }
        if (looksLikeUtf16Le(bytes)) {
            return new String(bytes, StandardCharsets.UTF_16LE);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static boolean looksLikeUtf16Le(byte[] bytes) {
        int nuls = 0;
        int pairs = 0;
        int limit = Math.min(bytes.length, 200);
        for (int i = 1; i < limit; i += 2) {
            pairs++;
            if (bytes[i] == 0) {
                nuls++;
            }
        }
        return pairs >= 8 && nuls >= pairs * 0.6;
    }
}
