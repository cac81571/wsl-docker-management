package dev.wsldocker.ui;

import dev.wsldocker.core.AppSession;
import dev.wsldocker.core.SettingsStore;
import dev.wsldocker.model.EngineInfo;
import dev.wsldocker.model.WslDistro;
import dev.wsldocker.ui.compose.ComposePanel;
import dev.wsldocker.ui.container.ContainerPanel;
import dev.wsldocker.ui.dialog.InspectDialog;
import dev.wsldocker.ui.image.ImagePanel;
import dev.wsldocker.ui.network.NetworkPanel;
import dev.wsldocker.ui.volume.VolumePanel;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public final class MainFrame extends JFrame {
    private final AppSession session;
    private final SettingsStore settings;
    private final StatusBar statusBar = new StatusBar();
    private final ConnectionBar connectionBar;
    private final ContainerPanel containers;
    private final ImagePanel images;
    private final VolumePanel volumes;
    private final NetworkPanel networks;
    private final ComposePanel compose;
    private final JTabbedPane tabs = new JTabbedPane();

    public MainFrame(AppSession session, SettingsStore settings) {
        super("WSL Docker マネージャ");
        this.session = session;
        this.settings = settings;
        this.connectionBar = new ConnectionBar(
                this::reloadDistros,
                this::connect,
                this::startEngine,
                this::showEngineInfo,
                this::refreshCurrent,
                this::toggleTheme,
                settings.darkTheme()
        );
        this.containers = new ContainerPanel(session, statusBar);
        this.images = new ImagePanel(session, statusBar);
        this.volumes = new VolumePanel(session, statusBar);
        this.networks = new NetworkPanel(session, statusBar);
        this.compose = new ComposePanel(session, statusBar, settings);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 620));
        setPreferredSize(new Dimension(1240, 780));
        setLayout(new BorderLayout());

        tabs.addTab("コンテナ", padded(containers));
        tabs.addTab("イメージ", padded(images));
        tabs.addTab("ボリューム", padded(volumes));
        tabs.addTab("ネットワーク", padded(networks));
        tabs.addTab("Compose", padded(compose));
        tabs.addChangeListener(e -> refreshCurrent());

        add(connectionBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        setJMenuBar(menuBar());
        getRootPane().registerKeyboardAction(
                e -> refreshCurrent(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        pack();
        setLocationRelativeTo(null);
        reloadDistrosAndConnect();
    }

    private void reloadDistrosAndConnect() {
        connectionBar.setBusy(true);
        statusBar.setMessage("WSL ディストリを検索しています...");
        UiExecutor.supply(
                session.wsl()::listDistros,
                distros -> {
                    connectionBar.setBusy(false);
                    connectionBar.setDistros(distros, settings.distro());
                    statusBar.setMessage("ディストリ " + distros.size() + " 件");
                    WslDistro selected = connectionBar.selectedDistro();
                    if (selected != null) {
                        connect(selected);
                    }
                },
                error -> {
                    connectionBar.setBusy(false);
                    connectionBar.showDisconnected();
                    statusBar.setMessage("WSL を検出できません");
                    Dialogs.error(this, error);
                }
        );
    }

    private void reloadDistros() {
        connectionBar.setBusy(true);
        statusBar.setMessage("WSL ディストリを再読込しています...");
        UiExecutor.supply(
                session.wsl()::listDistros,
                distros -> {
                    connectionBar.setBusy(false);
                    connectionBar.setDistros(distros, settings.distro());
                    statusBar.setMessage("ディストリ " + distros.size() + " 件");
                },
                error -> {
                    connectionBar.setBusy(false);
                    Dialogs.error(this, error);
                }
        );
    }

    private void connect(WslDistro distro) {
        connectionBar.setBusy(true);
        statusBar.setMessage(distro.name() + " の Docker エンジンに接続しています...");
        UiExecutor.supply(
                () -> session.docker().engineInfo(distro.name()),
                engine -> onConnected(distro, engine),
                error -> {
                    session.disconnect();
                    connectionBar.setBusy(false);
                    connectionBar.showDisconnected();
                    statusBar.setMessage("接続に失敗しました");
                    Dialogs.error(this, error);
                }
        );
    }

    private void onConnected(WslDistro distro, EngineInfo engine) {
        session.connect(distro.name(), engine);
        settings.setDistro(distro.name());
        connectionBar.setBusy(false);
        connectionBar.showConnected(engine);
        statusBar.setMessage(engine.summary());
        refreshCurrent();
    }

    private void startEngine() {
        WslDistro distro = connectionBar.selectedDistro();
        if (distro == null) {
            return;
        }
        if (!Dialogs.confirm(this, distro.name() + " で Docker デーモン起動を試しますか？\n(sudo パスワードが必要な場合は失敗します)")) {
            return;
        }
        connectionBar.setBusy(true);
        statusBar.setMessage("デーモン起動を試しています...");
        UiExecutor.run(
                () -> session.wsl().tryStartDocker(distro.name()),
                () -> {
                    connectionBar.setBusy(false);
                    connect(distro);
                },
                error -> {
                    connectionBar.setBusy(false);
                    statusBar.setMessage("デーモン起動に失敗しました");
                    Dialogs.error(this, error);
                }
        );
    }

    private void showEngineInfo() {
        if (!session.connected()) {
            return;
        }
        EngineInfo engine = session.engine();
        new InspectDialog(this, "エンジン情報 - " + session.distro(), engine.rawJson()).setVisible(true);
    }

    private void refreshCurrent() {
        if (!session.connected()) {
            return;
        }
        switch (tabs.getSelectedIndex()) {
            case 0 -> containers.reload();
            case 1 -> images.reload();
            case 2 -> volumes.reload();
            case 3 -> networks.reload();
            case 4 -> compose.reload();
            default -> {
            }
        }
    }

    private void toggleTheme(boolean dark) {
        settings.setDarkTheme(dark);
        ThemeController.switchTheme(dark);
        SwingUtilities.updateComponentTreeUI(this);
    }

    private JMenuBar menuBar() {
        JMenu file = new JMenu("ファイル");
        JMenuItem reload = new JMenuItem("ディストリ再読込");
        reload.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
        reload.addActionListener(e -> reloadDistros());
        JMenuItem exit = new JMenuItem("終了");
        exit.addActionListener(e -> dispose());
        file.add(reload);
        file.addSeparator();
        file.add(exit);

        JMenu view = new JMenu("表示");
        JMenuItem refresh = new JMenuItem("更新");
        refresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        refresh.addActionListener(e -> refreshCurrent());
        view.add(refresh);

        JMenu help = new JMenu("ヘルプ");
        JMenuItem about = new JMenuItem("このアプリについて");
        about.addActionListener(e -> Dialogs.info(this, "WSL Docker マネージャ", """
                WSL ディストリ内の Docker エンジンを、Windows 上の Swing / FlatLaf GUI から操作します。

                内部では `wsl -d <distro> -- docker ...` を実行しています。
                削除などの破壊操作は確認ダイアログを出します。

                F5: 現在のタブを更新
                """));
        help.add(about);

        JMenuBar bar = new JMenuBar();
        bar.add(file);
        bar.add(view);
        bar.add(help);
        return bar;
    }

    private static javax.swing.JComponent padded(javax.swing.JComponent component) {
        javax.swing.JPanel panel = new javax.swing.JPanel(new BorderLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }
}
