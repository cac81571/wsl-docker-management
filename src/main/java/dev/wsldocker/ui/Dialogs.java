package dev.wsldocker.ui;

import dev.wsldocker.core.AppException;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Dimension;

public final class Dialogs {
    private Dialogs() {
    }

    public static void error(Component parent, Throwable error) {
        String message = messageOf(error);
        JTextArea area = new JTextArea(message);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(520, Math.min(280, 40 + message.length() / 2)));
        JOptionPane.showMessageDialog(parent, scroll, "エラー", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(
                parent, message, "確認", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)
                == JOptionPane.OK_OPTION;
    }

    public static String prompt(Component parent, String title, String message, String initial) {
        Object value = JOptionPane.showInputDialog(
                parent, message, title, JOptionPane.PLAIN_MESSAGE, null, null, initial);
        return value == null ? null : value.toString();
    }

    public static String messageOf(Throwable error) {
        if (error instanceof AppException && error.getMessage() != null) {
            return error.getMessage();
        }
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return error.getClass().getSimpleName() + ": " + message;
    }
}
