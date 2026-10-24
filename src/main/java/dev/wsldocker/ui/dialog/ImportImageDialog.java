package dev.wsldocker.ui.dialog;

import dev.wsldocker.core.AppSession;
import dev.wsldocker.service.WindowsToWslPath;
import dev.wsldocker.ui.Dialogs;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ImportImageDialog extends JDialog {
    public ImportImageDialog(Frame owner, AppSession session, Runnable onSuccess) {
        super(owner, "イメージをインポート", true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JTextField fileField = new JTextField();
        fileField.putClientProperty("JTextField.placeholderText", "docker save / export の tar");
        JButton browse = new JButton("参照...");
        browse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("インポートする tar を選択");
            chooser.setFileFilter(new FileNameExtensionFilter("Image archive (tar, tgz)", "tar", "gz", "tgz"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                fileField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        JRadioButton load = new JRadioButton("load（docker save のイメージ）", true);
        JRadioButton imported = new JRadioButton("import（コンテナ export のファイルシステム）");
        ButtonGroup group = new ButtonGroup();
        group.add(load);
        group.add(imported);

        JTextField repository = new JTextField();
        repository.putClientProperty("JTextField.placeholderText", "例: my-app:latest");
        repository.setEnabled(false);
        imported.addActionListener(e -> repository.setEnabled(true));
        load.addActionListener(e -> repository.setEnabled(false));

        JButton start = new JButton("インポート");
        start.addActionListener(e -> {
            Path file = Path.of(fileField.getText().trim());
            if (!Files.isRegularFile(file)) {
                Dialogs.info(this, "インポート", "tar ファイルを選択してください。");
                return;
            }
            String wslPath = WindowsToWslPath.convert(file);
            boolean useLoad = load.isSelected();
            String tag = repository.getText().trim();
            String heading = useLoad
                    ? "docker load -i " + wslPath
                    : "docker import " + wslPath + (tag.isEmpty() ? "" : " " + tag);
            heading += "\n\n大きなファイルは数分かかることがあります。";
            dispose();
            new CommandStreamDialog(owner, "イメージをインポート", heading, onLine -> {
                if (useLoad) {
                    return session.docker().loadImages(session.requireDistro(), wslPath, onLine);
                }
                return session.docker().importImage(session.requireDistro(), wslPath, tag, onLine);
            }, onSuccess).setVisible(true);
        });

        JButton cancel = new JButton("キャンセル");
        cancel.addActionListener(e -> dispose());

        JPanel fileRow = new JPanel(new BorderLayout(8, 0));
        fileRow.add(fileField, BorderLayout.CENTER);
        fileRow.add(browse, BorderLayout.EAST);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 6, 8);
        form.add(new JLabel("ファイル"), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(fileRow, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("形式"), c);
        c.gridx = 1;
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        radios.add(load);
        radios.add(imported);
        form.add(radios, c);

        c.gridx = 0;
        c.gridy = 2;
        form.add(new JLabel("タグ"), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(repository, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(start);
        buttons.add(cancel);

        setLayout(new BorderLayout(8, 8));
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(640, 220));
        pack();
        setLocationRelativeTo(owner);
    }
}
