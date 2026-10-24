package dev.wsldocker.ui.image;

import dev.wsldocker.model.ImageInfo;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public final class ImageTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"リポジトリ", "タグ", "ID", "サイズ", "作成"};
    private final List<ImageInfo> rows = new ArrayList<>();

    public void setRows(List<ImageInfo> images) {
        rows.clear();
        rows.addAll(images);
        fireTableDataChanged();
    }

    public List<ImageInfo> rows() {
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
        ImageInfo row = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> row.repository();
            case 1 -> row.tag();
            case 2 -> row.id();
            case 3 -> row.size();
            case 4 -> row.createdSince().isBlank() ? row.createdAt() : row.createdSince();
            default -> "";
        };
    }
}
