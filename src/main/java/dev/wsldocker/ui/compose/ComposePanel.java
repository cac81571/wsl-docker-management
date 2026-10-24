package dev.wsldocker.ui.compose;

import dev.wsldocker.core.AppException;
import dev.wsldocker.core.AppSession;
import dev.wsldocker.core.SettingsStore;
import dev.wsldocker.model.ComposeListing;
import dev.wsldocker.model.ComposeProject;
import dev.wsldocker.service.ComposePaths;
import dev.wsldocker.ui.BusySupport;
import dev.wsldocker.ui.Dialogs;
import dev.wsldocker.ui.StatusBar;
import dev.wsldocker.ui.dialog.ComposeProjectDialog;
import dev.wsldocker.ui.table.FilterableTable;
import dev.wsldocker.ui.table.StateCellRenderer;
import dev.wsldocker.ui.table.Tables;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ComposePanel extends JPanel {
    private final AppSession session;
    private final StatusBar statusBar;
    private final SettingsStore settings;
    private final ComposeTableModel model = new ComposeTableModel();
    private final JTable table = Tables.create();
    private final JTextArea output = new JTextArea();
    private final JButton edit = new JButton("編集");
    private final JButton remove = new JButton("削除");
    private final JButton up = new JButton("up -d");
    private final JButton start = new JButton("start");
    private final JButton stop = new JButton("stop");
    private final JButton restart = new JButton("restart");
    private final JButton down = new JButton("down");
    private final JButton ps = new JButton("ps");
    private final JButton logs = new JButton("logs");
    private boolean triedAutoDiscover;

    public ComposePanel(AppSession session, StatusBar statusBar, SettingsStore settings) {
        this.session = session;
        this.statusBar = statusBar;
        this.settings = settings;
        setLayout(new BorderLayout(0, 8));

        table.setModel(model);
        table.getColumnModel().getColumn(3).setCellRenderer(new StateCellRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(420);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    editSelected();
                }
            }
        });

        JButton add = new JButton("追加...");
        JButton browse = new JButton("参照して追加");
        JButton discover = new JButton("検出して追加");
        add.addActionListener(e -> addManually());
        browse.addActionListener(e -> addFromFileChooser());
        discover.addActionListener(e -> discoverProjects(false));
        edit.addActionListener(e -> editSelected());
        remove.addActionListener(e -> removeSelected());
        up.addActionListener(e -> runCompose(Duration.ofMinutes(5), "up -d 実行中...", "up", "-d"));
        start.addActionListener(e -> runCompose(Duration.ofMinutes(1), "start 実行中...", "start"));
        stop.addActionListener(e -> runCompose(Duration.ofMinutes(1), "stop 実行中...", "stop"));
        restart.addActionListener(e -> runCompose(Duration.ofMinutes(2), "restart 実行中...", "restart"));
        down.addActionListener(e -> runCompose(Duration.ofMinutes(3), "down 実行中...", "down"));
        ps.addActionListener(e -> runCompose(Duration.ofSeconds(30), "ps 実行中...", "ps"));
        logs.addActionListener(e -> runCompose(Duration.ofSeconds(25), "logs 取得中...", "logs", "--tail", "200"));

        JPanel manage = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        manage.add(add);
        manage.add(browse);
        manage.add(discover);
        manage.add(edit);
        manage.add(remove);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.add(up);
        actions.add(start);
        actions.add(stop);
        actions.add(restart);
        actions.add(down);
        actions.add(ps);
        actions.add(logs);

        JPanel top = new JPanel(new BorderLayout());
        top.add(manage, BorderLayout.NORTH);
        top.add(FilterableTable.wrap(table, "Compose をフィルタ"), BorderLayout.CENTER);
        top.add(actions, BorderLayout.SOUTH);

        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        output.setText("""
                複数の compose ファイルを登録して、選択したプロジェクトに対して up / start / stop / restart / down / ps / logs できます。
                WSL 内のパスも使えます。空のときはエンジン上のプロジェクトを自動検出します。
                """);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, new JScrollPane(output));
        split.setResizeWeight(0.55);
        split.setDividerLocation(280);
        add(split, BorderLayout.CENTER);

        model.setProjects(settings.composeProjects());
        updateButtons();
    }

    public void reload() {
        if (!session.connected()) {
            model.clearStatuses();
            output.setText("先に WSL ディストリを接続してください。");
            return;
        }
        if (model.getRowCount() == 0 && !triedAutoDiscover) {
            triedAutoDiscover = true;
            discoverProjects(true);
            return;
        }
        refreshStatuses();
    }

    private void addManually() {
        ComposeProject created = ComposeProjectDialog.show(owner(), "Compose を追加", null);
        if (created == null) {
            return;
        }
        addProject(created, true);
    }

    private void addFromFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Compose ファイルを追加");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Compose (yml, yaml)", "yml", "yaml"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        int added = 0;
        for (java.io.File file : chooser.getSelectedFiles()) {
            if (addProject(ComposeProject.fromFile(file.getAbsolutePath()), false)) {
                added++;
            }
        }
        persist();
        statusBar.setMessage(added + " 件追加しました");
    }

    private void editSelected() {
        List<ComposeTableModel.Row> selected = Tables.selected(table, model.rows());
        if (selected.size() != 1) {
            return;
        }
        ComposeTableModel.Row row = selected.getFirst();
        int index = model.rows().indexOf(row);
        ComposeProject edited = ComposeProjectDialog.show(owner(), "Compose を編集", row.project());
        if (edited == null) {
            return;
        }
        try {
            ComposePaths.resolve(edited);
        } catch (AppException e) {
            Dialogs.error(this, e);
            return;
        }
        if (isDuplicate(edited, index)) {
            Dialogs.info(this, "Compose", "同じファイルは既に登録されています。");
            return;
        }
        model.update(index, uniqueName(edited, index));
        persist();
        refreshStatuses();
    }

    private void removeSelected() {
        List<ComposeTableModel.Row> selected = Tables.selected(table, model.rows());
        if (selected.isEmpty()) {
            return;
        }
        String names = selected.stream().map(row -> row.project().name()).reduce((a, b) -> a + ", " + b).orElse("");
        if (!Dialogs.confirm(this, names + " を一覧から外しますか？ コンテナは削除されません。")) {
            return;
        }
        List<ComposeTableModel.Row> remaining = new ArrayList<>(model.rows());
        remaining.removeAll(selected);
        model.setProjects(remaining.stream().map(ComposeTableModel.Row::project).toList());
        persist();
        updateButtons();
    }

    private void discoverProjects(boolean silent) {
        if (!session.connected()) {
            Dialogs.info(this, "Compose", "先に WSL ディストリを接続してください。");
            return;
        }
        BusySupport.supply(this, statusBar, "Compose プロジェクトを検出中...",
                () -> session.docker().listComposeProjects(session.requireDistro()),
                listings -> {
                    int added = 0;
                    for (ComposeListing listing : listings) {
                        List<String> files = listing.files();
                        if (files.isEmpty() && !listing.name().isBlank()) {
                            continue;
                        }
                        for (String file : files) {
                            ComposeProject project = new ComposeProject(
                                    listing.name().isBlank() ? ComposeProject.defaultName(file) : listing.name(),
                                    file,
                                    ""
                            );
                            if (addProject(project, false)) {
                                added++;
                            }
                        }
                    }
                    persist();
                    applyStatuses(listings);
                    if (silent && added == 0 && model.getRowCount() == 0) {
                        output.setText("エンジン上に compose プロジェクトは見つかりませんでした。「追加...」で登録できます。");
                    } else if (!silent) {
                        statusBar.setMessage(added + " 件を検出して追加しました");
                    } else if (added > 0) {
                        statusBar.setMessage("Compose プロジェクトを " + added + " 件検出しました");
                    }
                    updateButtons();
                });
    }

    private void refreshStatuses() {
        if (!session.connected()) {
            return;
        }
        BusySupport.supply(this, statusBar, "Compose の状態を更新中...",
                () -> session.docker().listComposeProjects(session.requireDistro()),
                listings -> {
                    applyStatuses(listings);
                    statusBar.setMessage("Compose " + model.getRowCount() + " 件");
                });
    }

    private void applyStatuses(List<ComposeListing> listings) {
        List<ComposeTableModel.Row> rows = model.rows();
        for (int i = 0; i < rows.size(); i++) {
            ComposeProject project = rows.get(i).project();
            String resolved;
            try {
                resolved = ComposePaths.normalize(project.filePath());
            } catch (RuntimeException e) {
                model.setStatus(i, "パス不正");
                continue;
            }
            String status = listings.stream()
                    .filter(listing -> listing.files().stream()
                            .anyMatch(file -> ComposePaths.normalize(file).equals(resolved)))
                    .map(ComposeListing::status)
                    .findFirst()
                    .or(() -> listings.stream()
                            .filter(listing -> listing.name().equalsIgnoreCase(project.name()))
                            .map(ComposeListing::status)
                            .findFirst())
                    .orElse("未起動");
            model.setStatus(i, status);
        }
    }

    private boolean addProject(ComposeProject project, boolean persistNow) {
        try {
            ComposePaths.resolve(project);
        } catch (AppException e) {
            if (persistNow) {
                Dialogs.error(this, e);
            }
            return false;
        }
        if (isDuplicate(project, -1)) {
            if (persistNow) {
                Dialogs.info(this, "Compose", "同じファイルは既に登録されています。");
            }
            return false;
        }
        model.add(uniqueName(project, -1));
        if (persistNow) {
            persist();
            refreshStatuses();
        }
        updateButtons();
        return true;
    }

    private boolean isDuplicate(ComposeProject candidate, int ignoreIndex) {
        String resolved;
        try {
            resolved = ComposePaths.normalize(candidate.filePath());
        } catch (RuntimeException e) {
            return false;
        }
        List<ComposeTableModel.Row> rows = model.rows();
        for (int i = 0; i < rows.size(); i++) {
            if (i == ignoreIndex) {
                continue;
            }
            try {
                if (ComposePaths.normalize(rows.get(i).project().filePath()).equals(resolved)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // 比較できない行は無視
            }
        }
        return false;
    }

    private ComposeProject uniqueName(ComposeProject project, int ignoreIndex) {
        Set<String> used = new HashSet<>();
        List<ComposeTableModel.Row> rows = model.rows();
        for (int i = 0; i < rows.size(); i++) {
            if (i != ignoreIndex) {
                used.add(rows.get(i).project().name());
            }
        }
        if (!used.contains(project.name())) {
            return project;
        }
        int suffix = 2;
        while (used.contains(project.name() + "-" + suffix)) {
            suffix++;
        }
        return project.withName(project.name() + "-" + suffix);
    }

    private void runCompose(Duration timeout, String busy, String... args) {
        List<ComposeProject> selected = Tables.selected(table, model.rows()).stream()
                .map(ComposeTableModel.Row::project)
                .toList();
        if (selected.isEmpty()) {
            return;
        }
        output.setText("");
        BusySupport.supply(this, statusBar, busy, () -> {
            StringBuilder all = new StringBuilder();
            for (ComposeProject project : selected) {
                ComposePaths.Resolved resolved = ComposePaths.resolve(project);
                all.append("=== ").append(project.name()).append(" ===\n");
                all.append("docker compose -f ").append(resolved.wslFile())
                        .append(" ").append(String.join(" ", args)).append("\n");
                try {
                    String text = session.docker().compose(
                            session.requireDistro(), timeout, resolved.wslFile(), resolved.wslDir(), args);
                    all.append(text.isBlank() ? "(出力なし)\n" : text);
                    if (!text.endsWith("\n")) {
                        all.append('\n');
                    }
                } catch (RuntimeException e) {
                    all.append(Dialogs.messageOf(e)).append('\n');
                }
                all.append('\n');
            }
            return all.toString();
        }, text -> {
            output.setText(text);
            output.setCaretPosition(0);
            refreshStatuses();
        });
    }

    private void persist() {
        settings.setComposeProjects(model.projects());
    }

    private void updateButtons() {
        int count = table.getSelectedRowCount();
        edit.setEnabled(count == 1);
        remove.setEnabled(count > 0);
        boolean canRun = count > 0 && session.connected();
        up.setEnabled(canRun);
        start.setEnabled(canRun);
        stop.setEnabled(canRun);
        restart.setEnabled(canRun);
        down.setEnabled(canRun);
        ps.setEnabled(canRun);
        logs.setEnabled(canRun);
    }

    private Frame owner() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }
}
