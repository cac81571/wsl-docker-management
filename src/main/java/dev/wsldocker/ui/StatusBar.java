package dev.wsldocker.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

public final class StatusBar extends JPanel {
    private final JLabel message = new JLabel("起動中...");

    public StatusBar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 12));
        message.setHorizontalAlignment(SwingConstants.LEFT);
        add(message, BorderLayout.CENTER);
    }

    public void setMessage(String text) {
        message.setText(text);
    }
}
