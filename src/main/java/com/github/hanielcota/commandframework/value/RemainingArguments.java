package com.github.hanielcota.commandframework.value;

/**
 * Representa os argumentos restantes após processar um subcomando.
 *
 * @param value Array de argumentos restantes (não pode ser nulo)
 */
public record RemainingArguments(String[] value) {
    public RemainingArguments {
        if (value == null) {
            throw new IllegalArgumentException("RemainingArguments não pode ser nulo");
        }
    }

    /**
     * Retorna o número de argumentos restantes.
     *
     * @return Tamanho do array de argumentos
     */
    public int length() {
        return value.length;
    }

    /**
     * Verifica se não há argumentos restantes.
     *
     * @return true se o array está vazio
     */
    public boolean isEmpty() {
        return value.length == 0;
    }

    /**
     * Obtém um argumento pelo índice.
     *
     * @param index Índice do argumento
     * @return O argumento no índice especificado, ou null se o índice for inválido
     */
    public String get(int index) {
        if (index < 0 || index >= value.length) {
            return null;
        }
        return value[index];
    }
}

