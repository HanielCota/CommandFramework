package com.github.hanielcota.commandframework.value;

/**
 * Representa o índice de um parâmetro em um método.
 *
 * @param value Índice do parâmetro (deve ser não negativo)
 */
public record ParameterIndex(int value) {
    public ParameterIndex {
        if (value < 0) {
            throw new IllegalArgumentException("ParameterIndex não pode ser negativo");
        }
    }

    /**
     * Verifica se o índice está dentro dos limites de um array.
     *
     * @param arrayLength Tamanho do array
     * @return true se o índice é válido para o array
     */
    public boolean isWithinBounds(int arrayLength) {
        return value < arrayLength && value >= 0;
    }
}

