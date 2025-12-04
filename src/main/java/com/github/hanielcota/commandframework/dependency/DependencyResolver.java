package com.github.hanielcota.commandframework.dependency;

/**
 * Interface para resolução de dependências durante a criação de instâncias.
 * Permite que o framework resolva dependências para comandos e providers de tab completion.
 */
public interface DependencyResolver {
    
    /**
     * Resolve uma dependência do tipo especificado.
     * 
     * @param type O tipo da dependência a ser resolvida
     * @return A instância da dependência, ou null se não puder ser resolvida
     */
    Object resolve(Class<?> type);
}

