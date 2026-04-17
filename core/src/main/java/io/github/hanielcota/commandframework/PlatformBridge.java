package io.github.hanielcota.commandframework;

import java.util.List;

/**
 * Supplies platform-specific behavior required by the shared framework.
 *
 * @param <S> the native sender type
 */
public interface PlatformBridge<S> {

    /**
     * Returns the platform class loader used for auto-scanning.
     *
     * @return the platform class loader
     */
    ClassLoader classLoader();

    /**
     * Returns the default root package used for auto-scanning.
     *
     * @return the default root package
     */
    String defaultScanPackage();

    /**
     * Returns the logger used by the framework.
     *
     * @return the framework logger
     */
    FrameworkLogger logger();

    /**
     * Creates a platform-neutral actor wrapper.
     *
     * @param sender the native sender
     * @return the actor wrapper
     */
    CommandActor createActor(S sender);

    /**
     * Returns whether the supplied type is valid for {@link io.github.hanielcota.commandframework.annotation.Sender}.
     *
     * @param type the declared Java type
     * @return {@code true} when the type is supported
     */
    boolean supportsSenderType(Class<?> type);

    /**
     * Returns whether the supplied type represents a player-only sender type.
     *
     * @param type the declared Java type
     * @return {@code true} when the type requires a player sender
     */
    boolean isPlayerSenderType(Class<?> type);

    /**
     * Returns platform-specific argument resolvers.
     *
     * @return the additional argument resolvers
     */
    default List<ArgumentResolver<?>> platformResolvers() {
        return List.of();
    }

    /**
     * Registers the built framework with the host platform.
     *
     * @param framework the built framework
     */
    void register(CommandFramework<S> framework);
}
