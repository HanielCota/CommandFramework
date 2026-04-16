# IntelliJ live templates

`live-templates.xml` contains five live templates to reduce boilerplate when writing
commands. Import them through **Settings → Editor → Live Templates → Import...** and
select `ide/live-templates.xml`.

| Abbrev | Expands to |
|---|---|
| `cfcmd` | `@Command` class skeleton with a root `@Execute` method. |
| `cfexec` | An additional `@Execute(sub = "...")` subcommand method with `@Description`. |
| `cfcool` | A `@Cooldown(value, unit, bypassPermission)` annotation. |
| `cfconfirm` | A `@Confirm(expireSeconds, commandName)` annotation. |
| `cfresolver` | A custom `ArgumentResolver<T>` skeleton. |

Templates apply inside Java declarations (inside a class body for `cfcool`/`cfconfirm`, at
top level for `cfcmd`/`cfresolver`).
