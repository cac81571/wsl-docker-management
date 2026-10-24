package dev.wsldocker.ui.compose;

import dev.wsldocker.model.ComposeProject;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public final class ComposeTableModel extends AbstractTableModel {
    public static final class Row {
        private ComposeProject project;
        private String status;

        Row(ComposeProject project, String status) {
            this.project = project;
            this.status = status;
        }

        public ComposeProject project() {
            return project;
        }

        public String status() {
            return status;
        }
    }

    private static final String[] COLUMNS = {"名前", "ファイル", "作業ディレクトリ", "状態"};
    private final List<Row> rows = new ArrayList<>();

    public void setProjects(List<ComposeProject> projects) {
        rows.clear();
        for (ComposeProject project : projects) {
            rows.add(new Row(project, "-"));
        }
        fireTableDataChanged();
    }

    public void add(ComposeProject project) {
        rows.add(new Row(project, "-"));
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }

    public void update(int index, ComposeProject project) {
        rows.get(index).project = project;
        fireTableRowsUpdated(index, index);
    }

    public void remove(int index) {
        rows.remove(index);
        fireTableRowsDeleted(index, index);
    }

    public void setStatus(int index, String status) {
        rows.get(index).status = status == null || status.isBlank() ? "-" : status;
        fireTableRowsUpdated(index, index);
    }

    public void clearStatuses() {
        for (Row row : rows) {
            row.status = "-";
        }
        if (!rows.isEmpty()) {
            fireTableRowsUpdated(0, rows.size() - 1);
        }
    }

    public List<Row> rows() {
        return List.copyOf(rows);
    }

    public List<ComposeProject> projects() {
        return rows.stream().map(Row::project).toList();
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
        Row row = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> row.project.name();
            case 1 -> row.project.filePath();
            case 2 -> row.project.projectDir();
            case 3 -> row.status;
            default -> "";
        };
    }
}
