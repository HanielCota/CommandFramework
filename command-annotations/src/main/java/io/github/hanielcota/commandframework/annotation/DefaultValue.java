package io.github.hanielcota.commandframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Supplies a default raw value when the caller omits this argument.
 *
 * <p>The value is parsed by the same {@link io.github.hanielcota.commandframework.core.ArgumentResolver}
 * that would normally handle the parameter type.</p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultValue {

    String value();
}
