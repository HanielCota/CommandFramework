package com.github.hanielcota.commandframework.collection;

import com.github.hanielcota.commandframework.value.CompletionLimit;

import java.util.ArrayList;
import java.util.List;

public class TabCompletions {
    private final List<String> completions;
    private final CompletionLimit limit;

    public TabCompletions() {
        this(CompletionLimit.DEFAULT);
    }

    public TabCompletions(CompletionLimit limit) {
        this.completions = new ArrayList<>();
        this.limit = limit;
    }

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

    public boolean isEmpty() {
        return completions.isEmpty();
    }

    public List<String> asList() {
        return new ArrayList<>(completions);
    }
}

