package com.github.hanielcota.commandframework.brigadier;

import com.github.hanielcota.commandframework.annotation.Command;
import lombok.Builder;
import lombok.Value;

import java.lang.reflect.Method;
import java.util.List;

@Value
@Builder
public class CommandMetadata {

    Command commandAnnotation;
    Class<?> type;
    Object instance;
    List<Method> handlers;
}

