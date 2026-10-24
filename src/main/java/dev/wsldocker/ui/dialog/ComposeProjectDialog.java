package dev.wsldocker.ui.dialog;

import dev.wsldocker.model.ComposeProject;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

public final class ComposeProjectDialog extends JDialog {
    private ComposeProject result;

    private ComposeProjectDialog(Frame owner, String title, ComposeProject initial) {
        super(owner, title, true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JTextField name = new JTextField(initial == null ? "" : initial.name());
        JTextField file = new JTextField(initial == null ? "" : initial.filePath());
        JTextField dir = new JTextField(initial == null ? "" : initial.projectDir());
        name.putClientProperty("JTextField.placeholderText", "例: oracle");
        file.putClientProperty("JTextField.placeholderText", "C:\\... または /home/.../docker-compose.yml");
        dir.putClientProperty("JTextField.placeholderText", "空ならファイルの親ディレクトリ");

        JButton browse = new JButton("参照...");
        browse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Compose ファイルを選択");
            chooser.setFileFilter(new FileNameExtensionFilter("Compose (yml, yaml)", "yml", "yaml"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                String path = chooser.getSelectedFile().getAbsolutePath();
                file.setText(path);
                if (name.getText().isBlank()) {
                    name.setText(ComposeProject.defaultName(path));
                }
            }
        });

        JButton ok = new JButton("OK");
        ok.addActionListener(e -> {
            String filePath = file.getText().trim();
            if (filePath.isEmpty()) {
                return;
            }
            String displayName = name.getText().trim();
            if (displayName.isEmpty()) {
                displayName = ComposeProject.defaultName(filePath);
            }
            result = new ComposeProject(displayName, filePath, dir.getText().trim());
            dispose();
        });
        JButton cancel = new JButton("キャンセル");
        cancel.addActionListener(e -> dispose());

        JPanel fileRow = new JPanel(new BorderLayout(8, 0));
        fileRow.add(file, BorderLayout.CENTER);
        fileRow.add(browse, BorderLayout.EAST);

        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "名前", name);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 8, 8);
        form.add(new JLabel("ファイル"), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(fileRow, c);
        addRow(form, 2, "作業ディレクトリ", dir);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(ok);
        buttons.add(cancel);

        JLabel hint = new JLabel("WSL 内のパスも直接入力できます。例: /home/user/app/docker-compose.yml");
        JPanel south = new JPanel(new BorderLayout());
        south.add(hint, BorderLayout.WEST);
        south.add(buttons, BorderLayout.EAST);

        setLayout(new BorderLayout(8, 8));
        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(680, 220));
        pack();
        setLocationRelativeTo(owner);
    }

    public static ComposeProject show(Frame owner, String title, ComposeProject initial) {
        ComposeProjectDialog dialog = new ComposeProjectDialog(owner, title, initial);
        dialog.setVisible(true);
        return dialog.result;
    }

    private static void addRow(JPanel form, int y, String label, JTextField field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 8, 8);
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
    }
}
