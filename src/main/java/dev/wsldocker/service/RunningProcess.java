package dev.wsldocker.service;

public final class RunningProcess implements AutoCloseable {
    private final Process process;

    RunningProcess(Process process) {
        this.process = process;
    }

    public boolean alive() {
        return process.isAlive();
    }

    public int join() {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cancel();
            return -1;
        }
    }

    public void cancel() {
        process.destroy();
        if (process.isAlive()) {
            process.destroyForcibly();
        }
    }

    @Override
    public void close() {
        cancel();
    }
}
