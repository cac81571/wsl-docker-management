package dev.wsldocker.ui.network;

import dev.wsldocker.core.AppSession;
import dev.wsldocker.model.NetworkInfo;
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

public final class NetworkPanel extends JPanel {
    private final AppSession session;
    private final StatusBar statusBar;
    private final NetworkTableModel model = new NetworkTableModel();
    private final JTable table = Tables.create();
    private final JButton create = new JButton("作成");
    private final JButton inspect = new JButton("inspect");
    private final JButton remove = new JButton("削除");

    public NetworkPanel(AppSession session, StatusBar statusBar) {
        this.session = session;
        this.statusBar = statusBar;
        setLayout(new BorderLayout(0, 8));
        table.setModel(model);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        create.addActionListener(e -> createNetwork());
        inspect.addActionListener(e -> openInspect());
        remove.addActionListener(e -> removeSelected());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.add(create);
        toolbar.add(inspect);
        toolbar.add(remove);
        add(toolbar, BorderLayout.NORTH);
        add(FilterableTable.wrap(table, "ネットワークをフィルタ"), BorderLayout.CENTER);
        updateButtons();
    }

    public void reload() {
        if (!session.connected()) {
            model.setRows(List.of());
            return;
        }
        BusySupport.supply(this, statusBar, "ネットワークを読み込み中...",
                () -> session.docker().listNetworks(session.requireDistro()),
                rows -> {
                    model.setRows(rows);
                    statusBar.setMessage("ネットワーク " + rows.size() + " 件");
                    updateButtons();
                });
    }

    private void createNetwork() {
        String name = Dialogs.prompt(this, "ネットワーク作成", "ネットワーク名 (driver=bridge)", "");
        if (name == null || name.isBlank()) {
            return;
        }
        BusySupport.run(this, statusBar, "ネットワーク作成中...",
                () -> session.docker().createNetwork(session.requireDistro(), name.trim(), "bridge"),
                this::reload);
    }

    private void openInspect() {
        NetworkInfo row = Tables.single(table, model.rows());
        if (row == null) {
            return;
        }
        BusySupport.supply(this, statusBar, "inspect 中...",
                () -> session.docker().inspect(session.requireDistro(), "network", row.id()),
                json -> new InspectDialog(owner(), "inspect - " + row.name(), json).setVisible(true));
    }

    private void removeSelected() {
        List<NetworkInfo> selected = Tables.selected(table, model.rows());
        if (selected.isEmpty()) {
            return;
        }
        if (selected.stream().anyMatch(NetworkInfo::builtIn)) {
            Dialogs.info(this, "削除できません", "bridge / host / none は削除できません。");
            return;
        }
        String names = selected.stream().map(NetworkInfo::name).reduce((a, b) -> a + ", " + b).orElse("");
        if (!Dialogs.confirm(this, names + " を削除しますか？")) {
            return;
        }
        BusySupport.run(this, statusBar, "ネットワーク削除中...",
                () -> session.docker().removeNetworks(
                        session.requireDistro(), selected.stream().map(NetworkInfo::id).toList()),
                this::reload);
    }

    private void updateButtons() {
        List<NetworkInfo> selected = Tables.selected(table, model.rows());
        inspect.setEnabled(selected.size() == 1);
        remove.setEnabled(!selected.isEmpty() && selected.stream().noneMatch(NetworkInfo::builtIn));
    }

    private Frame owner() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }
}
