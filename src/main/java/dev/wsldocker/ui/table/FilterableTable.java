package dev.wsldocker.ui.table;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.util.Locale;

public final class FilterableTable {
    private FilterableTable() {
    }

    public static JPanel wrap(JTable table, String placeholder) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        JTextField search = new JTextField();
        search.putClientProperty("JTextField.placeholderText", placeholder);
        search.putClientProperty("JTextField.showClearButton", Boolean.TRUE);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                apply();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                apply();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                apply();
            }

            private void apply() {
                String query = search.getText().trim().toLowerCase(Locale.ROOT);
                if (query.isEmpty()) {
                    sorter.setRowFilter(null);
                    return;
                }
                sorter.setRowFilter(new RowFilter<>() {
                    @Override
                    public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                        for (int i = 0; i < entry.getValueCount(); i++) {
                            Object value = entry.getValue(i);
                            if (value != null && value.toString().toLowerCase(Locale.ROOT).contains(query)) {
                                return true;
                            }
                        }
                        return false;
                    }
                });
            }
        });

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(search, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }
}
