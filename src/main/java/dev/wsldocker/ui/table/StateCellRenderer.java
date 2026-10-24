package dev.wsldocker.ui.table;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.util.Locale;

public final class StateCellRenderer extends DefaultTableCellRenderer {
    public StateCellRenderer() {
        setHorizontalAlignment(SwingConstants.CENTER);
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column
    ) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String state = value == null ? "" : value.toString();
        label.setText(state);
        Color background = colorFor(state);
        label.setBackground(background);
        label.setForeground(Color.WHITE);
        return label;
    }

    private static Color colorFor(String state) {
        String normalized = state.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("running")) {
            return new Color(0x2E7D4F);
        }
        if (normalized.startsWith("exited") || normalized.startsWith("dead")) {
            return new Color(0x8B3A3A);
        }
        if (normalized.startsWith("created") || normalized.startsWith("paused")) {
            return new Color(0x8A6D1B);
        }
        if (normalized.startsWith("restarting")) {
            return new Color(0x2F5F8A);
        }
        return new Color(0x4A5563);
    }
}
