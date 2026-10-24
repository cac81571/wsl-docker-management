package dev.wsldocker.ui.container;

import dev.wsldocker.model.ContainerInfo;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public final class ContainerTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"名前", "ID", "イメージ", "状態", "ステータス", "ポート", "作成"};
    private final List<ContainerInfo> rows = new ArrayList<>();

    public void setRows(List<ContainerInfo> containers) {
        rows.clear();
        rows.addAll(containers);
        fireTableDataChanged();
    }

    public List<ContainerInfo> rows() {
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
        ContainerInfo row = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> row.name();
            case 1 -> row.id();
            case 2 -> row.image();
            case 3 -> row.state();
            case 4 -> row.status();
            case 5 -> row.ports();
            case 6 -> row.createdAt();
            default -> "";
        };
    }
}
