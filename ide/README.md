# IntelliJ live templates

`live-templates.xml` contains seven live templates to reduce boilerplate when writing
commands. Import them through **Settings → Editor → Live Templates → Import...** and
select `ide/live-templates.xml`.

| Abbrev | Expands to |
|---|---|
| `cfcmd` | `@Command` class skeleton with a root `@Execute` method. |
| `cfexec` | An additional `@Execute(sub = "...")` subcommand method with `@Description`. |
| `cfarg` | An `@Arg(name, description)` parameter. |
| `cfinject` | An `@Inject` field for dependency injection. |
| `cfcool` | A `@Cooldown(value, unit, bypassPermission)` annotation. |
| `cfconfirm` | A `@Confirm(expireSeconds, commandName)` annotation. |
| `cfresolver` | A custom `ArgumentResolver<T>` skeleton. |

Templates apply inside Java declarations (inside a class body for `cfcool`/`cfconfirm`/`cfinject`,
at top level for `cfcmd`/`cfresolver`, on method parameter positions for `cfarg`).
