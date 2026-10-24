package dev.wsldocker.ui.volume;

import dev.wsldocker.core.AppSession;
import dev.wsldocker.model.VolumeInfo;
import dev.wsldocker.ui.BusySupport;
import dev.wsldocker.ui.Dialogs;
import dev.wsldocker.ui.StatusBar;
import dev.wsldocker.ui.dialog.InspectDialog;
import dev.wsldocker.ui.table.FilterableTable;
import dev.wsldocker.ui.table.Tables;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.List;

public final class VolumePanel extends JPanel {
    private final AppSession session;
    private final StatusBar statusBar;
    private final VolumeTableModel model = new VolumeTableModel();
    private final JTable table = Tables.create();
    private final JButton create = new JButton("作成");
    private final JButton inspect = new JButton("inspect");
    private final JButton remove = new JButton("削除");

    public VolumePanel(AppSession session, StatusBar statusBar) {
        this.session = session;
        this.statusBar = statusBar;
        setLayout(new BorderLayout(0, 8));
        table.setModel(model);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        create.addActionListener(e -> createVolume());
        inspect.addActionListener(e -> openInspect());
        remove.addActionListener(e -> removeSelected());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.add(create);
        toolbar.add(inspect);
        toolbar.add(remove);
        add(toolbar, BorderLayout.NORTH);
        add(FilterableTable.wrap(table, "ボリュームをフィルタ"), BorderLayout.CENTER);
        updateButtons();
    }

    public void reload() {
        if (!session.connected()) {
            model.setRows(List.of());
            return;
        }
        BusySupport.supply(this, statusBar, "ボリュームを読み込み中...",
                () -> session.docker().listVolumes(session.requireDistro()),
                rows -> {
                    model.setRows(rows);
                    statusBar.setMessage("ボリューム " + rows.size() + " 件");
                    updateButtons();
                });
    }

    private void createVolume() {
        String name = Dialogs.prompt(this, "ボリューム作成", "ボリューム名", "");
        if (name == null || name.isBlank()) {
            return;
        }
        BusySupport.run(this, statusBar, "ボリューム作成中...",
                () -> session.docker().createVolume(session.requireDistro(), name.trim()),
                this::reload);
    }

    private void openInspect() {
        VolumeInfo row = Tables.single(table, model.rows());
        if (row == null) {
            return;
        }
        BusySupport.supply(this, statusBar, "inspect 中...",
                () -> session.docker().inspect(session.requireDistro(), "volume", row.name()),
                json -> new InspectDialog(owner(), "inspect - " + row.name(), json).setVisible(true));
    }

    private void removeSelected() {
        List<VolumeInfo> selected = Tables.selected(table, model.rows());
        if (selected.isEmpty()) {
            return;
        }
        String names = selected.stream().map(VolumeInfo::name).reduce((a, b) -> a + ", " + b).orElse("");
        if (!Dialogs.confirm(this, names + " を削除しますか？")) {
            return;
        }
        BusySupport.run(this, statusBar, "ボリューム削除中...",
                () -> session.docker().removeVolumes(
                        session.requireDistro(), selected.stream().map(VolumeInfo::name).toList()),
                this::reload);
    }

    private void updateButtons() {
        List<VolumeInfo> selected = Tables.selected(table, model.rows());
        inspect.setEnabled(selected.size() == 1);
        remove.setEnabled(!selected.isEmpty());
    }

    private Frame owner() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }
}
