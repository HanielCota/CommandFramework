package io.github.hanielcota.commandframework.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class TestActor implements CommandActor {

    private final ActorKind kind;
    private final String uniqueId;
    private final Set<String> permissions = new HashSet<>();
    private final List<String> messages = new ArrayList<>();

    public TestActor(ActorKind kind) {
        this.kind = kind;
        this.uniqueId = UUID.randomUUID().toString();
    }

    public TestActor(ActorKind kind, String uniqueId) {
        this.kind = kind;
        this.uniqueId = uniqueId;
    }

    public void grant(String permission) {
        permissions.add(permission);
    }

    public List<String> messages() {
        return messages;
    }

    @Override
    public String uniqueId() {
        return uniqueId;
    }

    @Override
    public String name() {
        return "Test";
    }

    @Override
    public ActorKind kind() {
        return kind;
    }

    @Override
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    @Override
    public void sendMessage(String message) {
        messages.add(message);
    }
}
