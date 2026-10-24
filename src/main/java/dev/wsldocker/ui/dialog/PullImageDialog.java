package dev.wsldocker.ui.dialog;

import dev.wsldocker.core.AppSession;
import dev.wsldocker.service.RunningProcess;
import dev.wsldocker.ui.Dialogs;
import dev.wsldocker.ui.UiExecutor;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class PullImageDialog extends JDialog {
    private RunningProcess running;

    public PullImageDialog(Frame owner, AppSession session, Runnable onFinished) {
        super(owner, "イメージを pull", true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JTextField reference = new JTextField("alpine:latest");
        reference.putClientProperty("JTextField.placeholderText", "例: nginx:1.27-alpine");
        JTextArea output = new JTextArea();
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JButton pull = new JButton("pull");
        JButton close = new JButton("閉じる");
        close.addActionListener(e -> dispose());

        pull.addActionListener(e -> {
            String ref = reference.getText().trim();
            if (ref.isEmpty()) {
                return;
            }
            stop();
            output.setText("pull " + ref + "\n");
            pull.setEnabled(false);
            try {
                running = session.docker().pull(session.requireDistro(), ref, line ->
                        UiExecutor.later(() -> {
                            output.append(line);
                            output.append("\n");
                            output.setCaretPosition(output.getDocument().getLength());
                        }));
                Thread waiter = new Thread(() -> {
                    while (running != null && running.alive()) {
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    UiExecutor.later(() -> {
                        pull.setEnabled(true);
                        output.append("\n完了\n");
                        onFinished.run();
                    });
                }, "docker-pull-wait");
                waiter.setDaemon(true);
                waiter.start();
            } catch (Exception ex) {
                pull.setEnabled(true);
                Dialogs.error(this, ex);
            }
        });

        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.add(new JLabel("参照"), BorderLayout.WEST);
        form.add(reference, BorderLayout.CENTER);
        form.add(pull, BorderLayout.EAST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(close);

        setLayout(new BorderLayout(8, 8));
        add(form, BorderLayout.NORTH);
        add(new JScrollPane(output), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(720, 420));
        pack();
        setLocationRelativeTo(owner);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                stop();
            }
        });
    }

    private void stop() {
        if (running != null) {
            running.cancel();
            running = null;
        }
    }
}
