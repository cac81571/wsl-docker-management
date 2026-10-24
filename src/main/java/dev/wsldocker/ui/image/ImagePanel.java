package dev.wsldocker.ui.image;

import dev.wsldocker.core.AppSession;
import dev.wsldocker.model.ImageInfo;
import dev.wsldocker.service.WindowsToWslPath;
import dev.wsldocker.ui.BusySupport;
import dev.wsldocker.ui.Dialogs;
import dev.wsldocker.ui.StatusBar;
import dev.wsldocker.ui.dialog.CommandStreamDialog;
import dev.wsldocker.ui.dialog.ImportImageDialog;
import dev.wsldocker.ui.dialog.InspectDialog;
import dev.wsldocker.ui.dialog.PullImageDialog;
import dev.wsldocker.ui.table.FilterableTable;
import dev.wsldocker.ui.table.Tables;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ImagePanel extends JPanel {
    private final AppSession session;
    private final StatusBar statusBar;
    private final ImageTableModel model = new ImageTableModel();
    private final JTable table = Tables.create();
    private final JButton pull = new JButton("pull");
    private final JButton export = new JButton("エクスポート");
    private final JButton imported = new JButton("インポート");
    private final JButton inspect = new JButton("inspect");
    private final JButton remove = new JButton("削除");

    public ImagePanel(AppSession session, StatusBar statusBar) {
        this.session = session;
        this.statusBar = statusBar;
        setLayout(new BorderLayout(0, 8));
        table.setModel(model);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        pull.addActionListener(e -> new PullImageDialog(owner(), session, this::reload).setVisible(true));
        export.addActionListener(e -> exportSelected());
        imported.addActionListener(e -> new ImportImageDialog(owner(), session, this::reload).setVisible(true));
        inspect.addActionListener(e -> openInspect());
        remove.addActionListener(e -> removeSelected());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.add(pull);
        toolbar.add(export);
        toolbar.add(imported);
        toolbar.add(inspect);
        toolbar.add(remove);
        add(toolbar, BorderLayout.NORTH);
        add(FilterableTable.wrap(table, "イメージをフィルタ"), BorderLayout.CENTER);
        updateButtons();
    }

    public void reload() {
        if (!session.connected()) {
            model.setRows(List.of());
            return;
        }
        BusySupport.supply(this, statusBar, "イメージを読み込み中...",
                () -> session.docker().listImages(session.requireDistro()),
                rows -> {
                    model.setRows(rows);
                    statusBar.setMessage("イメージ " + rows.size() + " 件");
                    updateButtons();
                });
    }

    private void exportSelected() {
        List<ImageInfo> selected = Tables.selected(table, model.rows());
        if (selected.isEmpty()) {
            return;
        }
        String names = selected.stream().map(ImageInfo::reference).reduce((a, b) -> a + ", " + b).orElse("");
        if (!Dialogs.confirm(this, names + " を tar にエクスポートしますか？\n大きなイメージは時間がかかります。")) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("エクスポート先");
        chooser.setSelectedFile(new java.io.File(ImageArchiveNames.defaultFileName(selected)));
        chooser.setFileFilter(new FileNameExtensionFilter("Docker image tar", "tar", "gz", "tgz"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path dest = Path.of(ImageArchiveNames.ensureTar(chooser.getSelectedFile().getAbsolutePath()));
        if (Files.exists(dest) && !Dialogs.confirm(this, dest.getFileName() + " は既に存在します。上書きしますか？")) {
            return;
        }

        List<String> references = selected.stream().map(ImageInfo::reference).toList();
        String wslPath = WindowsToWslPath.convert(dest);
        String heading = "docker save -o " + wslPath + " " + String.join(" ", references)
                + "\n\nWindows 側 (/mnt/<drive>) への書き込みは遅くなることがあります。";
        new CommandStreamDialog(owner(), "イメージをエクスポート", heading,
                onLine -> session.docker().saveImages(session.requireDistro(), wslPath, references, onLine),
                () -> statusBar.setMessage("エクスポート完了: " + dest.getFileName())
        ).setVisible(true);
    }

    private void openInspect() {
        ImageInfo row = Tables.single(table, model.rows());
        if (row == null) {
            return;
        }
        BusySupport.supply(this, statusBar, "inspect 中...",
                () -> session.docker().inspect(session.requireDistro(), "image", row.id()),
                json -> new InspectDialog(owner(), "inspect - " + row.reference(), json).setVisible(true));
    }

    private void removeSelected() {
        List<ImageInfo> selected = Tables.selected(table, model.rows());
        if (selected.isEmpty()) {
            return;
        }
        String names = selected.stream().map(ImageInfo::reference).reduce((a, b) -> a + ", " + b).orElse("");
        if (!Dialogs.confirm(this, names + " を削除しますか？ 使用中なら強制削除します。")) {
            return;
        }
        List<String> ids = selected.stream().map(ImageInfo::id).toList();
        BusySupport.run(this, statusBar, "イメージ削除中...",
                () -> session.docker().removeImages(session.requireDistro(), ids, true),
                this::reload);
    }

    private void updateButtons() {
        List<ImageInfo> selected = Tables.selected(table, model.rows());
        export.setEnabled(!selected.isEmpty());
        inspect.setEnabled(selected.size() == 1);
        remove.setEnabled(!selected.isEmpty());
    }

    private Frame owner() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }
}
