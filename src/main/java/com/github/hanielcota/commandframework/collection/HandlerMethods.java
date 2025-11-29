package com.github.hanielcota.commandframework.collection;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class HandlerMethods {
    private final List<Method> methods;

    public HandlerMethods() {
        this.methods = new ArrayList<>();
    }

    public HandlerMethods(List<Method> methods) {
        this.methods = new ArrayList<>(methods);
    }

    public void add(Method method) {
        if (method != null) {
            methods.add(method);
        }
    }

    public boolean isEmpty() {
        return methods.isEmpty();
    }

    public int size() {
        return methods.size();
    }

    public List<Method> asList() {
        return new ArrayList<>(methods);
    }

    public void forEach(java.util.function.Consumer<Method> action) {
        methods.forEach(action);
    }
}

