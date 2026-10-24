package dev.wsldocker.ui.network;

import dev.wsldocker.model.NetworkInfo;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public final class NetworkTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"名前", "ドライバ", "スコープ", "ID"};
    private final List<NetworkInfo> rows = new ArrayList<>();

    public void setRows(List<NetworkInfo> networks) {
        rows.clear();
        rows.addAll(networks);
        fireTableDataChanged();
    }

    public List<NetworkInfo> rows() {
        return List.copyOf(rows);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        NetworkInfo row = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> row.name();
            case 1 -> row.driver();
            case 2 -> row.scope();
            case 3 -> row.id();
            default -> "";
        };
    }
}
