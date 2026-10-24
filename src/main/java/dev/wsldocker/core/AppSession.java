package dev.wsldocker.core;

import dev.wsldocker.model.EngineInfo;
import dev.wsldocker.service.DockerService;
import dev.wsldocker.service.WslService;

public final class AppSession {
    private final WslService wsl;
    private final DockerService docker;
    private String distro;
    private EngineInfo engine;

    public AppSession(WslService wsl, DockerService docker) {
        this.wsl = wsl;
        this.docker = docker;
    }

    public WslService wsl() {
        return wsl;
    }

    public DockerService docker() {
        return docker;
    }

    public synchronized void connect(String distro, EngineInfo engine) {
        this.distro = distro;
        this.engine = engine;
    }

    public synchronized void disconnect() {
        this.distro = null;
        this.engine = null;
    }

    public synchronized boolean connected() {
        return distro != null && !distro.isBlank() && engine != null;
    }

    public synchronized String requireDistro() {
        if (!connected()) {
            throw new AppException("先に WSL ディストリを接続してください。");
        }
        return distro;
    }

    public synchronized String distro() {
        return distro;
    }

    public synchronized EngineInfo engine() {
        return engine;
    }
}
