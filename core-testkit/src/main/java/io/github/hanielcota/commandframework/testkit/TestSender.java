package io.github.hanielcota.commandframework.testkit;

import io.github.hanielcota.commandframework.CommandActor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

/**
 * Lightweight sender used by {@link CommandTestKit}. Messages and permissions are
 * captured in-memory so tests can assert against them.
 */
public final class TestSender implements CommandActor {

    private final String name;
    private final UUID uniqueId;
    private final boolean player;
    private final Set<String> permissions = ConcurrentHashMap.newKeySet();
    private final List<Component> messages = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean available = true;

    TestSender(String name, boolean player) {
        this.name = name;
        this.uniqueId = UUID.nameUUIDFromBytes(("testkit:" + name).getBytes(StandardCharsets.UTF_8));
        this.player = player;
    }

    /**
     * Grants a permission to this sender.
     *
     * @param permission permission node
     * @return this sender
     */
    public TestSender grant(String permission) {
        this.permissions.add(permission);
        return this;
    }

    /**
     * Revokes a permission from this sender.
     *
     * @param permission permission node
     * @return this sender
     */
    public TestSender revoke(String permission) {
        this.permissions.remove(permission);
        return this;
    }

    /**
     * Marks this sender as offline / unavailable so the framework skips further messages.
     *
     * @return this sender
     */
    public TestSender markUnavailable() {
        this.available = false;
        return this;
    }

    /**
     * Returns every {@link Component} this sender has received during the test.
     *
     * @return received messages (live list - copy if mutating)
     */
    public List<Component> receivedMessages() {
        synchronized (this.messages) {
            return List.copyOf(this.messages);
        }
    }

    /**
     * Returns received messages rendered as plain text. Simple best-effort render that
     * extracts the top-level content of {@link TextComponent}s and recursively appends
     * children. Does not resolve translations or styling - use
     * {@code adventure-text-serializer-plain} in your own tests if you need richer output.
     *
     * @return plain-text rendered messages
     */
    public List<String> receivedPlainMessages() {
        synchronized (this.messages) {
            return this.messages.stream().map(TestSender::plainText).toList();
        }
    }

    /**
     * Returns the most recent plain-text message, or an empty string if none.
     *
     * @return last plain message
     */
    public String lastMessage() {
        synchronized (this.messages) {
            if (this.messages.isEmpty()) {
                return "";
            }
            return plainText(this.messages.getLast());
        }
    }

    private static String plainText(Component component) {
        StringBuilder builder = new StringBuilder();
        appendPlain(component, builder);
        return builder.toString();
    }

    private static void appendPlain(Component component, StringBuilder builder) {
        if (component instanceof TextComponent text) {
            builder.append(text.content());
        }
        component.children().forEach(child -> appendPlain(child, builder));
    }

    @Override public String name() { return this.name; }
    @Override public UUID uniqueId() { return this.uniqueId; }
    @Override public boolean isPlayer() { return this.player; }
    @Override public boolean hasPermission(String permission) { return this.permissions.contains(permission); }
    @Override public void sendMessage(Component message) { this.messages.add(message); }
    @Override public boolean isAvailable() { return this.available; }
    @Override public Object platformSender() { return this; }
}
