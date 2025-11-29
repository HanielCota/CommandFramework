package com.github.hanielcota.commandframework.adapter;

import com.github.hanielcota.commandframework.value.RemainingArguments;
import com.github.hanielcota.commandframework.value.SubCommandPath;

import java.util.Map;

public class SubCommandFinder {
    private final Map<String, String[]> subCommandPartsCache;

    public SubCommandFinder(Map<String, String[]> subCommandPartsCache) {
        this.subCommandPartsCache = subCommandPartsCache;
    }

    public SubCommandMatch findSubCommand(String[] args, Map<String, java.lang.reflect.Method> subCommands) {
        if (args.length == 0) {
            return null;
        }
        var maxDepth = Math.min(args.length, 3);
        return searchSubCommand(args, maxDepth, subCommands);
    }

    private SubCommandMatch searchSubCommand(String[] args, int maxDepth, Map<String, java.lang.reflect.Method> subCommands) {
        for (int depth = maxDepth; depth >= 1; depth--) {
            var path = buildPath(args, depth);
            if (!subCommands.containsKey(path)) {
                continue;
            }
            return createMatch(path, args, depth);
        }
        return null;
    }

    private String buildPath(String[] args, int depth) {
        var pathBuilder = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            if (i > 0) {
                pathBuilder.append(' ');
            }
            var lowerCase = args[i].toLowerCase();
            pathBuilder.append(lowerCase);
        }
        return pathBuilder.toString();
    }

    private SubCommandMatch createMatch(String path, String[] args, int depth) {
        var remaining = extractRemainingArgs(args, depth);
        return new SubCommandMatch(path, remaining);
    }

    private RemainingArguments extractRemainingArgs(String[] args, int depth) {
        if (depth >= args.length) {
            return new RemainingArguments(new String[0]);
        }
        var remaining = java.util.Arrays.copyOfRange(args, depth, args.length);
        return new RemainingArguments(remaining);
    }

    public record SubCommandMatch(String path, RemainingArguments remainingArgs) {}
}

