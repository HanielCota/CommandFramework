package com.github.hanielcota.commandframework.value;

/**
 * Limite máximo de sugestões de tab completion.
 *
 * @param value Número máximo de sugestões (deve ser não negativo)
 */
public record CompletionLimit(int value) {
    /** Limite padrão de 50 sugestões. */
    public static final CompletionLimit DEFAULT = new CompletionLimit(50);

    public CompletionLimit {
        if (value < 0) {
            throw new IllegalArgumentException("CompletionLimit não pode ser negativo");
        }
    }

    /**
     * Verifica se o limite foi atingido.
     *
     * @param currentCount Contagem atual
     * @return true se o limite foi atingido ou ultrapassado
     */
    public boolean isReached(int currentCount) {
        return currentCount >= value;
    }
}

