package dev.wsldocker.ui;

import java.awt.Component;
import java.awt.Cursor;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class BusySupport {
    private BusySupport() {
    }

    public static <T> void supply(
            Component host,
            StatusBar statusBar,
            String busyMessage,
            Supplier<T> task,
            Consumer<T> onSuccess
    ) {
        host.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        statusBar.setMessage(busyMessage);
        UiExecutor.supply(task, value -> {
            host.setCursor(Cursor.getDefaultCursor());
            statusBar.setMessage("準備完了");
            onSuccess.accept(value);
        }, error -> {
            host.setCursor(Cursor.getDefaultCursor());
            statusBar.setMessage("エラー");
            Dialogs.error(host, error);
        });
    }

    public static void run(
            Component host,
            StatusBar statusBar,
            String busyMessage,
            Runnable task,
            Runnable onSuccess
    ) {
        supply(host, statusBar, busyMessage, () -> {
            task.run();
            return Boolean.TRUE;
        }, ignored -> onSuccess.run());
    }
}
