package dev.wsldocker.ui.dialog;

import dev.wsldocker.core.AppException;
import dev.wsldocker.service.RunningProcess;
import dev.wsldocker.ui.Dialogs;
import dev.wsldocker.ui.UiExecutor;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;
import java.util.function.Function;

public final class CommandStreamDialog extends JDialog {
    private final JTextArea output = new JTextArea();
    private volatile boolean cancelled;
    private RunningProcess running;

    public CommandStreamDialog(
            Frame owner,
            String title,
            String heading,
            Function<Consumer<String>, RunningProcess> starter,
            Runnable onSuccess
    ) {
        super(owner, title, true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        output.setText(heading + "\n\n");

        JButton close = new JButton("閉じる");
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(close);

        setLayout(new BorderLayout(8, 8));
        add(new JScrollPane(output), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(760, 440));
        pack();
        setLocationRelativeTo(owner);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                cancelled = true;
                stop();
            }
        });

        try {
            running = starter.apply(line -> UiExecutor.later(() -> append(line)));
        } catch (Exception e) {
            Dialogs.error(this, e);
            return;
        }

        Thread waiter = new Thread(() -> {
            int code = running.join();
            UiExecutor.later(() -> {
                if (cancelled) {
                    return;
                }
                if (code == 0) {
                    append("完了");
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                } else {
                    append("失敗 (exit " + code + ")");
                    Dialogs.error(this, new AppException("コマンドが失敗しました (exit " + code + ")"));
                }
            });
        }, "command-stream-wait");
        waiter.setDaemon(true);
        waiter.start();
    }

    private void append(String line) {
        output.append(line);
        output.append("\n");
        output.setCaretPosition(output.getDocument().getLength());
    }

    private void stop() {
        if (running != null) {
            running.cancel();
        }
    }
}
