package dev.wsldocker.service;

import dev.wsldocker.core.AppException;
import dev.wsldocker.model.WslDistro;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class WslService {
    private final ProcessRunner runner;
    private final String wslExecutable;

    public WslService(ProcessRunner runner) {
        this.runner = runner;
        this.wslExecutable = resolveWslExecutable();
    }

    public String executable() {
        return wslExecutable;
    }

    public List<WslDistro> listDistros() {
        CommandResult result = runner.run(List.of(wslExecutable, "--list", "--verbose"), Duration.ofSeconds(15));
        if (!result.ok()) {
            throw new AppException("WSL ディストリの一覧取得に失敗しました。\n" + friendlyWslError(result));
        }
        List<WslDistro> distros = WslListParser.parse(result.stdout());
        if (distros.isEmpty()) {
            throw new AppException("WSL ディストリが見つかりませんでした。`wsl --list --verbose` の結果を確認してください。");
        }
        return distros;
    }

    public CommandResult runInDistro(String distro, Duration timeout, String... command) {
        List<String> full = new ArrayList<>();
        full.add(wslExecutable);
        full.add("-d");
        full.add(distro);
        full.add("--");
        full.addAll(List.of(command));
        return runner.run(full, timeout);
    }

    public RunningProcess startInDistro(String distro, java.util.function.Consumer<String> onLine, String... command) {
        List<String> full = new ArrayList<>();
        full.add(wslExecutable);
        full.add("-d");
        full.add(distro);
        full.add("--");
        full.addAll(List.of(command));
        return runner.start(full, onLine);
    }

    public void tryStartDocker(String distro) {
        CommandResult result = runInDistro(distro, Duration.ofSeconds(20),
                "sh", "-lc", "service docker start || systemctl start docker");
        if (!result.ok()) {
            throw new AppException("""
                    Docker デーモンの起動に失敗しました。
                    パスワード付き sudo が必要な場合は、WSL 側で手動起動してください。

                    """ + result.combinedError());
        }
    }

    private static String resolveWslExecutable() {
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot != null && !systemRoot.isBlank()) {
            Path candidate = Path.of(systemRoot, "System32", "wsl.exe");
            if (Files.isRegularFile(candidate)) {
                return candidate.toString();
            }
        }
        return "wsl";
    }

    static String friendlyWslError(CommandResult result) {
        String raw = result.combinedError();
        if (raw.toLowerCase().contains("wsl") && raw.toLowerCase().contains("not")
                && (raw.toLowerCase().contains("install") || raw.toLowerCase().contains("found"))) {
            return "WSL が見つかりません。Windows に WSL がインストールされているか確認してください。\n\n" + raw;
        }
        if (raw.contains("There is no distribution") || raw.contains("無効なディストリビューション")) {
            return "指定した WSL ディストリが存在しません。\n\n" + raw;
        }
        return raw.isBlank()
                ? "WSL コマンドが失敗しました (exit " + result.exitCode() + ")"
                : raw;
    }
}
