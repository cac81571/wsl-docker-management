package dev.wsldocker.service;

import com.google.gson.JsonObject;
import dev.wsldocker.core.AppException;
import dev.wsldocker.model.ComposeListing;
import dev.wsldocker.model.ContainerInfo;
import dev.wsldocker.model.EngineInfo;
import dev.wsldocker.model.ImageInfo;
import dev.wsldocker.model.NetworkInfo;
import dev.wsldocker.model.VolumeInfo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class DockerService {
    private final WslService wsl;

    public DockerService(WslService wsl) {
        this.wsl = wsl;
    }

    public EngineInfo engineInfo(String distro) {
        CommandResult result = docker(distro, Duration.ofSeconds(25), "info", "--format", "{{json .}}");
        JsonObject object = DockerJson.parseObject(result.stdout());
        return EngineInfo.from(object, DockerJson.pretty(result.stdout().trim()));
    }

    public List<ContainerInfo> listContainers(String distro) {
        CommandResult result = docker(distro, Duration.ofSeconds(30), "ps", "-a", "--format", "{{json .}}");
        return DockerJson.parseLines(result.stdout()).stream().map(ContainerInfo::from).toList();
    }

    public List<ImageInfo> listImages(String distro) {
        CommandResult result = docker(distro, Duration.ofSeconds(30), "images", "--format", "{{json .}}");
        return DockerJson.parseLines(result.stdout()).stream().map(ImageInfo::from).toList();
    }

    public List<VolumeInfo> listVolumes(String distro) {
        CommandResult result = docker(distro, Duration.ofSeconds(30), "volume", "ls", "--format", "{{json .}}");
        return DockerJson.parseLines(result.stdout()).stream().map(VolumeInfo::from).toList();
    }

    public List<NetworkInfo> listNetworks(String distro) {
        CommandResult result = docker(distro, Duration.ofSeconds(30), "network", "ls", "--format", "{{json .}}");
        return DockerJson.parseLines(result.stdout()).stream().map(NetworkInfo::from).toList();
    }

    public void startContainers(String distro, List<String> ids) {
        docker(distro, Duration.ofMinutes(1), prepend("start", ids));
    }

    public void stopContainers(String distro, List<String> ids) {
        docker(distro, Duration.ofMinutes(1), prepend("stop", ids));
    }

    public void restartContainers(String distro, List<String> ids) {
        docker(distro, Duration.ofMinutes(2), prepend("restart", ids));
    }

    public void removeContainers(String distro, List<String> ids, boolean force) {
        List<String> args = new ArrayList<>();
        args.add("rm");
        if (force) {
            args.add("-f");
        }
        args.addAll(ids);
        docker(distro, Duration.ofMinutes(1), args.toArray(String[]::new));
    }

    public String logs(String distro, String id, int tail) {
        CommandResult result = docker(distro, Duration.ofSeconds(20),
                "logs", "--tail", Integer.toString(tail), id);
        String out = result.stdout();
        String err = result.stderr();
        if (err != null && !err.isBlank()) {
            return (out == null ? "" : out) + (out == null || out.isBlank() ? "" : "\n") + err;
        }
        return out == null ? "" : out;
    }

    public RunningProcess followLogs(String distro, String id, int tail, Consumer<String> onLine) {
        return wsl.startInDistro(distro, onLine, "docker", "logs", "-f", "--tail", Integer.toString(tail), id);
    }

    public String inspect(String distro, String type, String id) {
        CommandResult result = docker(distro, Duration.ofSeconds(20), type, "inspect", id);
        return DockerJson.pretty(result.stdout().trim());
    }

    public String exec(String distro, String id, String command) {
        CommandResult result = docker(distro, Duration.ofMinutes(2),
                "exec", id, "sh", "-lc", command);
        String out = result.stdout() == null ? "" : result.stdout();
        String err = result.stderr() == null ? "" : result.stderr();
        if (!result.ok()) {
            throw new AppException(friendlyDockerError(result));
        }
        if (!err.isBlank()) {
            return out + (out.isBlank() ? "" : "\n") + err;
        }
        return out;
    }

    public RunningProcess pull(String distro, String reference, Consumer<String> onLine) {
        return wsl.startInDistro(distro, onLine, "docker", "pull", reference);
    }

    public RunningProcess saveImages(String distro, String wslPath, List<String> references, Consumer<String> onLine) {
        List<String> args = new ArrayList<>();
        args.add("docker");
        args.add("save");
        args.add("-o");
        args.add(wslPath);
        args.addAll(references);
        return wsl.startInDistro(distro, onLine, args.toArray(String[]::new));
    }

    public RunningProcess loadImages(String distro, String wslPath, Consumer<String> onLine) {
        return wsl.startInDistro(distro, onLine, "docker", "load", "-i", wslPath);
    }

    public RunningProcess importImage(String distro, String wslPath, String repository, Consumer<String> onLine) {
        if (repository == null || repository.isBlank()) {
            return wsl.startInDistro(distro, onLine, "docker", "import", wslPath);
        }
        return wsl.startInDistro(distro, onLine, "docker", "import", wslPath, repository);
    }

    public void removeImages(String distro, List<String> ids, boolean force) {
        List<String> args = new ArrayList<>();
        args.add("rmi");
        if (force) {
            args.add("-f");
        }
        args.addAll(ids);
        docker(distro, Duration.ofMinutes(2), args.toArray(String[]::new));
    }

    public void createVolume(String distro, String name) {
        docker(distro, Duration.ofSeconds(20), "volume", "create", name);
    }

    public void removeVolumes(String distro, List<String> names) {
        docker(distro, Duration.ofMinutes(1), prepend("volume", "rm", names));
    }

    public void createNetwork(String distro, String name, String driver) {
        docker(distro, Duration.ofSeconds(20), "network", "create", "--driver", driver, name);
    }

    public void removeNetworks(String distro, List<String> ids) {
        docker(distro, Duration.ofMinutes(1), prepend("network", "rm", ids));
    }

    public List<ComposeListing> listComposeProjects(String distro) {
        CommandResult result = docker(distro, Duration.ofSeconds(25), "compose", "ls", "-a", "--format", "json");
        return DockerJson.parseArrayOrLines(result.stdout()).stream().map(ComposeListing::from).toList();
    }

    public String compose(String distro, Duration timeout, String composeFile, String projectDir, String... args) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("compose");
        command.add("--project-directory");
        command.add(projectDir);
        command.add("-f");
        command.add(composeFile);
        command.addAll(List.of(args));
        CommandResult result = wsl.runInDistro(distro, timeout, command.toArray(String[]::new));
        if (!result.ok()) {
            throw new AppException(friendlyDockerError(result));
        }
        String out = result.stdout() == null ? "" : result.stdout();
        String err = result.stderr() == null ? "" : result.stderr();
        if (!err.isBlank()) {
            return out + (out.isBlank() ? "" : "\n") + err;
        }
        return out;
    }

    public RunningProcess composeFollow(String distro, String composeFile, String projectDir,
                                        Consumer<String> onLine, String... args) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("compose");
        command.add("--project-directory");
        command.add(projectDir);
        command.add("-f");
        command.add(composeFile);
        command.addAll(List.of(args));
        return wsl.startInDistro(distro, onLine, command.toArray(String[]::new));
    }

    private CommandResult docker(String distro, Duration timeout, String... args) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.addAll(List.of(args));
        CommandResult result = wsl.runInDistro(distro, timeout, command.toArray(String[]::new));
        if (!result.ok()) {
            throw new AppException(friendlyDockerError(result));
        }
        return result;
    }

    private static String[] prepend(String command, List<String> ids) {
        List<String> args = new ArrayList<>();
        args.add(command);
        args.addAll(ids);
        return args.toArray(String[]::new);
    }

    private static String[] prepend(String command, String sub, List<String> ids) {
        List<String> args = new ArrayList<>();
        args.add(command);
        args.add(sub);
        args.addAll(ids);
        return args.toArray(String[]::new);
    }

    static String friendlyDockerError(CommandResult result) {
        String raw = result.combinedError();
        String lower = raw.toLowerCase();
        if (lower.contains("cannot connect to the docker daemon") || lower.contains("is the docker daemon running")) {
            return "Docker デーモンに接続できません。WSL 内で Docker が起動しているか確認してください。\n\n" + raw;
        }
        if (lower.contains("command not found") || lower.contains("docker: not found")) {
            return "選択したディストリに docker コマンドがありません。\n\n" + raw;
        }
        if (raw.contains("There is no distribution") || raw.contains("無効なディストリビューション")) {
            return "指定した WSL ディストリが存在しません。\n\n" + raw;
        }
        return raw.isBlank()
                ? "Docker コマンドが失敗しました (exit " + result.exitCode() + "): " + result.commandLine()
                : raw;
    }
}
