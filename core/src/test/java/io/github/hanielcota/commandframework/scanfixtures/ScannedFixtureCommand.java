package io.github.hanielcota.commandframework.scanfixtures;

import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Execute;

@Command(name = "scannedfixture")
public final class ScannedFixtureCommand {

    @Execute
    public void execute() {
    }
}
