package dev.wsldocker.ui.container;

import dev.wsldocker.core.AppSession;
import dev.wsldocker.model.ContainerInfo;
import dev.wsldocker.ui.BusySupport;
import dev.wsldocker.ui.Dialogs;
import dev.wsldocker.ui.StatusBar;
import dev.wsldocker.ui.dialog.ExecDialog;
import dev.wsldocker.ui.dialog.InspectDialog;
import dev.wsldocker.ui.dialog.LogDialog;
import dev.wsldocker.ui.table.FilterableTable;
import dev.wsldocker.ui.table.StateCellRenderer;
import dev.wsldocker.ui.table.Tables;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

public final class ContainerPanel extends JPanel {
    private final AppSession session;
    private final StatusBar statusBar;
    private final ContainerTableModel model = new ContainerTableModel();
    private final JTable table = Tables.create();
    private final JButton start = new JButton("起動");
    private final JButton stop = new JButton("停止");
    private final JButton restart = new JButton("再起動");
    private final JButton remove = new JButton("削除");
    private final JButton logs = new JButton("ログ");
    private final JButton inspect = new JButton("inspect");
    private final JButton exec = new JButton("exec");

    public ContainerPanel(AppSession session, StatusBar statusBar) {
        this.session = session;
        this.statusBar = statusBar;
        setLayout(new BorderLayout(0, 8));
        table.setModel(model);
        table.getColumnModel().getColumn(3).setCellRenderer(new StateCellRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(140);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());

        start.addActionListener(e -> act("起動中...", ids -> session.docker().startContainers(session.requireDistro(), ids)));
        stop.addActionListener(e -> act("停止中...", ids -> session.docker().stopContainers(session.requireDistro(), ids)));
        restart.addActionListener(e -> act("再起動中...", ids -> session.docker().restartContainers(session.requireDistro(), ids)));
        remove.addActionListener(e -> removeSelected());
        logs.addActionListener(e -> openLogs());
        inspect.addActionListener(e -> openInspect());
        exec.addActionListener(e -> openExec());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.add(start);
        toolbar.add(stop);
        toolbar.add(restart);
        toolbar.add(remove);
        toolbar.add(logs);
        toolbar.add(inspect);
        toolbar.add(exec);

        add(toolbar, BorderLayout.NORTH);
        add(FilterableTable.wrap(table, "コンテナをフィルタ"), BorderLayout.CENTER);
        installPopup();
        updateButtons();
    }

    public void reload() {
        if (!session.connected()) {
            model.setRows(List.of());
            return;
        }
        BusySupport.supply(this, statusBar, "コンテナを読み込み中...",
                () -> session.docker().listContainers(session.requireDistro()),
                rows -> {
                    model.setRows(rows);
                    statusBar.setMessage("コンテナ " + rows.size() + " 件");
                    updateButtons();
                });
    }

    private void act(String message, Consumer<List<String>> action) {
        List<ContainerInfo> selected = Tables.selected(table, model.rows());
        if (selected.isEmpty()) {
            return;
        }
        List<String> ids = selected.stream().map(ContainerInfo::id).toList();
        BusySupport.run(this, statusBar, message, () -> action.accept(ids), this::reload);
    }

    private void removeSelected() {
        List<ContainerInfo> selected = Tables.selected(table, model.rows());
        if (selected.isEmpty()) {
            return;
        }
        boolean running = selected.stream().anyMatch(ContainerInfo::running);
        String names = selected.stream().map(ContainerInfo::name).reduce((a, b) -> a + ", " + b).orElse("");
        String confirm = running
                ? names + " を強制削除しますか？ 実行中のコンテナが含まれています。"
                : names + " を削除しますか？";
        if (!Dialogs.confirm(this, confirm)) {
            return;
        }
        List<String> ids = selected.stream().map(ContainerInfo::id).toList();
        BusySupport.run(this, statusBar, "削除中...",
                () -> session.docker().removeContainers(session.requireDistro(), ids, running),
                this::reload);
    }

    private void openLogs() {
        ContainerInfo row = Tables.single(table, model.rows());
        if (row == null) {
            return;
        }
        new LogDialog(owner(), session, row.id(), row.name()).setVisible(true);
    }

    private void openInspect() {
        ContainerInfo row = Tables.single(table, model.rows());
        if (row == null) {
            return;
        }
        BusySupport.supply(this, statusBar, "inspect 中...",
                () -> session.docker().inspect(session.requireDistro(), "container", row.id()),
                json -> new InspectDialog(owner(), "inspect - " + row.name(), json).setVisible(true));
    }

    private void openExec() {
        ContainerInfo row = Tables.single(table, model.rows());
        if (row == null) {
            return;
        }
        if (!row.running()) {
            Dialogs.info(this, "exec", "実行中のコンテナに対してのみ exec できます。");
            return;
        }
        new ExecDialog(owner(), session, row.id(), row.name(), statusBar).setVisible(true);
    }

    private void installPopup() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(item("起動", () -> start.doClick()));
        menu.add(item("停止", () -> stop.doClick()));
        menu.add(item("再起動", () -> restart.doClick()));
        menu.add(item("削除", () -> remove.doClick()));
        menu.addSeparator();
        menu.add(item("ログ", this::openLogs));
        menu.add(item("inspect", this::openInspect));
        menu.add(item("exec", this::openExec));
        menu.addSeparator();
        menu.add(item("ID をコピー", () -> Tables.copy(table, row -> model.rows().get(row).id())));
        menu.add(item("名前をコピー", () -> Tables.copy(table, row -> model.rows().get(row).name())));
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                show(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                show(e);
            }

            private void show(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && !table.isRowSelected(row)) {
                    table.setRowSelectionInterval(row, row);
                }
                menu.show(table, e.getX(), e.getY());
            }
        });
    }

    private static JMenuItem item(String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        return item;
    }

    private void updateButtons() {
        List<ContainerInfo> selected = Tables.selected(table, model.rows());
        boolean any = !selected.isEmpty();
        boolean one = selected.size() == 1;
        start.setEnabled(any && selected.stream().anyMatch(ContainerInfo::startable));
        stop.setEnabled(any && selected.stream().anyMatch(ContainerInfo::stoppable));
        restart.setEnabled(any);
        remove.setEnabled(any);
        logs.setEnabled(one);
        inspect.setEnabled(one);
        exec.setEnabled(one && selected.getFirst().running());
    }

    private Frame owner() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }
}
