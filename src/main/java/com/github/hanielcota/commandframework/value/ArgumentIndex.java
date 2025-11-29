package com.github.hanielcota.commandframework.value;

/**
 * Representa o índice de um argumento em um array de argumentos.
 *
 * @param value Índice do argumento (deve ser não negativo)
 */
public record ArgumentIndex(int value) {
    public ArgumentIndex {
        if (value < 0) {
            throw new IllegalArgumentException("ArgumentIndex não pode ser negativo");
        }
    }

    /**
     * Verifica se o índice está dentro dos limites do array.
     *
     * @param arrayLength Tamanho do array
     * @return true se o índice é válido
     */
    public boolean isWithinBounds(int arrayLength) {
        return value < arrayLength;
    }

    /**
     * Incrementa o índice em 1.
     *
     * @return Novo índice incrementado
     */
    public ArgumentIndex increment() {
        return new ArgumentIndex(value + 1);
    }
}

