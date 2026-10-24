package dev.wsldocker.ui.dialog;

import dev.wsldocker.core.AppSession;
import dev.wsldocker.ui.BusySupport;
import dev.wsldocker.ui.StatusBar;

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

public final class ExecDialog extends JDialog {
    public ExecDialog(Frame owner, AppSession session, String containerId, String containerName, StatusBar statusBar) {
        super(owner, "exec - " + containerName, true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JTextField command = new JTextField("uname -a && pwd");
        command.putClientProperty("JTextField.placeholderText", "コンテナ内で実行するコマンド");
        JTextArea output = new JTextArea();
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JButton run = new JButton("実行");
        run.addActionListener(e -> {
            String text = command.getText().trim();
            if (text.isEmpty()) {
                return;
            }
            output.setText("実行中...\n");
            BusySupport.supply(this, statusBar, "exec 実行中...",
                    () -> session.docker().exec(session.requireDistro(), containerId, text),
                    result -> {
                        output.setText(result.isBlank() ? "(出力なし)\n" : result);
                        output.setCaretPosition(0);
                    });
        });
        JButton close = new JButton("閉じる");
        close.addActionListener(e -> dispose());

        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.add(new JLabel("コマンド (非対話 / sh -lc)"), BorderLayout.WEST);
        form.add(command, BorderLayout.CENTER);
        form.add(run, BorderLayout.EAST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(close);

        setLayout(new BorderLayout(8, 8));
        add(form, BorderLayout.NORTH);
        add(new JScrollPane(output), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(760, 480));
        pack();
        setLocationRelativeTo(owner);
        command.requestFocusInWindow();
    }
}
