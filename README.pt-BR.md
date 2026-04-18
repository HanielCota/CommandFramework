<div align="center">

**🌐 Idioma / Language:** [English](./README.md) · **Português (BR)**

# CommandFramework

**Crie comandos para plugins de Minecraft escrevendo classes Java simples.**
**Sem comandos no `plugin.yml`. Sem encanamento de Brigadier. Funciona em Paper e Velocity.**

[![Build](https://github.com/HanielCota/CommandFramework/actions/workflows/build.yml/badge.svg)](https://github.com/HanielCota/CommandFramework/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/HanielCota/CommandFramework?label=release&color=0b7)](https://github.com/HanielCota/CommandFramework/releases/latest)
[![JitPack](https://img.shields.io/jitpack/version/com.github.HanielCota/CommandFramework.svg)](https://jitpack.io/#HanielCota/CommandFramework)
[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/25/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

</div>

---

## 📖 O que é isso?

CommandFramework é uma **biblioteca** que você adiciona ao seu plugin de Minecraft
(Paper ou Velocity). Em vez de escrever toda a boilerplate que Paper/Velocity normalmente
exigem - command maps, declarações no `plugin.yml`, parsing de argumentos, checagens de
permissão, cooldowns, tab-complete, telas de ajuda - você escreve apenas uma classe Java
com algumas anotações, e tudo o mais é conectado automaticamente.

**Exemplo do fluxo inteiro:**

```java
@Command(name = "heal")
@Permission("myplugin.heal")
public final class HealCommand {
    @Execute
    public void run(@Sender Player player) {
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
    }
}
```

Coloque essa classe no pacote `commands` do seu plugin, e `/heal` funciona no jogo -
com permissão, tab-complete, sugestão "você quis dizer" se alguém digitar errado um
subcomando, e um botão clicável de confirmação quando você adicionar `@Confirm`. Só isso.

---

## ✅ Para quem é?

- Desenvolvedores construindo um plugin Paper (servidor Minecraft 1.21+) ou plugin de proxy Velocity.
- Devs que sabem Java o suficiente para escrever uma classe com um método, mas não querem decorar toda a API do Bukkit/Brigadier só para adicionar um `/kit`.
- Times que querem **uma única base de código de comandos** rodando tanto em Paper quanto em Velocity.

**Você não precisa saber:** Brigadier, truques de `plugin.yml`, reflection, ou detalhes do annotation processor. O framework esconde tudo isso.

---

## 📋 Antes de começar - pré-requisitos

| Você precisa | Para quê | Como obter |
|---|---|---|
| **JDK 25** | O projeto e os plugins gerados compilam para Java 25. | [Adoptium Temurin 25](https://adoptium.net/) ou SDKMAN `sdk install java 25-tem` |
| **Gradle 9 (ou Maven)** | Build tool que baixa a biblioteca do JitPack. | Use o wrapper `./gradlew` que os templates de Paper/Velocity já incluem. |
| **Um esqueleto de plugin Paper ou Velocity** | Onde você vai colocar suas classes de comando. | [Template de plugin Paper](https://docs.papermc.io/paper/dev/getting-started/paper-plugins) ou o [template IntelliJ do Velocity](https://docs.papermc.io/velocity/creating-your-first-plugin) |
| **Uma IDE (IntelliJ recomendado)** | Processamento de anotações + autocomplete. | [IntelliJ Community](https://www.jetbrains.com/idea/download/) é gratuito. |

> **Nunca fez plugin?** Comece pelo guia oficial do Paper
> [*Getting Started*](https://docs.papermc.io/paper/dev/getting-started/project-setup) - construa um hello-world, veja ele carregar num servidor, depois volte aqui.

---

## 🗂️ Índice

1. [Instalação](#-instalação)
2. [Seu primeiro comando - tutorial de 10 minutos](#-seu-primeiro-comando--tutorial-de-10-minutos)
3. [Aprenda por exemplo](#-aprenda-por-exemplo)
   - [Subcomandos](#subcomandos)
   - [Argumentos e tipos](#argumentos-e-tipos)
   - [Permissões](#permissões)
   - [Cooldowns](#cooldowns)
   - [Confirmações](#confirmações)
   - [Somente-jogador e assíncrono](#somente-jogador-e-assíncrono)
   - [Injeção de dependência](#injeção-de-dependência)
   - [Tipos de argumento customizados](#tipos-de-argumento-customizados)
   - [Middlewares (auditoria, métricas, tracing)](#middlewares)
   - [Rate limiting](#rate-limiting)
4. [Mensagens - totalmente configuráveis](#-mensagens--totalmente-configuráveis)
5. [Testando seus comandos](#-testando-seus-comandos)
6. [Cola de anotações](#-cola-de-anotações)
7. [Cola do builder](#-cola-do-builder)
8. [Troubleshooting](#-troubleshooting)
9. [FAQ](#-faq)
10. [Glossário](#-glossário)
11. [Arquitetura (para quem tem curiosidade)](#-arquitetura-para-quem-tem-curiosidade)
12. [Compatibilidade](#-compatibilidade)
13. [Contribuindo e Licença](#-contribuindo)

---

## 📦 Instalação

Você tem **três formas** de adicionar o CommandFramework ao seu plugin. Escolha uma.

### Opção 1 - JitPack (recomendado, sem configuração)

Adicione o repositório JitPack e o módulo da sua plataforma.

**`settings.gradle.kts`:**
```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://jitpack.io")
    }
}
```

**`build.gradle.kts` (plugin Paper):**
```kotlin
dependencies {
    implementation("io.github.hanielcota.commandframework:paper:0.2.2")
    annotationProcessor("io.github.hanielcota.commandframework:processor:0.2.2")
}
```

**`build.gradle.kts` (plugin Velocity):**
```kotlin
dependencies {
    implementation("io.github.hanielcota.commandframework:velocity:0.2.2")
    annotationProcessor("io.github.hanielcota.commandframework:processor:0.2.2")
}
```

> **O que é o `annotationProcessor`?** Ele roda em tempo de compilação, lê suas
> anotações `@Command` / `@Execute` / `@Arg`, valida elas, e gera um arquivo
> pequeno que o framework usa para encontrar seus comandos em tempo de execução.
> Você não interage com ele - só precisa estar no classpath para `scanPackage(...)` funcionar.

### Opção 2 - Plugin Gradle (configuração em uma linha)

Se você usa Gradle, o plugin nativo conecta tudo (dependência da plataforma,
annotation processor, toolchain Java 25) para você:

```kotlin
plugins {
    java
    id("io.github.hanielcota.commandframework") version "0.2.2"
}

commandframework {
    platform.set("paper")   // ou "velocity"
    version.set("0.2.2")
}
```

### Opção 3 - Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.github.hanielcota.commandframework</groupId>
        <artifactId>paper</artifactId>
        <version>0.2.2</version>
    </dependency>
    <dependency>
        <groupId>io.github.hanielcota.commandframework</groupId>
        <artifactId>processor</artifactId>
        <version>0.2.2</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

---

## 📂 Estrutura de pacotes - de onde importar

Tudo no framework vive em uma destas três raízes. **Não existem sub-pacotes** tipo `.message`, `.middleware`, `.resolver` ou `.actor` - não deixe uma IA alucinar esses nomes.

| Você quer | Importar de |
| --- | --- |
| Anotações (`@Command`, `@Execute`, `@Arg`, `@Sender`, `@Permission`, `@Cooldown`, `@Confirm`, `@Async`, `@Optional`, `@Inject`, `@RequirePlayer`, `@Description`) | `io.github.hanielcota.commandframework.annotation.*` |
| Tipos do runtime (`CommandActor`, `CommandContext`, `CommandResult`, `CommandMiddleware`, `ArgumentResolver`, `MessageKey`, `MessageProvider`) | `io.github.hanielcota.commandframework.*` |
| Bridge do Paper (`PaperCommandFramework`) | `io.github.hanielcota.commandframework.paper.*` |
| Bridge do Velocity (`VelocityCommandFramework`) | `io.github.hanielcota.commandframework.velocity.*` |
| Testkit (`CommandTestKit`, `TestSender`, `DispatchAssert`) | `io.github.hanielcota.commandframework.testkit.*` |

Se a IDE sugerir um import terminado em `.message.`, `.middleware.`, `.resolver.` ou `.actor.`, está errado - apague e importe da raiz plana acima.

---

## 🚀 Seu primeiro comando - tutorial de 10 minutos

Assumindo que você já tem um projeto de plugin Paper funcionando (um `onEnable` vazio,
`paper-plugin.yml` - ou `plugin.yml` legado - declarado). Vamos adicionar um comando `/heal`.

### Passo 1 - Adicione a dependência

Veja a seção [Instalação](#-instalação) acima.

### Passo 2 - Inicialize o framework no `onEnable`

```java
package com.example.myplugin;

import io.github.hanielcota.commandframework.paper.PaperCommandFramework;
import org.bukkit.plugin.java.JavaPlugin;

public final class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        PaperCommandFramework.paper(this)
            .scanPackage("com.example.myplugin.commands")
            .build();
    }
}
```

**O que isso faz:**
- `scanPackage(...)` diz ao framework: "procure nesse pacote Java qualquer classe
  anotada com `@Command` e registre automaticamente."
- `build()` valida tudo e conecta no registrar moderno de comandos do Paper
  (Brigadier). Nenhuma alteração em `plugin.yml` necessária.

### Passo 3 - Escreva o comando

Crie `src/main/java/com/example/myplugin/commands/HealCommand.java`:

```java
package com.example.myplugin.commands;

import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Execute;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.Sender;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

@Command(name = "heal", description = "Curar um jogador")
@Permission("myplugin.heal")
public final class HealCommand {

    @Execute
    public void healSelf(@Sender Player player) {
        // Paper 1.20.5+ usa a API de Attribute; getMaxHealth() legado está deprecated.
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.sendMessage(Component.text("Você foi totalmente curado."));
    }
}
```

### Passo 4 - Compile e rode

```bash
./gradlew build
```

Coloque o jar gerado em `plugins/` no servidor e reinicie. No jogo:

- `/heal` funciona para qualquer jogador com a permissão `myplugin.heal`.
- Sem a permissão: o framework envia a mensagem `NO_PERMISSION` automaticamente (configurável - veja [Mensagens](#-mensagens--totalmente-configuráveis)).
- Tab-complete, sugestões para subcomandos desconhecidos e ajuda para `/heal ?` já estão conectados.

**🎉 Você escreveu um comando.** Agora vamos ver tudo o mais que dá para fazer.

---

## 📚 Aprenda por exemplo

Cada seção abaixo mostra uma feature em um snippet mínimo rodável. Todos os `@`
importam de `io.github.hanielcota.commandframework.annotation.*`.

### Subcomandos

Uma classe pode ter um executor "raiz" (sem `sub`) e quantos subcomandos quiser:

```java
@Command(name = "eco", description = "Comandos de economia")
public final class EconomyCommand {

    @Execute
    public void balance(@Sender Player player) {
        // /eco  → roda esse
    }

    @Execute(sub = "pay")
    public void pay(@Sender Player player, Player target, double amount) {
        // /eco pay <jogador> <quantidade>
    }

    @Execute(sub = "reset")
    public void reset(@Sender Player player, Player target) {
        // /eco reset <jogador>
    }
}
```

> **Aliases:** `@Command(name = "eco", aliases = {"dinheiro", "bal"})` faz `/dinheiro` e `/bal` funcionarem também.

### Argumentos e tipos

**Tipos nativos, convertidos automaticamente** a partir do que o jogador digitou:
`String`, `int` / `Integer`, `long` / `Long`, `double` / `Double`,
`float` / `Float`, `boolean` / `Boolean`, `UUID`, e qualquer `enum`.

- **Paper** também fornece `Player` e `World`.
- **Velocity** também fornece `Player`.

```java
@Execute(sub = "give")
public void give(@Sender Player sender, Player target, int amount) {
    // /eco give <alvo> <quantidade>
    // "target" faz tab-complete de jogadores online. Nome errado → mensagem de erro.
    // "amount" precisa virar int. "abc" → erro tipado.
}
```

**Argumentos opcionais** - use um padrão quando estiver faltando:

```java
@Execute(sub = "give")
public void give(@Sender Player player, Player target, double amount,
                 @Optional("false") boolean silent) {
    // `silent` vira false se o jogador não digitar.
}
```

**Greedy (guloso)** - captura a linha inteira como uma `String`:

```java
@Execute(sub = "say")
public void say(@Sender Player player, @Arg(greedy = true) String message) {
    // /eco say Olá mundo, tudo bem?
    // → message = "Olá mundo, tudo bem?"
}
```

> Apenas um argumento greedy por método, e precisa ser o **último** parâmetro.
> O annotation processor pega isso em tempo de compilação.

### Permissões

Permissões ficam na classe (valem para todos os métodos) ou em um método específico (sobrepõe):

```java
@Command(name = "admin")
@Permission("myplugin.admin")              // exigido em todo método abaixo
public final class AdminCommand {

    @Execute
    public void panel(@Sender Player player) { /* precisa de myplugin.admin */ }

    @Execute(sub = "ban")
    @Permission("myplugin.admin.ban")      // requisito mais restrito só para /admin ban
    public void ban(@Sender Player player, Player target) { /* ... */ }
}
```

### Cooldowns

Por remetente, por comando. O cooldown só é aplicado **depois** dos argumentos serem
validados, então errar digitação não trava você.

```java
import java.util.concurrent.TimeUnit;

@Execute
@Cooldown(value = 30, unit = TimeUnit.SECONDS, bypassPermission = "myplugin.heal.bypass")
public void heal(@Sender Player player) { /* ... */ }
```

Jogadores com `myplugin.heal.bypass` pulam o cooldown por completo.

### Confirmações

Para ações destrutivas, peça a confirmação do jogador. O framework envia um
**botão `[Confirmar]` clicável** (Adventure `<click:run_command>`); se ele não clicar
dentro da janela, a invocação é descartada.

```java
@Execute(sub = "wipe")
@Permission("myplugin.admin.wipe")
@Confirm(expireSeconds = 10, commandName = "confirmar")
public void wipe(@Sender Player player) {
    // Só roda se o jogador clicar em [Confirmar] (ou digitar /confirmar) em 10s.
}
```

### Somente-jogador e assíncrono

```java
@Command(name = "home")
@RequirePlayer                    // console recebe um erro tipado "só jogador"
public final class HomeCommand {

    @Execute
    @Async                        // roda em virtual thread - seguro para DB/HTTP
    public void home(@Sender CommandActor actor) {
        UUID playerId = actor.uniqueId();
        Location h = database.loadHome(playerId);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.teleport(h);
            }
        });
    }
}
```

### Injeção de dependência

Registre um serviço no builder; o framework injeta em todos os comandos.

```java
// No seu onEnable:
var economy = new EconomyService(database);
var kits = new KitService();

PaperCommandFramework.paper(this)
    .bind(EconomyService.class, economy)
    .bind(KitService.class, kits)
    .scanPackage("com.example.myplugin.commands")
    .build();
```

```java
@Command(name = "bal")
public final class BalanceCommand {

    @Inject private EconomyService economy;        // ← injetado automaticamente

    @Execute
    public void balance(@Sender Player player) {
        double saldo = this.economy.getBalance(player.getUniqueId());
        player.sendMessage(Component.text("Saldo: " + saldo));
    }
}
```

`JavaPlugin` (Paper) e `ProxyServer` (Velocity) já ficam registrados automaticamente -
você não precisa chamar `.bind(...)` neles.

### Tipos de argumento customizados

Se você quer que `/kit <nome>` aceite seu próprio tipo `Kit` (com autocomplete),
implemente `ArgumentResolver<Kit>`:

```java
public final class KitResolver implements ArgumentResolver<Kit> {
    private final KitService kits;
    public KitResolver(KitService kits) { this.kits = kits; }

    @Override public Class<Kit> type() { return Kit.class; }

    @Override
    public Kit resolve(ArgumentResolutionContext ctx, String input)
            throws ArgumentResolveException {
        return this.kits.find(input).orElseThrow(() ->
                new ArgumentResolveException("kit", input, "Kit desconhecido"));
    }

    @Override
    public List<String> suggest(CommandActor actor, String currentInput) {
        return this.kits.names().stream()
                .filter(name -> name.startsWith(currentInput))
                .toList();
    }
}
```

Registre:

```java
PaperCommandFramework.paper(this)
    .resolver(new KitResolver(kits))
    .build();
```

Agora qualquer parâmetro de método com tipo `Kit` funciona automaticamente:

```java
@Execute(sub = "give")
public void give(@Sender Player player, Kit kit) { /* ... */ }
```

### Middlewares

Um middleware roda **em volta** de todo comando - útil para auditoria, métricas, tracing.

```java
public final class AuditMiddleware implements CommandMiddleware {
    private final AuditLog log;
    public AuditMiddleware(AuditLog log) { this.log = log; }

    @Override
    public CommandResult handle(CommandContext ctx, Chain chain) {
        // ctx.rawArguments() retorna uma única String (ex. "kill Notch"),
        // NÃO um String[]. Para a lista tokenizada use ctx.arguments() : List<String>.
        String raw = ctx.rawArguments();
        this.log.write(ctx.actor(), ctx.label(), raw);
        CommandResult result = chain.proceed(ctx);
        this.log.result(ctx.actor(), ctx.label(), result);
        return result;
    }
}
```

```java
builder.middleware(new AuditMiddleware(auditLog));
```

Middlewares rodam na ordem em que são registrados.

### Rate limiting

Um limitador de janela fixa bloqueia spam de comandos. Padrão: **30 comandos em 10 segundos** por remetente (console não conta). Para mudar:

```java
builder.rateLimit(50, Duration.ofMinutes(1));
```

---

## 💬 Mensagens - totalmente configuráveis

Todo erro, prompt e linha de ajuda é um template **MiniMessage** que você customiza.

### Sobrepor uma mensagem inline

```java
builder.message(MessageKey.NO_PERMISSION, "<red>Você não tem permissão para isso.");
```

### Carregar de um arquivo YAML (recomendado)

Coloque um `messages.yml` nos resources do seu plugin:

```yaml
# messages.yml
no-permission: "<red>Você não tem permissão para isso."
cooldown-active: "<yellow>Aguarde <bold>{remaining}</bold>."
confirm-prompt: "<yellow>Clique <click:run_command:'/{command}'><green>[Confirmar]</green></click> em {seconds}s."
```

Carregue no startup:

```java
this.saveResource("messages.yml", false);
var yaml = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
var templates = yaml.getKeys(false).stream()
        .collect(Collectors.toMap(k -> k, yaml::getString));

PaperCommandFramework.paper(this)
    .messages(MessageProvider.fromStringMap(templates))
    .scanPackage("com.example.myplugin.commands")
    .build();
```

`fromStringMap` tolera `no-permission`, `no_permission` e `NO_PERMISSION`
indiferentemente, e qualquer chave omitida cai no template padrão.

**Referência completa** - todas as chaves e seus placeholders:

| Chave | Placeholders | Quando dispara |
|---|---|---|
| `PLAYER_ONLY` | - | Não-jogador tentou um comando `@RequirePlayer`. |
| `NO_PERMISSION` | - | Remetente sem `@Permission`. |
| `INVALID_ARGUMENT` | `{name}`, `{input}` | Resolver rejeitou o input. |
| `MISSING_ARGUMENT` | `{name}` | Argumento obrigatório não foi fornecido. |
| `TOO_MANY_ARGUMENTS` | `{input}` | Tokens a mais que a assinatura. |
| `COOLDOWN_ACTIVE` | `{remaining}` | Janela do `@Cooldown` ainda aberta. |
| `COMMAND_ERROR` | - | Handler lançou exceção não tratada. |
| `CONFIRM_PROMPT` | `{command}`, `{seconds}` | Prompt clicável de confirmação. |
| `CONFIRM_NOTHING_PENDING` | - | Usuário rodou o comando de confirmação sem nada pendente. |
| `HELP_HEADER` | `{command}` | Linha de topo da ajuda gerada. |
| `HELP_ENTRY` | `{usage}`, `{description}` | Uma linha por executor na ajuda. |
| `UNKNOWN_SUBCOMMAND` | `{typed}`, `{command}`, `{suggestion}` | Sugestão "você quis dizer" para typos. |

Um `messages.yml` pronto para copiar vive em
[`examples/paper-sample/src/main/resources/messages.yml`](./examples/paper-sample/src/main/resources/messages.yml).

---

## 🧪 Testando seus comandos

Adicione o testkit ao seu `testImplementation` para testar comandos unitariamente
sem subir um servidor:

```kotlin
testImplementation("io.github.hanielcota.commandframework:core-testkit:0.2.2")
```

```java
@Test
void healCommandFunciona() {
    var env = CommandTestKit.create();
    var result = env.framework(new HealCommand())
            .player("Alice").grant("myplugin.heal")
            .dispatch("heal", "");

    DispatchAssert.assertThat(result).succeeded();
}
```

Veja o código-fonte de [`CommandTestKit`](./core-testkit/src/main/java/io/github/hanielcota/commandframework/testkit/CommandTestKit.java)
para a API fluente completa (assertivas de cooldown, permissão, confirmação).

---

## 🏷️ Cola de anotações

| Anotação | Colocar em | O que faz |
|---|---|---|
| `@Command(name, aliases, description, permission)` | classe | Declara um comando. `name` é o label; `aliases` são alternativas. |
| `@Execute(sub)` | método | Declara um executor. Sem `sub` = raiz (`/comando`). Com `sub = "foo"` = subcomando (`/comando foo`). |
| `@Sender` | parâmetro | Marca qual parâmetro recebe o jogador/console. Opcional se o tipo é inequívoco. |
| `@Arg(value, greedy, maxLength)` | parâmetro | Sobrepõe nome/regras do argumento. Use `greedy=true` para capturar o resto da linha. |
| `@Optional(value)` | parâmetro | Valor padrão quando o argumento está faltando. |
| `@Description(value)` | método | Texto mostrado na ajuda auto-gerada. |
| `@Permission(value)` | classe ou método | Protege um comando (método sobrepõe a classe). |
| `@Cooldown(value, unit, bypassPermission)` | método | Cooldown por remetente; bypass opcional. |
| `@Confirm(expireSeconds, commandName)` | método | Exige confirmação clicável/tipada. |
| `@RequirePlayer` | classe ou método | Console recebe rejeição tipada. |
| `@Async` | método | Roda em virtual thread; use para DB / HTTP. |
| `@Inject` | campo | Injeta uma dependência registrada via `builder.bind(...)`. |

---

## 🛠️ Cola do builder

Todos os métodos retornam `this` (encadeie). Chame `.build()` por último.

| Método | Para que serve |
|---|---|
| `scanPackage(String)` | Carrega toda classe `@Command` do pacote. Precisa do annotation processor. |
| `command(Object)` | Registra um único comando já instanciado manualmente. |
| `commands(Object...)` | Mesmo, para vários. |
| `bind(Class<T>, T)` | Registra um serviço para `@Inject` / construtores de resolver. |
| `resolver(ArgumentResolver<?>)` | Registra um tipo de argumento customizado. |
| `middleware(CommandMiddleware)` | Registra um middleware em volta de todo dispatch. |
| `message(MessageKey, String)` | Sobrepõe um template de mensagem. |
| `messages(MessageProvider)` | Substitui o provider inteiro (use `MessageProvider.fromStringMap(...)`). |
| `rateLimit(int, Duration)` | Configura o limitador por remetente. |
| `debug(boolean)` | Loga cada fase do dispatch (útil quando algo "não dispara"). |
| `build()` | Valida, registra, retorna o `CommandFramework<S>` vivo. |

---

## 🔧 Troubleshooting

### "Meu comando não aparece no jogo"

- Você chamou `.build()`? Sem ele nada é registrado.
- A classe do comando está no pacote que você passou pro `scanPackage(...)` (ou um subpacote)?
- Você adicionou o **annotation processor** como dependência? Sem ele `scanPackage` não acha nada.
- Rebuilde com `./gradlew clean build`. O descriptor gerado é produzido em tempo de compilação.
- Habilite `.debug(true)` no builder e observe o console do servidor para os traces de dispatch.

### "Erro de compilação: @Arg(greedy = true) must be the last parameter"

É o annotation processor fazendo o trabalho dele. Mova o parâmetro greedy
para o fim da assinatura do método, ou use não-greedy (string) caso contrário.

### "Unknown command" no jogo

- Verifique que o plugin foi registrado corretamente (o framework usa o ciclo
  Brigadier do Paper - declarações legadas no `plugin.yml` **não** são necessárias,
  mas o plugin em si precisa carregar).
- Tente `.debug(true)` - se o dispatcher nunca loga seu comando, o literal
  nunca foi registrado.

### "Meu campo `@Inject` está null"

- O tipo precisa estar registrado no builder: `builder.bind(MyService.class, myService)`.
- Ou o tipo precisa ser `JavaPlugin`/`ProxyServer`, que já vêm pré-registrados.
- O campo precisa ser **não-final**, não-estático. O framework seta via reflection após instanciar.

### "Resolver de jogador diz 'Player not found' pra um nome válido"

O resolver nativo chama `server.getPlayerExact(name)` - o jogador precisa estar
**online** e o nome precisa bater exatamente. Sensibilidade a maiúsculas segue a configuração do servidor.

### `NoClassDefFoundError: net/kyori/adventure/text/minimessage/MiniMessage`

Você fez um jar sem shadow. Os jars publicados `paper` / `velocity` relocam o
MiniMessage do Adventure; se você produz seu próprio fat-jar, use o plugin
Gradle Shadow ou declare o MiniMessage no seu POM.

### "Cooldown não aplica"

- Só execuções **bem-sucedidas** gravam cooldown. Se o parsing falha, o cooldown não entra.
- Jogadores com a `bypassPermission` pulam o cooldown por completo.

---

## ❓ FAQ

**Ainda preciso de um `plugin.yml` (ou `paper-plugin.yml`)?**
Sim - o Paper precisa de *algum* descritor de plugin pra achar sua classe
`main`. O que você não precisa é da seção `commands:`.

No **Paper 1.20.5+** o sample oficial (`examples/paper-sample`) usa o formato
moderno `paper-plugin.yml` - essa é a escolha recomendada. O `plugin.yml`
legado ainda funciona, mas o Paper imprime um aviso de deprecation no load.

`src/main/resources/paper-plugin.yml` mínimo:

```yaml
name: MyPlugin
main: com.example.myplugin.MyPlugin
version: 1.0.0
api-version: '1.21'
```

Se você usa filtragem de recursos do Gradle, lembre de casar o nome:

```kotlin
tasks.processResources {
    filesMatching("paper-plugin.yml") { expand("version" to project.version) }
}
```

**Um plugin pode registrar dúzias de comandos?**
Pode. `scanPackage("...")` varre toda classe `@Command`. Não há limite rígido.

**Funciona no Spigot?**
Não - a API de ciclo de vida do Brigadier do Paper é obrigatória. Use Paper 1.20.6+.

**Funciona com Kotlin?**
Funciona, desde que você mantenha `@Command`/`@Execute` em métodos comuns
(não extension functions). Retorno `Unit` do Kotlin é tratado como `void`.

**Bloqueia a thread do servidor?**
Só se *o corpo do seu comando* bloquear. Use `@Async` para chamadas de DB/rede.

**Como escrevo subcomandos de vários níveis tipo `/admin player ban`?**
Não escreve. `@Execute(sub = "...")` aceita apenas um token. Use um rótulo
único como `player_ban` / `playerban`, ou quebre o fluxo em comandos
separados.

**Onde os comandos são declarados para `plugin.yml` / `paper-plugin.yml`?**
Não são. O framework registra direto no registrar Brigadier do Paper via
`LifecycleEvents.COMMANDS`, que é o caminho moderno e recomendado.

---

## 📘 Glossário

| Termo | Significado |
|---|---|
| **Adapter / Bridge** | O módulo fino (`paper`, `velocity`) que traduz entre a API do servidor e o dispatcher do framework. |
| **Actor** | Remetente neutro de plataforma - jogador ou console. Use `CommandActor` para escrever comandos agnósticos de plataforma. |
| **Brigadier** | A biblioteca de comandos da Mojang que vem com o Minecraft. Dá tab-complete no cliente. |
| **Dispatcher** | O componente central que pega um comando digitado e decide que método chamar. |
| **Executor** | Um método anotado com `@Execute`. A coisa que de fato roda. |
| **MiniMessage** | Formato de texto do Adventure (`<red>`, `<hover>`, `<click>`). Usado em templates de mensagem. |
| **Resolver** | Conversor de string crua (o que o jogador digitou) para objeto tipado (`Player`, `Kit`, etc.). |
| **Middleware** | Código que envolve todo dispatch - roda antes e depois do executor. |
| **Annotation processor** | Ferramenta de tempo de compilação que lê anotações e gera um pequeno arquivo de metadata que o framework usa para achar comandos em runtime. Não é dependência de runtime. |

---

## 🏗️ Arquitetura (para quem tem curiosidade)

```
+-------------------+         +-----------------------+
|  Plugin Paper     |         |  Plugin Velocity      |
|  onEnable(...)    |         |  @Plugin ctor(...)    |
+----------+--------+         +----------+------------+
           |                             |
           v                             v
+----------+----------+         +--------+-----------+
| PaperCommandFw      |         | VelocityCommandFw  |
| (extends Builder)   |         | (extends Builder)  |
+----------+----------+         +--------+-----------+
           |                             |
           |   .bind / .resolver /       |
           |   .middleware / .build()    |
           v                             v
+----------+-----------------------------+------------+
|              CommandFrameworkBuilder (core)         |
|                                                     |
|  InternalCommandBuilder ----> CommandFramework<S>   |
|     - carrega descriptors gerados                   |
|     - valida metadata / assinaturas                 |
|     - injeta campos                                 |
|     - monta o CommandDispatcher                     |
+-----------+-----------------------------------------+
            |
            v
+-----------+--------------------------+
| CommandDispatcher (pipeline)         |
|  1. permissão     4. parse argumento |
|  2. só-jogador    5. confirmação     |
|  3. cooldown      6. executa         |
|                      (sync / virtual |
|                       thread)        |
+--------------------------------------+
             ^           ^
             |           |
     CooldownMgr   ConfirmationMgr    RateLimiter
     (Caffeine)    (Caffeine)         (Caffeine)
```

### Layout dos módulos

| Módulo | Para que serve |
|---|---|
| `annotations/` | Classes de anotação (`@Command`, `@Execute`, …). Leve - sem dependências de runtime. |
| `core/` | Dispatcher, resolvers, managers, serviço de mensagem. Agnóstico de plataforma. |
| `paper/` | Adapter do Paper usando `LifecycleEvents.COMMANDS` Brigadier. |
| `velocity/` | Adapter do Velocity usando `BrigadierCommand`. |
| `processor/` | Validador de tempo de compilação + gerador de descriptor. |
| `core-testkit/` | `CommandTestKit` + `TestSender` + `DispatchAssert`. |
| `gradle-plugin/` | Plugin Gradle opcional que conecta tudo. |

Os jars `paper` e `velocity` são publicados com Caffeine/ClassGraph relocados para
`io.github.hanielcota.commandframework.libs.*` pra evitar conflito com outros plugins.

---

## 🧾 Compatibilidade

| Componente | Versão |
|---|---|
| Java | 25 (toolchain) |
| Gradle | 9.4.1 (via wrapper) |
| API do Paper | `26.1.2.build.7-alpha` (MC 1.21+) |
| API do Velocity | `3.4.0-SNAPSHOT` |
| Adventure | 4.26.1 |
| MiniMessage | Já vem dentro do jar shaded |
| Annotation processing | Obrigatório para `scanPackage(...)` |

Plugins Paper precisam usar a API **moderna** do Paper (lifecycle events do
Brigadier). `JavaPlugin.getCommand(...)` legado não é usado.

---

## 🤖 Usando com LLMs (Cursor, Copilot, Claude Code, ChatGPT)

Este repo inclui um [`llms.txt`](./llms.txt) na raiz - um resumo denso, legível
por máquina, de toda anotação pública, método do builder, message key e receita
comum, seguindo a [convenção llms.txt](https://llmstxt.org). LLMs que indexam o
repo pegam automaticamente; você também pode colar no contexto do chat quando
quiser sugestões mais precisas.

### Template de prompt (copie e cole)

> Você está escrevendo um plugin Paper 1.21+ usando **CommandFramework 0.2.2**
> (https://github.com/HanielCota/CommandFramework). Sempre importe anotações de
> `io.github.hanielcota.commandframework.annotation.*`. Use `@Command` em
> classes, `@Execute` em métodos, `@Sender Player` como primeiro parâmetro.
> Nunca declare comandos no `plugin.yml`. Nunca use `JavaPlugin.getCommand()`,
> `CommandExecutor` ou `TabExecutor`. Mensagens são templates MiniMessage
> (`<red>`, `<click:run_command:'/foo'>`). Use `@Async` para DB/HTTP e volte
> para a thread principal do Paper antes de tocar APIs thread-confined.
> `Bukkit.getScheduler()`. Para a referência completa da API, consulte o
> `llms.txt` na raiz do repositório. Se uma feature não estiver nessa
> referência, diga isso em vez de inventar anotações.

### O que tem no `llms.txt`

- Lista exaustiva de anotações com todo atributo e default.
- Enum `MessageKey` completo com nomes de placeholders.
- Resolvers de argumento nativos e como escrever um custom.
- Referência dos métodos do builder com descrição de uma linha.
- Dez "erros comuns" para LLMs não alucinarem `@Route` / `@Handler` / `@Subcommand`.
- Receitas de copy-paste para injection, confirm, async, mensagens via YAML.

---

## 🤝 Contribuindo

- Veja [`CONTRIBUTING.md`](./CONTRIBUTING.md) para setup local e convenções de PR.
- O changelog vive em [`CHANGELOG.md`](./CHANGELOG.md).
- Issues e PRs bem-vindos em [HanielCota/CommandFramework](https://github.com/HanielCota/CommandFramework).

## 📄 Licença

MIT. Veja [`LICENSE`](./LICENSE).

---

<div align="center">

Construído com cuidado para redes Paper e Velocity.
Mantido por [@HanielCota](https://github.com/HanielCota).

</div>
