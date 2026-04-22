module io.github.hanielcota.commandframework.core {
    requires static org.jspecify;
    requires com.github.benmanes.caffeine;
    requires io.github.bucket4j.core;

    exports io.github.hanielcota.commandframework.core;
    exports io.github.hanielcota.commandframework.core.cooldown;
    exports io.github.hanielcota.commandframework.core.dispatch;
    exports io.github.hanielcota.commandframework.core.message;
    exports io.github.hanielcota.commandframework.core.pipeline;
    exports io.github.hanielcota.commandframework.core.rate;
    exports io.github.hanielcota.commandframework.core.route;
    exports io.github.hanielcota.commandframework.core.safety;
    exports io.github.hanielcota.commandframework.core.suggestion;
    exports io.github.hanielcota.commandframework.core.usage;
    exports io.github.hanielcota.commandframework.core.argument;
    exports io.github.hanielcota.commandframework.core.config;
    exports io.github.hanielcota.commandframework.core.metrics;
    exports io.github.hanielcota.commandframework.core.help;
}
