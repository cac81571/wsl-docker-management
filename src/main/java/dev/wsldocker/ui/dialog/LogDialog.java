package dev.wsldocker.ui.dialog;

import dev.wsldocker.core.AppSession;
import dev.wsldocker.service.RunningProcess;
import dev.wsldocker.ui.Dialogs;
import dev.wsldocker.ui.UiExecutor;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
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

public final class LogDialog extends JDialog {
    private final AppSession session;
    private final String containerId;
    private final String containerName;
    private final JTextArea area = new JTextArea();
    private final JCheckBox follow = new JCheckBox("追従");
    private RunningProcess running;

    public LogDialog(Frame owner, AppSession session, String containerId, String containerName) {
        super(owner, "ログ - " + containerName, false);
        this.session = session;
        this.containerId = containerId;
        this.containerName = containerName;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JButton refresh = new JButton("再読込");
        refresh.addActionListener(e -> loadSnapshot());
        JButton close = new JButton("閉じる");
        close.addActionListener(e -> dispose());
        follow.addActionListener(e -> {
            if (follow.isSelected()) {
                startFollow();
            } else {
                stopFollow();
            }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.add(new JLabel(containerName + "  (" + containerId + ")"));
        top.add(follow);
        top.add(refresh);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(close);

        setLayout(new BorderLayout(8, 8));
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(area), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(860, 560));
        pack();
        setLocationRelativeTo(owner);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                stopFollow();
            }
        });
        loadSnapshot();
    }

    private void loadSnapshot() {
        stopFollow();
        follow.setSelected(false);
        area.setText("読み込み中...\n");
        UiExecutor.supply(
                () -> session.docker().logs(session.requireDistro(), containerId, 400),
                text -> {
                    area.setText(text.isBlank() ? "(ログはありません)\n" : text);
                    area.setCaretPosition(area.getDocument().getLength());
                },
                error -> Dialogs.error(this, error)
        );
    }

    private void startFollow() {
        stopFollow();
        area.setText("");
        try {
            running = session.docker().followLogs(session.requireDistro(), containerId, 200, line ->
                    UiExecutor.later(() -> {
                        area.append(line);
                        area.append("\n");
                        area.setCaretPosition(area.getDocument().getLength());
                    }));
        } catch (Exception e) {
            follow.setSelected(false);
            Dialogs.error(this, e);
        }
    }

    private void stopFollow() {
        if (running != null) {
            running.cancel();
            running = null;
        }
    }
}
