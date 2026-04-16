package io.github.hanielcota.commandframework.scanfixtures;

import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Execute;

@Command(name = "manualfixture")
public final class ManualFixtureCommand {

    @Execute
    public void execute() {
    }
}
