package com.github.hanielcota.commandframework.collection;

import com.github.hanielcota.commandframework.registry.CommandDefinition;

import java.util.ArrayList;
import java.util.List;

public class CommandDefinitions {
    private final List<CommandDefinition> definitions;

    public CommandDefinitions() {
        this.definitions = new ArrayList<>();
    }

    public CommandDefinitions(List<CommandDefinition> definitions) {
        this.definitions = new ArrayList<>(definitions);
    }

    public void add(CommandDefinition definition) {
        if (definition != null) {
            definitions.add(definition);
        }
    }

    public boolean isEmpty() {
        return definitions.isEmpty();
    }

    public int size() {
        return definitions.size();
    }

    public void forEach(java.util.function.Consumer<CommandDefinition> action) {
        definitions.forEach(action);
    }

    public List<CommandDefinition> asList() {
        return new ArrayList<>(definitions);
    }
}

