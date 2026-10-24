package dev.wsldocker.ui.table;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class Tables {
    private Tables() {
    }

    public static JTable create() {
        JTable table = new JTable();
        table.setAutoCreateRowSorter(false);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    public static <T> List<T> selected(JTable table, List<T> rows) {
        List<T> selected = new ArrayList<>();
        for (int viewRow : table.getSelectedRows()) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            if (modelRow >= 0 && modelRow < rows.size()) {
                selected.add(rows.get(modelRow));
            }
        }
        return selected;
    }

    public static <T> T single(JTable table, List<T> rows) {
        List<T> selected = selected(table, rows);
        return selected.size() == 1 ? selected.getFirst() : null;
    }

    public static void copy(JTable table, Function<Integer, String> modelRowText) {
        StringBuilder builder = new StringBuilder();
        for (int viewRow : table.getSelectedRows()) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            if (builder.length() > 0) {
                builder.append(System.lineSeparator());
            }
            builder.append(modelRowText.apply(modelRow));
        }
        if (builder.isEmpty()) {
            return;
        }
        java.awt.Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(builder.toString()), null);
    }
}
