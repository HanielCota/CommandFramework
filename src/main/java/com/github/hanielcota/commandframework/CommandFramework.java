package com.github.hanielcota.commandframework;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.hanielcota.commandframework.cache.FrameworkCaches;
import com.github.hanielcota.commandframework.cooldown.CooldownService;
import com.github.hanielcota.commandframework.error.GlobalErrorHandler;
import com.github.hanielcota.commandframework.execution.CommandExecutor;
import com.github.hanielcota.commandframework.framework.FrameworkConfiguration;
import com.github.hanielcota.commandframework.framework.FrameworkInitializer;
import com.github.hanielcota.commandframework.framework.FrameworkState;
import com.github.hanielcota.commandframework.messaging.MessageProvider;
import com.github.hanielcota.commandframework.messaging.MiniMessageProvider;
import com.github.hanielcota.commandframework.parser.ArgumentParserRegistry;
import com.github.hanielcota.commandframework.parser.builtin.BuiltinParsers;
import com.github.hanielcota.commandframework.processor.CommandProcessor;
import com.github.hanielcota.commandframework.registry.CommandScanner;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.time.Duration;

public class CommandFramework {

    private final Plugin plugin;
    private FrameworkConfiguration configuration;
    private FrameworkState state;

    public CommandFramework(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin não pode ser nulo");
        }
        this.plugin = plugin;
        this.state = new FrameworkState();
    }

    private CommandFramework(Plugin plugin, FrameworkConfiguration configuration) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.state = new FrameworkState();
    }

    @SuppressWarnings("unused") // handlerCache é parte da API pública, pode ser usado por consumidores
    public static CommandFramework create(Plugin plugin, MessageProvider messageProvider, Cache<Class<?>, Object> handlerCache) {
        var initializer = new FrameworkInitializer(plugin);
        var configuration = initializer.initialize(messageProvider);
        return new CommandFramework(plugin, configuration);
    }

    public void registerPackage(String basePackage) {
        if (basePackage == null || basePackage.isBlank()) {
            return;
        }
        ensureInitialized();
        scheduleAsyncScanAndRegister(basePackage);
    }

    private void scheduleAsyncScanAndRegister(String basePackage) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> scanAndRegister(basePackage));
    }

    /**
     * Registra comandos de um pacote de forma síncrona.
     * Use este método quando precisar garantir que os comandos estejam registrados
     * antes de continuar a execução.
     * 
     * @param basePackage O pacote base onde os comandos estão localizados
     */
    public void registerPackageSync(String basePackage) {
        if (basePackage == null || basePackage.isBlank()) {
            return;
        }

        ensureInitialized();
        scanAndRegister(basePackage);
    }

    private void scanAndRegister(String basePackage) {
        var definitions = configuration.getScanner().scan(basePackage);
        if (definitions.isEmpty()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            configuration.getProcessor().processAndRegister(definitions);
            return;
        }
        scheduleSyncProcessAndRegister(definitions);
    }

    private void scheduleSyncProcessAndRegister(java.util.List<com.github.hanielcota.commandframework.registry.CommandDefinition> definitions) {
        Bukkit.getScheduler().runTask(plugin, () -> configuration.getProcessor().processAndRegister(definitions));
    }

    private void ensureInitialized() {
        if (configuration == null) {
            throw new IllegalStateException("Framework não foi configurado. Chame setup() primeiro.");
        }
    }

    public void reload() {
        ensureInitialized();
        scheduleAsyncReload();
    }

    private void scheduleAsyncReload() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> performReload());
    }

    private void performReload() {
        var packageName = plugin.getClass().getPackage().getName();
        var definitions = configuration.getScanner().scan(packageName);
        scheduleSyncProcessAndRegister(definitions);
    }

    /**
     * Registra um comando diretamente a partir de uma instância.
     * 
     * Exemplo:
     * <pre>{@code
     * framework.register(new GamemodeCommand());
     * }</pre>
     * 
     * @param instance Instância do comando (deve ter a anotação @Command)
     */
    public void register(Object instance) {
        if (instance == null) {
            return;
        }
        ensureInitialized();
        if (!Bukkit.isPrimaryThread()) {
            scheduleSyncRegister(instance);
            return;
        }
        registerSync(instance);
    }

    private void registerSync(Object instance) {
        configuration.getProcessor().register(instance);
    }

    private void scheduleSyncRegister(Object instance) {
        Bukkit.getScheduler().runTask(plugin, () -> registerSync(instance));
    }

    /**
     * Registra um comando a partir de uma classe (cria a instância automaticamente).
     * 
     * Exemplo:
     * <pre>{@code
     * framework.register(GamemodeCommand.class);
     * }</pre>
     * 
     * @param commandClass Classe do comando (deve ter a anotação @Command)
     */
    public void register(Class<?> commandClass) {
        if (commandClass == null) {
            return;
        }
        ensureInitialized();
        if (!Bukkit.isPrimaryThread()) {
            scheduleSyncRegister(commandClass);
            return;
        }
        registerSync(commandClass);
    }

    private void registerSync(Class<?> commandClass) {
        configuration.getProcessor().register(commandClass);
    }

    private void scheduleSyncRegister(Class<?> commandClass) {
        Bukkit.getScheduler().runTask(plugin, () -> registerSync(commandClass));
    }

    /**
     * Configura o CommandFramework automaticamente.
     * Detecta automaticamente o pacote base do plugin e registra os comandos.
     * 
     * Exemplo:
     * <pre>{@code
     * CommandFramework framework = new CommandFramework(plugin);
     * framework.setup();
     * }</pre>
     * 
     * @return Esta instância para permitir chamadas encadeadas
     */
    public CommandFramework setup() {
        var packageName = plugin.getClass().getPackage();
        var basePackage = packageName.getName();
        return setup(basePackage);
    }

    /**
     * Configura o CommandFramework automaticamente.
     * Cria o BukkitAudiences, MiniMessage, MessageProvider e registra os comandos do pacote.
     * 
     * Exemplo:
     * <pre>{@code
     * CommandFramework framework = new CommandFramework(plugin);
     * framework.setup("com.meuprojeto.commands");
     * }</pre>
     * 
     * @param basePackage O pacote base onde os comandos estão localizados
     * @return Esta instância para permitir chamadas encadeadas
     */
    public CommandFramework setup(String basePackage) {
        if (basePackage == null || basePackage.isBlank()) {
            throw new IllegalArgumentException("basePackage não pode ser nulo ou vazio");
        }
        initializeWithAdventure();
        registerPackage(basePackage);
        return this;
    }

    private void initializeWithAdventure() {
        var initializer = new FrameworkInitializer(plugin);
        configuration = initializer.initializeWithAdventure();
        var audiences = BukkitAudiences.create(plugin);
        state.setAudiences(audiences);
    }

    public void close() {
        state.close();
    }

    public void configureParserRegistry(java.util.function.Consumer<ArgumentParserRegistry> action) {
        if (configuration == null) {
            throw new IllegalStateException("Framework não foi configurado. Chame setup() primeiro.");
        }
        action.accept(configuration.getParserRegistry());
    }

    /**
     * Cria e configura o CommandFramework de forma simplificada (método estático alternativo).
     * Configura automaticamente o BukkitAudiences, MiniMessage e MessageProvider.
     * 
     * Exemplo de uso:
     * <pre>{@code
     * public class MeuPlugin extends JavaPlugin {
     *     private FrameworkSetup setup;
     *     
     *     @Override
     *     public void onEnable() {
     *         setup = CommandFramework.setup(this, "com.meuprojeto.commands");
     *     }
     *     
     *     @Override
     *     public void onDisable() {
     *         if (setup != null) {
     *             setup.close();
     *         }
     *     }
     * }
     * }</pre>
     * 
     * @param plugin O plugin JavaPlugin
     * @param basePackage O pacote base onde os comandos estão localizados
     * @return Um FrameworkSetup contendo o framework e o audiences (para fechar no onDisable)
     */
    public static FrameworkSetup setup(Plugin plugin, String basePackage) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin não pode ser nulo");
        }
        var audiences = BukkitAudiences.create(plugin);
        var miniMessage = MiniMessage.miniMessage();
        var messageProvider = new MiniMessageProvider(audiences, miniMessage);
        var handlerCache = FrameworkCaches.handlerInstances();
        var framework = create(plugin, messageProvider, handlerCache);
        framework.state.setAudiences(audiences);
        framework.registerPackage(basePackage);
        return new FrameworkSetup(framework, audiences);
    }

    /**
     * Wrapper que contém o CommandFramework e o BukkitAudiences.
     * Use o método close() no onDisable para fechar o audiences.
     */
    public record FrameworkSetup(CommandFramework framework, BukkitAudiences audiences) {
        /**
         * Fecha o BukkitAudiences. Deve ser chamado no onDisable do plugin.
         */
        public void close() {
            if (audiences != null) {
                audiences.close();
            }
        }
    }
}

