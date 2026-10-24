package dev.wsldocker;

import com.formdev.flatlaf.util.SystemInfo;
import dev.wsldocker.core.AppSession;
import dev.wsldocker.core.SettingsStore;
import dev.wsldocker.model.WslDistro;
import dev.wsldocker.service.DockerService;
import dev.wsldocker.service.ProcessRunner;
import dev.wsldocker.service.WslService;
import dev.wsldocker.ui.MainFrame;
import dev.wsldocker.ui.ThemeController;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class WslDockerApp {
    private WslDockerApp() {
    }

    public static void main(String[] args) {
        ProcessRunner runner = new ProcessRunner();
        WslService wsl = new WslService(runner);
        DockerService docker = new DockerService(wsl);

        if (args.length > 0 && "--check".equals(args[0])) {
            check(wsl, docker);
            return;
        }

        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("flatlaf.useWindowDecorations", "true");
        if (SystemInfo.isLinux) {
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
        }

        SettingsStore settings = new SettingsStore();
        ThemeController.install(settings.darkTheme());
        AppSession session = new AppSession(wsl, docker);
        SwingUtilities.invokeLater(() -> new MainFrame(session, settings).setVisible(true));
    }

    private static void check(WslService wsl, DockerService docker) {
        var distros = wsl.listDistros();
        System.out.println("distros=" + distros.size());
        distros.forEach(distro -> System.out.println(" - " + distro.label()));
        String name = distros.stream()
                .filter(WslDistro::defaultDistro)
                .findFirst()
                .orElse(distros.getFirst())
                .name();
        var engine = docker.engineInfo(name);
        System.out.println("engine=" + engine.summary());
        var containers = docker.listContainers(name);
        System.out.println("containers=" + containers.size());
        containers.forEach(container ->
                System.out.println(" - " + container.name() + " [" + container.state() + "] " + container.image()));
    }
}
