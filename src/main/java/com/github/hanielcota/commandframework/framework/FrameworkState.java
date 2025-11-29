package com.github.hanielcota.commandframework.framework;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;

public class FrameworkState {
    private BukkitAudiences audiences;

    public void setAudiences(BukkitAudiences audiences) {
        this.audiences = audiences;
    }

    public void close() {
        if (audiences == null) {
            return;
        }
        audiences.close();
        audiences = null;
    }

    public boolean hasAudiences() {
        return audiences != null;
    }
}

