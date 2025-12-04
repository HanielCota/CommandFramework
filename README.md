<div align="center">

# 🚀 Command Framework

**Uma framework moderna, performática e completa para criação de comandos no Minecraft**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Paper](https://img.shields.io/badge/Paper-1.21+-blue.svg)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Brigadier](https://img.shields.io/badge/Brigadier-1.0.18-yellow.svg)](https://github.com/Mojang/brigadier)

*Framework baseada em Brigadier para Paper/Purpur 1.21+ com suporte completo a subcomandos, cooldowns, permissões, tab completion e muito mais.*

</div>

---

## 📋 Índice

- [✨ Características](#-características)
- [📦 Instalação](#-instalação)
- [🚀 Início Rápido](#-início-rápido)
- [📚 Guia Completo](#-guia-completo)
  - [Criando Comandos](#criando-comandos)
  - [Subcomandos](#subcomandos)
  - [Anotações Disponíveis](#anotações-disponíveis)
  - [Parsers de Argumentos](#parsers-de-argumentos)
  - [Tab Completion](#tab-completion)
  - [Injeção de Dependências](#injeção-de-dependências)
  - [Cooldown](#cooldown)
  - [Permissões](#permissões)
  - [Execução Assíncrona](#execução-assíncrona)
  - [Registro de Comandos](#registro-de-comandos)
- [🏗️ Arquitetura](#️-arquitetura)
- [⚡ Performance](#-performance)
- [🔒 Segurança](#-segurança)
- [🐛 Tratamento de Erros](#-tratamento-de-erros)
- [📝 Exemplos Avançados](#-exemplos-avançados)
- [🤝 Contribuindo](#-contribuindo)

---

## ✨ Características

### 🎯 Principais Funcionalidades

- ✅ **100% Baseada em Brigadier** - Integração nativa com o sistema de comandos do Minecraft
- ✅ **Registro Automático** - Scan automático por reflection, sem necessidade de `plugin.yml`
- ✅ **Subcomandos Multi-nível** - Suporte completo a subcomandos aninhados
- ✅ **Tab Completion Inteligente** - Sugestões estáticas e dinâmicas baseadas em tipos
- ✅ **Sistema de Cooldown** - Cooldown por comando/subcomando com cache eficiente
- ✅ **Permissões Granulares** - Permissões por comando, subcomando ou método
- ✅ **Execução Assíncrona** - Suporte nativo a operações assíncronas
- ✅ **Parsers Customizados** - Sistema extensível de parsing de argumentos
- ✅ **Thread-Safe** - Nunca bloqueia a main thread
- ✅ **Coexistência Segura** - Não sobrescreve comandos vanilla sem permissão explícita

### 🏆 Qualidade de Código

- ✅ **Clean Code** - Código limpo, legível e manutenível
- ✅ **Objects Calisthenics** - Segue todas as 9 regras rigorosamente
- ✅ **Java 21 Moderno** - Records, sealed interfaces, pattern matching, text blocks
- ✅ **Null-Safe** - Null-check defensivo, early-return, Optional em tudo
- ✅ **Caffeine Cache** - Caching interno de alta performance
- ✅ **Lombok** - Redução de boilerplate mantendo boas práticas

---

## 📦 Instalação

### Via JitPack

Adicione ao seu `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    
    implementation("com.github.hanielcota:CommandFramework:VERSION")
}
```

### Build Local

```bash
./gradlew build publishToMavenLocal
```

Depois adicione ao seu projeto:

```kotlin
repositories {
    mavenLocal()
}

dependencies {
    implementation("com.github.hanielcota:CommandFramework:0.1.0-SNAPSHOT")
}
```

---

## 🚀 Início Rápido

### 1. Configurar o Framework

```java
public class MeuPlugin extends JavaPlugin {
    
    private CommandFramework commandFramework;
    
    @Override
    public void onEnable() {
        // Cria e configura o framework
        commandFramework = new CommandFramework(this);
        
        // Setup automático (escaneia o pacote base do plugin)
        commandFramework.setup();
        
        // OU especifique um pacote customizado
        // commandFramework.setup("com.seuprojeto.commands");
    }
    
    @Override
    public void onDisable() {
        if (commandFramework != null) {
            commandFramework.close();
        }
    }
}
```

### 2. Criar seu Primeiro Comando

```java
@Command(
    name = "hello",
    description = "Um comando simples de exemplo",
    aliases = {"hi", "ola"}
)
public class HelloCommand {
    
    @DefaultCommand
    public Component execute(CommandSender sender) {
        return Component.text()
            .append(Component.text("Olá, ", NamedTextColor.GREEN))
            .append(Component.text(sender.getName(), NamedTextColor.YELLOW))
            .append(Component.text("!", NamedTextColor.GREEN))
            .build();
    }
}
```

**Pronto!** O comando `/hello` já está funcionando! 🎉

---

## 📚 Guia Completo

### Criando Comandos

#### Estrutura Básica

Todo comando precisa:
1. Anotação `@Command` na classe
2. Pelo menos um método com `@DefaultCommand` ou `@SubCommand`

```java
@Command(
    name = "gamemode",
    description = "Gerencia o gamemode dos jogadores",
    aliases = {"gm", "gmode"},
    overrideVanilla = false  // true para sobrescrever comando vanilla
)
public class GamemodeCommand {
    
    @DefaultCommand
    public Component defaultHandler(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return Component.text("Este comando só pode ser usado por jogadores.", 
                NamedTextColor.RED);
        }
        
        var current = player.getGameMode();
        return Component.text()
            .append(Component.text("Seu gamemode atual: ", NamedTextColor.GRAY))
            .append(Component.text(current.name(), NamedTextColor.GREEN))
            .build();
    }
}
```

#### Tipos de Retorno

Os handlers podem retornar diferentes tipos:

| Tipo | Descrição | Exemplo |
|------|-----------|---------|
| `void` | Nenhuma mensagem enviada | `public void silent(CommandSender sender) {}` |
| `String` | Convertido automaticamente para Component | `return "Mensagem simples";` |
| `Component` | Mensagem formatada com Adventure API | `return Component.text("Olá!");` |
| `CommandResult` | Resultado estruturado (Success/Failure/NoOp) | `return CommandResult.success("OK");` |
| `CompletionStage<?>` | Para operações assíncronas | `return CompletableFuture.supplyAsync(...);` |

### Subcomandos

#### Subcomandos Simples

```java
@Command(name = "home")
public class HomeCommand {
    
    @DefaultCommand
    public Component list(CommandSender sender) {
        return Component.text("Lista de homes...");
    }
    
    @SubCommand("set")
    @RequiredPermission("home.set")
    public Component setHome(CommandSender sender, String homeName) {
        // Lógica para criar home
        return Component.text("Home '" + homeName + "' criada!");
    }
    
    @SubCommand("delete")
    @RequiredPermission("home.delete")
    public Component deleteHome(CommandSender sender, String homeName) {
        // Lógica para deletar home
        return Component.text("Home '" + homeName + "' deletada!");
    }
}
```

**Uso:**
- `/home` → executa `list()`
- `/home set spawn` → executa `setHome()` com `homeName = "spawn"`
- `/home delete spawn` → executa `deleteHome()` com `homeName = "spawn"`

#### Subcomandos Multi-nível

```java
@Command(name = "player")
public class PlayerCommand {
    
    @SubCommand("info")
    public Component info(CommandSender sender, Player target) {
        return Component.text("Informações do jogador: " + target.getName());
    }
    
    @SubCommand("ban")
    @RequiredPermission("player.ban")
    public Component ban(CommandSender sender, Player target, String reason) {
        // Lógica de ban
        return Component.text("Jogador " + target.getName() + " banido!");
    }
    
    @SubCommand("unban")
    @RequiredPermission("player.unban")
    public Component unban(CommandSender sender, String playerName) {
        // Lógica de unban
        return Component.text("Jogador " + playerName + " desbanido!");
    }
}
```

**Uso:**
- `/player info Notch` → executa `info()` com `target = Player("Notch")`
- `/player ban Notch Spam` → executa `ban()` com `target = Player("Notch")` e `reason = "Spam"`

### Anotações Disponíveis

#### `@Command`

Define uma classe como comando principal.

```java
@Command(
    name = "meucomando",           // Nome do comando (obrigatório)
    description = "Descrição",      // Descrição do comando
    aliases = {"alias1", "alias2"}, // Aliases alternativos
    overrideVanilla = false         // true para sobrescrever comandos vanilla
)
```

#### `@SubCommand`

Define um método como subcomando.

```java
@SubCommand("set")                    // Subcomando simples
@SubCommand("player set")             // Subcomando multi-nível
@SubCommand(value = "set", description = "Define um valor")  // Com descrição
```

#### `@DefaultCommand`

Marca o método que será executado quando o comando for chamado sem subcomandos.

```java
@DefaultCommand
public Component defaultHandler(CommandSender sender) {
    return Component.text("Comando padrão");
}
```

#### `@RequiredPermission`

Define permissão necessária para executar o comando ou subcomando.

```java
// Na classe (aplica a todos os métodos)
@RequiredPermission("meuplugin.comando.use")
@Command(name = "comando")
public class MeuComando { }

// No método (sobrescreve a permissão da classe)
@SubCommand("admin")
@RequiredPermission("meuplugin.comando.admin")
public Component admin(CommandSender sender) { }
```

#### `@Cooldown`

Aplica cooldown ao comando/subcomando.

```java
// Na classe (aplica a todos os métodos)
@Cooldown(seconds = 5)
@Command(name = "comando")
public class MeuComando { }

// No método (sobrescreve o cooldown da classe)
@SubCommand("spam")
@Cooldown(seconds = 10)
public Component spam(CommandSender sender) { }
```

#### `@Async`

Marca o método para execução assíncrona.

```java
@Async
@SubCommand("heavy")
public CompletionStage<Component> heavyOperation(CommandSender sender) {
    return CompletableFuture.supplyAsync(() -> {
        // Operação pesada (banco de dados, API, etc.)
        return Component.text("Operação concluída!");
    });
}
```

#### `@TabCompletion`

Define sugestões para tab completion (veja seção [Tab Completion](#tab-completion)).

### Parsers de Argumentos

#### Parsers Built-in

A framework já inclui parsers para os seguintes tipos:

| Tipo | Exemplo de Uso |
|------|----------------|
| `String` | `public void cmd(CommandSender sender, String texto)` |
| `Integer` | `public void cmd(CommandSender sender, Integer numero)` |
| `Boolean` | `public void cmd(CommandSender sender, Boolean valor)` |
| `UUID` | `public void cmd(CommandSender sender, UUID id)` |
| `Player` | `public void cmd(CommandSender sender, Player jogador)` |
| `OfflinePlayer` | `public void cmd(CommandSender sender, OfflinePlayer jogador)` |
| `GameMode` | `public void cmd(CommandSender sender, GameMode modo)` |
| `World` | `public void cmd(CommandSender sender, World mundo)` |
| `Duration` | `public void cmd(CommandSender sender, Duration duracao)` |
| `Enum` | `public void cmd(CommandSender sender, MeuEnum valor)` |

#### Exemplo com Parsers Built-in

```java
@Command(name = "teleport")
public class TeleportCommand {
    
    @SubCommand("player")
    @RequiredPermission("teleport.player")
    public Component teleportToPlayer(
        CommandSender sender, 
        Player target,      // Parser automático de Player
        World world         // Parser automático de World
    ) {
        if (!(sender instanceof Player player)) {
            return Component.text("Apenas jogadores podem usar este comando.", 
                NamedTextColor.RED);
        }
        
        player.teleport(target.getLocation());
        return Component.text("Teleportado para " + target.getName() + "!");
    }
    
    @SubCommand("coords")
    public Component teleportToCoords(
        CommandSender sender,
        Integer x,          // Parser automático de Integer
        Integer y,         // Parser automático de Integer
        Integer z          // Parser automático de Integer
    ) {
        if (!(sender instanceof Player player)) {
            return Component.text("Apenas jogadores podem usar este comando.", 
                NamedTextColor.RED);
        }
        
        player.teleport(new Location(player.getWorld(), x, y, z));
        return Component.text("Teleportado para " + x + ", " + y + ", " + z + "!");
    }
}
```

#### Parser Customizado

Crie seu próprio parser implementando `ArgumentParser<T>`:

```java
public class ItemStackParser implements ArgumentParser<ItemStack> {
    
    @Override
    public String name() {
        return "item";
    }
    
    @Override
    public Class<ItemStack> type() {
        return ItemStack.class;
    }
    
    @Override
    public ArgumentType<?> brigadierType() {
        return StringArgumentType.word();
    }
    
    @Override
    public Optional<ItemStack> parse(CommandContext<CommandSender> context, String name) {
        var input = StringArgumentType.getString(context, name);
        
        try {
            var material = Material.valueOf(input.toUpperCase());
            return Optional.of(new ItemStack(material));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
```

**Registrar o parser:**

```java
@Override
public void onEnable() {
    commandFramework = new CommandFramework(this);
    commandFramework.setup();
    
    // Registrar parser customizado
    commandFramework.configureParserRegistry(registry -> {
        registry.register(new ItemStackParser());
    });
}
```

**Usar o parser:**

```java
@Command(name = "give")
public class GiveCommand {
    
    @SubCommand("item")
    @RequiredPermission("give.item")
    public Component giveItem(
        CommandSender sender,
        Player target,
        ItemStack item,    // Usa o parser customizado
        Integer amount
    ) {
        item.setAmount(amount);
        target.getInventory().addItem(item);
        return Component.text("Item dado com sucesso!");
    }
}
```

### Tab Completion

#### Sugestões Estáticas

```java
@Command(name = "gamemode")
public class GamemodeCommand {
    
    @SubCommand("set")
    public Component setGamemode(
        CommandSender sender,
        @TabCompletion("survival", "creative", "adventure", "spectator") GameMode mode
    ) {
        // ...
    }
}
```

#### Sugestões Dinâmicas (Provider)

Crie um provider customizado:

```java
public class PlayerSuggestionProvider implements SuggestionProvider<CommandSender> {
    
    @Override
    public CompletableFuture<Suggestions> getSuggestions(
        CommandContext<CommandSender> context,
        SuggestionsBuilder builder
    ) {
        var input = builder.getRemaining().toLowerCase();
        
        Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(input))
            .forEach(builder::suggest);
        
        return builder.buildFuture();
    }
}
```

**Usar o provider:**

```java
@Command(name = "teleport")
public class TeleportCommand {
    
    @SubCommand("player")
    public Component teleport(
        CommandSender sender,
        @TabCompletion(provider = PlayerSuggestionProvider.class) Player target
    ) {
        // ...
    }
}
```

#### Tab Completion no Método

Você também pode anotar o método inteiro:

```java
@SubCommand("set")
@TabCompletion("survival", "creative", "adventure", "spectator")
public Component setGamemode(CommandSender sender, GameMode mode) {
    // ...
}
```

#### Providers com Dependências

Providers de tab completion podem receber dependências através do construtor. Para isso, você precisa registrar as dependências no framework:

```java
// Provider que precisa de uma dependência
@RequiredArgsConstructor
public class WarpSuggestionProvider implements SuggestionProvider<CommandSender> {
    
    private final WarpManager warpManager;
    
    @Override
    public CompletableFuture<Suggestions> getSuggestions(
        CommandContext<CommandSender> context,
        SuggestionsBuilder builder
    ) {
        String input = builder.getRemaining().toLowerCase();
        
        warpManager.getWarpsSorted().stream()
            .map(Warp::name)
            .filter(name -> name.toLowerCase().startsWith(input))
            .forEach(builder::suggest);
        
        return builder.buildFuture();
    }
}
```

**Registrar a dependência:**

```java
@Override
public void onEnable() {
    commandFramework = new CommandFramework(this);
    commandFramework.setup();
    
    // Criar e registrar dependências
    WarpManager warpManager = new WarpManager();
    commandFramework.registerDependency(WarpManager.class, warpManager);
    
    // Agora o WarpSuggestionProvider será criado automaticamente com o WarpManager
}
```

**Usar o provider:**

```java
@Command(name = "warp")
public class WarpCommand {
    
    @SubCommand("to")
    public Component teleport(
        CommandSender sender,
        @TabCompletion(provider = WarpSuggestionProvider.class) String warpName
    ) {
        // ...
    }
}
```

### Injeção de Dependências

O framework possui um sistema de injeção de dependências que funciona tanto para comandos quanto para providers de tab completion.

#### Dependências do Framework

O framework resolve automaticamente as seguintes dependências:

- `Plugin` - Instância do seu plugin
- `ArgumentParserRegistry` - Registry de parsers
- `CommandExecutor` - Executor de comandos
- `CooldownService` - Serviço de cooldown
- `GlobalErrorHandler` - Handler de erros global

#### Dependências Customizadas

Para usar dependências customizadas (managers, serviços, etc.), você precisa registrá-las:

```java
@Override
public void onEnable() {
    commandFramework = new CommandFramework(this);
    commandFramework.setup();
    
    // Registrar dependências customizadas
    WarpManager warpManager = new WarpManager();
    EconomyService economyService = new EconomyService();
    DatabaseService databaseService = new DatabaseService();
    
    commandFramework.registerDependency(WarpManager.class, warpManager);
    commandFramework.registerDependency(EconomyService.class, economyService);
    commandFramework.registerDependency(DatabaseService.class, databaseService);
}
```

#### Comandos com Dependências

Comandos podem receber dependências através do construtor:

```java
@Command(name = "warp")
public class WarpCommand {
    
    private final WarpManager warpManager;
    
    // O framework injeta automaticamente se WarpManager estiver registrado
    public WarpCommand(WarpManager warpManager) {
        this.warpManager = warpManager;
    }
    
    @DefaultCommand
    public Component list(CommandSender sender) {
        var warps = warpManager.getAllWarps();
        // ...
    }
}
```

**Importante:** Dependências devem ser registradas **antes** de registrar comandos ou usar providers que as requerem.

#### Providers com Dependências

Providers de tab completion também podem receber dependências:

```java
@RequiredArgsConstructor
public class WarpSuggestionProvider implements SuggestionProvider<CommandSender> {
    
    private final WarpManager warpManager;
    
    @Override
    public CompletableFuture<Suggestions> getSuggestions(
        CommandContext<CommandSender> context,
        SuggestionsBuilder builder
    ) {
        // Usa warpManager para fornecer sugestões
        // ...
    }
}
```

Veja a seção [Tab Completion](#tab-completion) para mais detalhes.

### Cooldown

O sistema de cooldown é automático e eficiente, usando cache em memória.

```java
@Command(name = "heal")
public class HealCommand {
    
    // Cooldown de 5 segundos para todos os métodos
    @Cooldown(seconds = 5)
    @Command(name = "heal")
    public class HealCommand {
        
        @DefaultCommand
        public Component heal(CommandSender sender) {
            if (!(sender instanceof Player player)) {
                return Component.text("Apenas jogadores!", NamedTextColor.RED);
            }
            
            player.setHealth(player.getMaxHealth());
            return Component.text("Você foi curado!", NamedTextColor.GREEN);
        }
        
        // Cooldown de 10 segundos apenas para este método
        @SubCommand("full")
        @Cooldown(seconds = 10)
        public Component fullHeal(CommandSender sender) {
            if (!(sender instanceof Player player)) {
                return Component.text("Apenas jogadores!", NamedTextColor.RED);
            }
            
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20);
            return Component.text("Cura completa aplicada!", NamedTextColor.GREEN);
        }
    }
}
```

**Mensagens de cooldown:**

Quando um jogador tenta usar o comando durante o cooldown, uma mensagem automática é enviada informando o tempo restante.

### Permissões

#### Permissão na Classe

Aplica a todos os métodos do comando:

```java
@RequiredPermission("meuplugin.comando.use")
@Command(name = "comando")
public class MeuComando {
    
    @DefaultCommand
    public Component defaultHandler(CommandSender sender) {
        // Requer "meuplugin.comando.use"
        return Component.text("Comando executado!");
    }
    
    @SubCommand("admin")
    public Component admin(CommandSender sender) {
        // Também requer "meuplugin.comando.use"
        return Component.text("Admin!");
    }
}
```

#### Permissão no Método

Sobrescreve a permissão da classe:

```java
@RequiredPermission("meuplugin.comando.use")
@Command(name = "comando")
public class MeuComando {
    
    @DefaultCommand
    public Component defaultHandler(CommandSender sender) {
        // Requer "meuplugin.comando.use"
        return Component.text("Comando executado!");
    }
    
    @SubCommand("admin")
    @RequiredPermission("meuplugin.comando.admin")  // Permissão diferente
    public Component admin(CommandSender sender) {
        // Requer "meuplugin.comando.admin"
        return Component.text("Admin!");
    }
}
```

### Execução Assíncrona

Para operações pesadas (banco de dados, APIs, etc.), use `@Async`:

```java
@Command(name = "stats")
public class StatsCommand {
    
    @SubCommand("player")
    @Async
    public CompletionStage<Component> getPlayerStats(
        CommandSender sender,
        OfflinePlayer player
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // Operação pesada (banco de dados, API, etc.)
            var stats = database.getPlayerStats(player.getUniqueId());
            
            return Component.text()
                .append(Component.text("Estatísticas de ", NamedTextColor.GRAY))
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("Kills: ", NamedTextColor.GRAY))
                .append(Component.text(stats.getKills(), NamedTextColor.GREEN))
                .build();
        });
    }
}
```

### Registro de Comandos

#### Registro Automático (Scan)

O método `setup()` escaneia automaticamente o pacote e registra todos os comandos:

```java
@Override
public void onEnable() {
    commandFramework = new CommandFramework(this);
    commandFramework.setup();  // Escaneia o pacote base do plugin
}
```

#### Registro Manual

Para comandos que precisam de dependências (managers, serviços, etc.), você tem duas opções:

**Opção 1: Registrar dependências e usar scan automático (Recomendado)**

```java
@Override
public void onEnable() {
    commandFramework = new CommandFramework(this);
    commandFramework.setup();
    
    // Criar e registrar dependências
    BackLocationManager backManager = new BackLocationManager();
    commandFramework.registerDependency(BackLocationManager.class, backManager);
    
    // O comando será registrado automaticamente pelo scan
    // e receberá o BackLocationManager através do construtor
}
```

**Opção 2: Registrar comando manualmente**

```java
@Override
public void onEnable() {
    commandFramework = new CommandFramework(this);
    commandFramework.setup();
    
    // Criar dependências
    BackLocationManager backManager = new BackLocationManager();
    
    // Registrar comando com dependências
    commandFramework.register(new BackCommand(backManager));
}
```

**Importante:** 
- Comandos registrados manualmente têm **prioridade** sobre o scan automático. Se o scan encontrar um comando já registrado manualmente, ele será ignorado automaticamente.
- Dependências devem ser registradas **antes** de usar comandos ou providers que as requerem.

#### Registro por Classe

Você também pode registrar apenas a classe (o framework cria a instância):

```java
commandFramework.register(MeuComando.class);
```

**Nota:** Isso só funciona se a classe tiver construtor padrão ou construtor com parâmetros que o framework consegue resolver automaticamente (Plugin, ArgumentParserRegistry, etc.).

#### Registro de Pacote Customizado

```java
// Assíncrono (padrão)
commandFramework.registerPackage("com.seuprojeto.commands");

// Síncrono (garante que comandos estejam registrados antes de continuar)
commandFramework.registerPackageSync("com.seuprojeto.commands");
```

---

## 🏗️ Arquitetura

### Estrutura de Packages

```
com.github.hanielcota.commandframework
├── annotation          # Anotações (@Command, @SubCommand, etc.)
├── adapter             # Adaptadores para Bukkit
├── brigadier           # Integração com Brigadier
├── cache               # Caches Caffeine
├── cooldown            # Sistema de cooldown
├── error               # Tratamento de erros
├── execution           # Execução de comandos
├── framework           # Configuração e inicialização
├── messaging           # Sistema de mensagens
├── parser              # Parsers de argumentos
├── processor           # Processamento de comandos
├── registry            # Scanner e registro
└── value               # Value objects
```

### Princípios Aplicados

#### Clean Code
- ✅ Nomes significativos
- ✅ Métodos pequenos
- ✅ Baixa complexidade cognitiva
- ✅ Zero duplicação
- ✅ Sem `else` (early-return)
- ✅ Estrutura limpa

#### Objects Calisthenics
1. ✅ Um nível de indentação por método
2. ✅ Não usar `else`
3. ✅ Envolver primitivos quando fizer sentido (records)
4. ✅ Nomes pequenos e significativos
5. ✅ Classes pequenas
6. ✅ Métodos pequenos
7. ✅ Objetos imutáveis como padrão
8. ✅ Evitar coleções grandes
9. ✅ Sem getters/setters desnecessários (records)

---

## ⚡ Performance

### Otimizações Implementadas

- **Caffeine Cache** para:
  - Parsers de argumentos
  - Métodos anotados
  - Subcomandos e seus parts
  - Tab completions
  - Instâncias de handlers
  - Dispatcher Brigadier

- **Thread-Safe**: Nunca bloqueia a main thread
- **Async por padrão**: Scan e processamento assíncronos
- **Early-return**: Reduz branching desnecessário
- **Cache de metadata**: Anotações e reflexão são cacheadas

### Benchmarks

O framework foi projetado para ser extremamente performático, com:
- Registro de comandos em < 1ms por comando
- Execução de comandos com overhead mínimo
- Tab completion otimizado com cache

---

## 🔒 Segurança

### Coexistência com Comandos Vanilla

A framework **nunca sobrescreve comandos vanilla** a menos que explicitamente solicitado:

```java
@Command(
    name = "gamemode",
    overrideVanilla = true  // Apenas com esta flag
)
```

Sem a flag, comandos vanilla continuam funcionando normalmente.

### Verificação de Duplicatas

O framework automaticamente previne registro duplicado de comandos:
- Comandos registrados manualmente têm prioridade sobre scan automático
- Scan automático ignora comandos já registrados
- Logs informativos sobre comandos ignorados/substituídos

---

## 🐛 Tratamento de Erros

A framework trata automaticamente os seguintes erros:

| Erro | Tratamento |
|------|------------|
| Falta de permissão | Mensagem automática via Adventure API |
| Argumento inválido | Mensagem de erro formatada |
| Target offline | Mensagem informativa |
| Subcomando não encontrado | Sugestão de subcomandos disponíveis |
| Erro interno | Log detalhado + mensagem genérica ao usuário |
| Parsing error | Mensagem de erro específica do parser |
| Cooldown ativo | Mensagem com tempo restante |

Todas as mensagens são enviadas via **Adventure API** com formatação **MiniMessage**.

---

## 📝 Exemplos Avançados

### Exemplo Completo: Sistema de Warps

```java
// Provider de sugestões para warps
@RequiredArgsConstructor
public class WarpSuggestionProvider implements SuggestionProvider<CommandSender> {
    
    private final WarpManager warpManager;
    
    @Override
    public CompletableFuture<Suggestions> getSuggestions(
        CommandContext<CommandSender> context,
        SuggestionsBuilder builder
    ) {
        String input = builder.getRemaining().toLowerCase();
        
        warpManager.getWarpsSorted().stream()
            .map(Warp::name)
            .filter(name -> name.toLowerCase().startsWith(input))
            .forEach(builder::suggest);
        
        return builder.buildFuture();
    }
}

// Comando de warps
@Command(
    name = "warp",
    description = "Sistema de teleporte para warps",
    aliases = {"tpwarp", "goto"}
)
@RequiredPermission("warp.use")
public class WarpCommand {
    
    private final WarpManager warpManager;
    
    public WarpCommand(WarpManager warpManager) {
        this.warpManager = warpManager;
    }
    
    @DefaultCommand
    public Component list(CommandSender sender) {
        var warps = warpManager.getAllWarps();
        
        if (warps.isEmpty()) {
            return Component.text("Nenhum warp disponível.", NamedTextColor.RED);
        }
        
        var message = Component.text()
            .append(Component.text("Warps disponíveis: ", NamedTextColor.GREEN))
            .append(Component.newline());
        
        warps.forEach(warp -> 
            message.append(Component.text("- " + warp, NamedTextColor.YELLOW))
                   .append(Component.newline())
        );
        
        return message.build();
    }
    
    @SubCommand("set")
    @RequiredPermission("warp.set")
    @Cooldown(seconds = 3)
    public Component setWarp(
        CommandSender sender,
        @TabCompletion(provider = WarpSuggestionProvider.class) String warpName
    ) {
        if (!(sender instanceof Player player)) {
            return Component.text("Apenas jogadores podem criar warps.", 
                NamedTextColor.RED);
        }
        
        warpManager.setWarp(warpName, player.getLocation());
        return Component.text()
            .append(Component.text("Warp '", NamedTextColor.GREEN))
            .append(Component.text(warpName, NamedTextColor.YELLOW))
            .append(Component.text("' criado com sucesso!", NamedTextColor.GREEN))
            .build();
    }
    
    @SubCommand("delete")
    @RequiredPermission("warp.delete")
    public Component deleteWarp(
        CommandSender sender,
        @TabCompletion(provider = WarpSuggestionProvider.class) String warpName
    ) {
        if (!warpManager.exists(warpName)) {
            return Component.text("Warp não encontrado.", NamedTextColor.RED);
        }
        
        warpManager.deleteWarp(warpName);
        return Component.text()
            .append(Component.text("Warp '", NamedTextColor.GREEN))
            .append(Component.text(warpName, NamedTextColor.YELLOW))
            .append(Component.text("' deletado!", NamedTextColor.GREEN))
            .build();
    }
    
    @SubCommand("to")
    @Cooldown(seconds = 5)
    public Component teleportToWarp(
        CommandSender sender,
        @TabCompletion(provider = WarpSuggestionProvider.class) String warpName
    ) {
        if (!(sender instanceof Player player)) {
            return Component.text("Apenas jogadores podem usar warps.", 
                NamedTextColor.RED);
        }
        
        var location = warpManager.getWarp(warpName);
        if (location == null) {
            return Component.text("Warp não encontrado.", NamedTextColor.RED);
        }
        
        player.teleport(location);
        return Component.text()
            .append(Component.text("Teleportado para '", NamedTextColor.GREEN))
            .append(Component.text(warpName, NamedTextColor.YELLOW))
            .append(Component.text("'!", NamedTextColor.GREEN))
            .build();
    }
}
```

**Registro no plugin:**

```java
@Override
public void onEnable() {
    commandFramework = new CommandFramework(this);
    commandFramework.setup();
    
    // Criar e registrar dependências
    WarpManager warpManager = new WarpManager();
    commandFramework.registerDependency(WarpManager.class, warpManager);
    
    // O comando será registrado automaticamente pelo scan
    // e receberá o WarpManager através do construtor
    // O WarpSuggestionProvider também receberá o WarpManager automaticamente
}
```

### Exemplo: Comando com Múltiplas Dependências

```java
@Command(name = "economy")
public class EconomyCommand {
    
    private final EconomyService economyService;
    private final DatabaseService databaseService;
    private final MessageService messageService;
    
    public EconomyCommand(
        EconomyService economyService,
        DatabaseService databaseService,
        MessageService messageService
    ) {
        this.economyService = economyService;
        this.databaseService = databaseService;
        this.messageService = messageService;
    }
    
    @SubCommand("balance")
    @Async
    public CompletionStage<Component> getBalance(
        CommandSender sender,
        @TabCompletion(provider = PlayerSuggestionProvider.class) OfflinePlayer target
    ) {
        return CompletableFuture.supplyAsync(() -> {
            var balance = economyService.getBalance(target.getUniqueId());
            return messageService.formatBalance(target.getName(), balance);
        });
    }
    
    @SubCommand("pay")
    @RequiredPermission("economy.pay")
    @Cooldown(seconds = 2)
    public Component pay(
        CommandSender sender,
        @TabCompletion(provider = PlayerSuggestionProvider.class) Player target,
        Integer amount
    ) {
        if (!(sender instanceof Player player)) {
            return Component.text("Apenas jogadores podem pagar.", NamedTextColor.RED);
        }
        
        if (amount <= 0) {
            return Component.text("Valor inválido.", NamedTextColor.RED);
        }
        
        if (!economyService.hasBalance(player.getUniqueId(), amount)) {
            return Component.text("Saldo insuficiente.", NamedTextColor.RED);
        }
        
        economyService.transfer(player.getUniqueId(), target.getUniqueId(), amount);
        return Component.text()
            .append(Component.text("Você pagou ", NamedTextColor.GREEN))
            .append(Component.text("$" + amount, NamedTextColor.YELLOW))
            .append(Component.text(" para ", NamedTextColor.GREEN))
            .append(Component.text(target.getName(), NamedTextColor.YELLOW))
            .build();
    }
}
```

**Registro:**

```java
@Override
public void onEnable() {
    commandFramework = new CommandFramework(this);
    commandFramework.setup();
    
    // Criar e registrar serviços
    var economyService = new EconomyService();
    var databaseService = new DatabaseService();
    var messageService = new MessageService();
    
    // Registrar dependências
    commandFramework.registerDependency(EconomyService.class, economyService);
    commandFramework.registerDependency(DatabaseService.class, databaseService);
    commandFramework.registerDependency(MessageService.class, messageService);
    
    // O comando será registrado automaticamente pelo scan
    // e receberá todas as dependências através do construtor
}
```

**Alternativa - Registro Manual:**

Se preferir registrar manualmente:

```java
commandFramework.register(new EconomyCommand(
    economyService,
    databaseService,
    messageService
));
```

---

## 🤝 Contribuindo

Contribuições são muito bem-vindas! Por favor:

1. **Siga os princípios** de Clean Code e Objects Calisthenics
2. **Mantenha a cobertura de testes** alta
3. **Documente mudanças significativas**
4. **Use early-return** e Optional
5. **Mantenha a consistência** com o código existente

### Processo de Contribuição

1. Fork o repositório
2. Crie uma branch para sua feature (`git checkout -b feature/MinhaFeature`)
3. Commit suas mudanças (`git commit -m 'Adiciona MinhaFeature'`)
4. Push para a branch (`git push origin feature/MinhaFeature`)
5. Abra um Pull Request

---

## 📄 Licença

Este projeto está sob a licença **MIT**. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

---

<div align="center">

**Desenvolvido com ❤️ seguindo as melhores práticas de engenharia de software.**

[⭐ Dê uma estrela](https://github.com/hanielcota/CommandFramework) se este projeto te ajudou!

</div>
