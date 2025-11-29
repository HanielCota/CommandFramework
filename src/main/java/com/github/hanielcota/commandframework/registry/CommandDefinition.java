package com.github.hanielcota.commandframework.registry;

import com.github.hanielcota.commandframework.annotation.Command;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.lang.reflect.Method;
import java.util.List;

@Value
@Builder
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CommandDefinition {

    Command annotation;
    Class<?> type;
    List<Method> handlers;
}


