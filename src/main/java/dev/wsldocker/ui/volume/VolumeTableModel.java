package dev.wsldocker.ui.volume;

import dev.wsldocker.model.VolumeInfo;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public final class VolumeTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"名前", "ドライバ", "マウントポイント"};
    private final List<VolumeInfo> rows = new ArrayList<>();

    public void setRows(List<VolumeInfo> volumes) {
        rows.clear();
        rows.addAll(volumes);
        fireTableDataChanged();
    }

    public List<VolumeInfo> rows() {
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
        VolumeInfo row = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> row.name();
            case 1 -> row.driver();
            case 2 -> row.mountpoint();
            default -> "";
        };
    }
}
