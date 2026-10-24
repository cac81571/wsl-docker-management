package dev.wsldocker.ui.dialog;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.Font;

public final class InspectDialog extends JDialog {
    public InspectDialog(Frame owner, String title, String json) {
        super(owner, title, true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JTextArea area = new JTextArea(json);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setCaretPosition(0);

        JButton copy = new JButton("コピー");
        copy.addActionListener(e -> Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(area.getText()), null));
        JButton close = new JButton("閉じる");
        close.addActionListener(e -> dispose());

        JPanel buttons = new JPanel();
        buttons.add(copy);
        buttons.add(close);

        setLayout(new BorderLayout(8, 8));
        add(new JScrollPane(area), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(760, 560));
        pack();
        setLocationRelativeTo(owner);
    }
}
