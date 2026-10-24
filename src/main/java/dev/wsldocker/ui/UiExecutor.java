package dev.wsldocker.ui;

import javax.swing.SwingUtilities;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class UiExecutor {
    private UiExecutor() {
    }

    public static void later(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    public static <T> void supply(Supplier<T> task, Consumer<T> onSuccess, Consumer<Exception> onError) {
        Thread thread = new Thread(() -> {
            try {
                T value = task.get();
                later(() -> onSuccess.accept(value));
            } catch (Exception e) {
                later(() -> onError.accept(e));
            }
        }, "wsl-docker-bg");
        thread.setDaemon(true);
        thread.start();
    }

    public static void run(Runnable task, Runnable onSuccess, Consumer<Exception> onError) {
        supply(() -> {
            task.run();
            return Boolean.TRUE;
        }, ignored -> onSuccess.run(), onError);
    }
}
