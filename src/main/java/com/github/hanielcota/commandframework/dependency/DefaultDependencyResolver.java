package com.github.hanielcota.commandframework.dependency;

import com.github.hanielcota.commandframework.cooldown.CooldownService;
import com.github.hanielcota.commandframework.error.GlobalErrorHandler;
import com.github.hanielcota.commandframework.execution.CommandExecutor;
import com.github.hanielcota.commandframework.parser.ArgumentParserRegistry;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação padrão do DependencyResolver.
 * Resolve dependências do framework e permite registro de dependências customizadas.
 */
public class DefaultDependencyResolver implements DependencyResolver {
    
    private final Plugin plugin;
    private final ArgumentParserRegistry parserRegistry;
    private final CommandExecutor executor;
    private final CooldownService cooldownService;
    private final GlobalErrorHandler errorHandler;
    private final Map<Class<?>, Object> customDependencies = new ConcurrentHashMap<>();
    
    public DefaultDependencyResolver(
            Plugin plugin,
            ArgumentParserRegistry parserRegistry,
            CommandExecutor executor,
            CooldownService cooldownService,
            GlobalErrorHandler errorHandler) {
        this.plugin = plugin;
        this.parserRegistry = parserRegistry;
        this.executor = executor;
        this.cooldownService = cooldownService;
        this.errorHandler = errorHandler;
    }
    
    @Override
    public Object resolve(Class<?> type) {
        if (type == null) {
            return null;
        }
        
        // Resolve dependências do framework
        if (type.equals(Plugin.class)) {
            return plugin;
        }
        if (type.equals(ArgumentParserRegistry.class)) {
            return parserRegistry;
        }
        if (type.equals(CommandExecutor.class)) {
            return executor;
        }
        if (type.equals(CooldownService.class)) {
            return cooldownService;
        }
        if (type.equals(GlobalErrorHandler.class)) {
            return errorHandler;
        }
        
        // Resolve dependências customizadas registradas
        return customDependencies.get(type);
    }
    
    /**
     * Registra uma dependência customizada para ser resolvida automaticamente.
     * 
     * @param type O tipo da dependência
     * @param instance A instância da dependência
     */
    public void registerDependency(Class<?> type, Object instance) {
        if (type == null || instance == null) {
            return;
        }
        if (!type.isInstance(instance)) {
            throw new IllegalArgumentException("Instância não é do tipo " + type.getName());
        }
        customDependencies.put(type, instance);
    }
    
    /**
     * Remove uma dependência customizada.
     * 
     * @param type O tipo da dependência a ser removida
     */
    public void unregisterDependency(Class<?> type) {
        customDependencies.remove(type);
    }
}

