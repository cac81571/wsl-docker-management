package dev.wsldocker.ui;

import dev.wsldocker.model.EngineInfo;
import dev.wsldocker.model.WslDistro;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.util.function.Consumer;

public final class ConnectionBar extends JPanel {
    private final DefaultComboBoxModel<WslDistro> distroModel = new DefaultComboBoxModel<>();
    private final JComboBox<WslDistro> distroCombo = new JComboBox<>(distroModel);
    private final JLabel status = new JLabel("未接続");
    private final JLabel summary = new JLabel(" ");
    private final JButton connect = new JButton("接続");
    private final JButton reloadDistros = new JButton("再読込");
    private final JButton startEngine = new JButton("デーモン起動");
    private final JButton info = new JButton("エンジン情報");
    private final JButton theme = new JButton("ライト");
    private final JButton refresh = new JButton("更新");

    public ConnectionBar(
            Runnable onReloadDistros,
            Consumer<WslDistro> onConnect,
            Runnable onStartEngine,
            Runnable onEngineInfo,
            Runnable onRefresh,
            Consumer<Boolean> onThemeToggle,
            boolean darkTheme
    ) {
        setLayout(new BorderLayout());
        distroCombo.setRenderer(renderer());
        distroCombo.setPreferredSize(new Dimension(280, 30));
        distroCombo.setMaximumRowCount(12);

        connect.addActionListener(e -> {
            WslDistro selected = (WslDistro) distroCombo.getSelectedItem();
            if (selected != null) {
                onConnect.accept(selected);
            }
        });
        reloadDistros.addActionListener(e -> onReloadDistros.run());
        startEngine.addActionListener(e -> onStartEngine.run());
        info.addActionListener(e -> onEngineInfo.run());
        refresh.addActionListener(e -> onRefresh.run());
        applyThemeLabel(darkTheme);
        theme.addActionListener(e -> {
            boolean nextDark = "ダーク".equals(theme.getText());
            applyThemeLabel(nextDark);
            onThemeToggle.accept(nextDark);
        });

        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.add(new JLabel(" ディストリ "));
        bar.add(distroCombo);
        bar.add(reloadDistros);
        bar.addSeparator();
        bar.add(connect);
        bar.add(startEngine);
        bar.addSeparator();
        bar.add(status);
        bar.addSeparator();
        bar.add(summary);
        bar.add(info);
        bar.add(refresh);
        bar.add(theme);
        add(bar, BorderLayout.CENTER);
        showDisconnected();
    }

    public void setDistros(List<WslDistro> distros, String preferredName) {
        String current = preferredName;
        if (current == null || current.isBlank()) {
            WslDistro selected = (WslDistro) distroCombo.getSelectedItem();
            if (selected != null) {
                current = selected.name();
            }
        }
        distroModel.removeAllElements();
        distros.forEach(distroModel::addElement);
        if (distros.isEmpty()) {
            return;
        }
        int index = indexOf(current);
        if (index < 0) {
            index = indexOfDefault(distros);
        }
        distroCombo.setSelectedIndex(Math.max(index, 0));
    }

    public WslDistro selectedDistro() {
        return (WslDistro) distroCombo.getSelectedItem();
    }

    public void showConnected(EngineInfo engine) {
        status.setText("  ● 接続中  ");
        status.setForeground(new Color(0x3DDC84));
        summary.setText("  " + engine.summary() + "  ");
        info.setEnabled(true);
        refresh.setEnabled(true);
    }

    public void showDisconnected() {
        status.setText("  ● 未接続  ");
        status.setForeground(new Color(0xE06C75));
        summary.setText("  Docker エンジンに未接続  ");
        info.setEnabled(false);
        refresh.setEnabled(false);
    }

    public void setBusy(boolean busy) {
        connect.setEnabled(!busy);
        reloadDistros.setEnabled(!busy);
        startEngine.setEnabled(!busy);
        distroCombo.setEnabled(!busy);
    }

    private void applyThemeLabel(boolean dark) {
        theme.setText(dark ? "ライト" : "ダーク");
        theme.setToolTipText(dark ? "ライトテーマへ切替" : "ダークテーマへ切替");
    }

    private int indexOf(String name) {
        if (name == null || name.isBlank()) {
            return -1;
        }
        for (int i = 0; i < distroModel.getSize(); i++) {
            if (name.equals(distroModel.getElementAt(i).name())) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfDefault(List<WslDistro> distros) {
        for (int i = 0; i < distros.size(); i++) {
            if (distros.get(i).defaultDistro()) {
                return i;
            }
        }
        return 0;
    }

    private static ListCellRenderer<? super WslDistro> renderer() {
        return (list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value == null ? "" : value.label());
            label.setOpaque(true);
            label.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            return label;
        };
    }
}
