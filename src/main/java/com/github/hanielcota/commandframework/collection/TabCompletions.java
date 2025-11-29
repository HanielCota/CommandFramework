package com.github.hanielcota.commandframework.collection;

import com.github.hanielcota.commandframework.value.CompletionLimit;

import java.util.ArrayList;
import java.util.List;

/**
 * Coleção de sugestões de tab completion com limite de tamanho.
 * Evita sobrecarga ao limitar o número de sugestões retornadas.
 */
public class TabCompletions {
    private final List<String> completions;
    private final CompletionLimit limit;

    /**
     * Cria uma coleção com o limite padrão de completions.
     */
    public TabCompletions() {
        this(CompletionLimit.DEFAULT);
    }

    /**
     * Cria uma coleção com um limite específico.
     *
     * @param limit Limite máximo de completions
     */
    public TabCompletions(CompletionLimit limit) {
        this.completions = new ArrayList<>();
        this.limit = limit;
    }

    /**
     * Adiciona uma completion se o limite ainda não foi atingido.
     *
     * @param completion Completion a ser adicionada
     */
    public void add(String completion) {
        if (completion == null) {
            return;
        }
        if (limit.isReached(completions.size())) {
            return;
        }
        completions.add(completion);
    }

    public void addIfStartsWith(String completion, String input) {
        if (completion == null || input == null) {
            return;
        }
        if (!completion.toLowerCase().startsWith(input.toLowerCase())) {
            return;
        }
        add(completion);
    }

    /**
     * Verifica se a coleção está vazia.
     *
     * @return true se não há completions
     */
    public boolean isEmpty() {
        return completions.isEmpty();
    }

    /**
     * Retorna uma cópia imutável da lista de completions.
     *
     * @return Lista de completions
     */
    public List<String> asList() {
        return new ArrayList<>(completions);
    }
}

