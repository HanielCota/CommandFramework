# Command Framework - Paper/Purpur + Brigadier

Uma **Command Framework completa, moderna, modular, altamente performática e sustentável** para **Java 21** usando **Paper/Purpur 1.21+** e **Brigadier**, seguindo rigorosamente todas as melhores práticas profissionais de backend, arquitetura limpa e engenharia moderna.

## 🎯 Características Principais

- ✅ **100% baseada em Brigadier** - Integração nativa com o sistema de comandos do Minecraft
- ✅ **Caffeine Cache** - Caching interno de alta performance
- ✅ **Clean Code** - Código limpo, legível e manutenível
- ✅ **Objects Calisthenics** - Segue todas as 9 regras rigorosamente
- ✅ **Java 21 Moderno** - Records, sealed interfaces, pattern matching, text blocks
- ✅ **Null-Safe** - Null-check defensivo, early-return, Optional em tudo
- ✅ **Lombok** - Redução de boilerplate mantendo boas práticas
- ✅ **Thread-Safe** - Nunca bloqueia a main thread
- ✅ **Coexistência com Vanilla** - Não sobrescreve comandos vanilla sem permissão explícita
- ✅ **Registro Automático** - Scan por reflection, sem necessidade de plugin.yml

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
    
    implementation("com.github.seuprojeto:CommandFramework:VERSION")
}
```

### Build Local

```bash
./gradlew build publishToMavenLocal
```

## 🚀 Uso Rápido

### 1. Configurar a Framework no seu Plugin

```java
public class MeuPlugin extends JavaPlugin {
    
    private CommandFramework framework;
    private BukkitAudiences audiences;
    
    @Override
    public void onEnable() {
        // Configurar Adventure API
        audiences = BukkitAudiences.create(this);
        var miniMessage = MiniMessage.miniMessage();
        var messageProvider = new MiniMessageProvider(audiences, miniMessage);
        
        // Criar cache de handlers
        Cache<Class<?>, Object> handlerCache = FrameworkCaches.handlerInstances();
        
        // Criar framework
        framework = CommandFramework.create(this, messageProvider, handlerCache);
        
        // Registrar comandos do pacote
        framework.registerPackage("com.seuprojeto.meuplugin.commands");
    }
    
    @Override
    public void onDisable() {
        if (audiences != null) {
            audiences.close();
        }
    }
}
```

### 2. Criar um Comando

```java
@Command(
    name = "gm",
    description = "Comando de gamemode simplificado",
    aliases = {"gamemode", "gmode"}
)
@RequiredArgsConstructor
public class GamemodeCommand {
    
    @DefaultCommand
    public Component defaultHandler(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return Component.text("Este comando só pode ser usado por jogadores.", NamedTextColor.RED);
        }
        
        var current = player.getGameMode();
        return Component.text()
            .append(Component.text("Seu gamemode atual: ", NamedTextColor.GRAY))
            .append(Component.text(current.name(), NamedTextColor.GREEN))
            .build();
    }
    
    @SubCommand("set")
    @RequiredPermission("framework.gm.set")
    public Component setGamemode(CommandSender sender, GameMode gamemode) {
        if (!(sender instanceof Player player)) {
            return Component.text("Este comando só pode ser usado por jogadores.", NamedTextColor.RED);
        }
        
        player.setGameMode(gamemode);
        return Component.text()
            .append(Component.text("Gamemode alterado para: ", NamedTextColor.GREEN))
            .append(Component.text(gamemode.name(), NamedTextColor.YELLOW))
            .build();
    }
}
```

## 📚 Anotações Disponíveis

### @Command

Define uma classe como comando principal.

```java
@Command(
    name = "meucomando",
    description = "Descrição do comando",
    aliases = {"alias1", "alias2"},
    overrideVanilla = false  // true para sobrescrever comandos vanilla
)
```

### @SubCommand

Define um método como subcomando.

```java
@SubCommand("set")
@SubCommand("player set")  // Subcomandos de dois níveis
```

### @DefaultCommand

Marca o método que será executado quando o comando for chamado sem subcomandos.

```java
@DefaultCommand
public Component defaultHandler(CommandSender sender) {
    // ...
}
```

### @RequiredPermission

Define permissão necessária para executar o comando ou subcomando.

```java
@RequiredPermission("meuplugin.comando.use")
```

### @Async

Marca o método para execução assíncrona.

```java
@Async
public CompletionStage<Component> asyncHandler(CommandSender sender) {
    return CompletableFuture.supplyAsync(() -> {
        // Operação pesada
        return Component.text("Concluído!");
    });
}
```

### @Cooldown

Aplica cooldown ao comando/subcomando.

```java
@Cooldown(seconds = 5)
public Component meuComando(CommandSender sender) {
    // ...
}
```

### @TabCompletion

Define sugestões dinâmicas para argumentos.

```java
@SubCommand("player")
public Component playerCommand(
    CommandSender sender,
    @TabCompletion(provider = PlayerSuggestionProvider.class) Player target
) {
    // ...
}
```

## 🔧 Parsers de Argumentos

### Parsers Built-in

A framework já inclui parsers para:

- `Integer`, `String`, `Boolean`
- `UUID`
- `Player`, `OfflinePlayer`
- `GameMode`
- `World`
- `Duration`
- Enums genéricos

### Parser Customizado

```java
public class MeuParser implements ArgumentParser<MeuTipo> {
    
    @Override
    public String name() {
        return "meutipo";
    }
    
    @Override
    public Class<MeuTipo> type() {
        return MeuTipo.class;
    }
    
    @Override
    public ArgumentType<?> brigadierType() {
        return StringArgumentType.word();
    }
    
    @Override
    public Optional<MeuTipo> parse(CommandContext<CommandSender> context, String name) {
        var input = StringArgumentType.getString(context, name);
        // Lógica de parsing
        return Optional.of(new MeuTipo(input));
    }
}

// Registrar
var registry = ArgumentParserRegistry.create();
registry.register(new MeuParser());
```

## 🎨 Retornos Tipados

Os handlers podem retornar:

- `void` - Nenhuma mensagem
- `String` - Convertido automaticamente para Component
- `Component` - Mensagem formatada
- `CommandResult` - Resultado estruturado (Success, Failure, NoOp)
- `CompletionStage<?>` - Para operações assíncronas

```java
@SubCommand("result")
public CommandResult resultado(CommandSender sender) {
    return CommandResult.success("Operação bem-sucedida!");
}

@SubCommand("async")
@Async
public CompletionStage<Component> async(CommandSender sender) {
    return CompletableFuture.supplyAsync(() -> 
        Component.text("Concluído assincronamente!")
    );
}
```

## 🏗️ Arquitetura

### Estrutura de Packages

```
com.seuprojeto.framework
    ├── annotation          # Anotações (@Command, @SubCommand, etc.)
    ├── brigadier          # Integração com Brigadier
    ├── registry            # Scanner e registro de comandos
    ├── processor           # Processamento e construção de árvores
    ├── parser              # Parsers de argumentos
    ├── execution           # Execução de comandos
    ├── model               # Modelos (CommandResult, etc.)
    ├── cache               # Caches Caffeine
    ├── cooldown            # Sistema de cooldown
    ├── error               # Tratamento de erros
    ├── messaging           # Sistema de mensagens
    └── util                # Utilitários
```

### Princípios Aplicados

#### Clean Code
- Nomes significativos
- Métodos pequenos
- Baixa complexidade cognitiva
- Zero duplicação
- Sem `else` (early-return)
- Estrutura limpa

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

## 🔒 Coexistência com Comandos Vanilla

A framework **nunca sobrescreve comandos vanilla** a menos que explicitamente solicitado:

```java
@Command(
    name = "gamemode",
    overrideVanilla = true  // Apenas com esta flag
)
```

Sem a flag, comandos vanilla continuam funcionando normalmente.

## ⚡ Performance

- **Caffeine Cache** para:
  - Parsers de argumentos
  - Métodos anotados
  - Subcomandos
  - Completions
  - Instâncias de handlers
  - Dispatcher Brigadier

- **Thread-Safe**: Nunca bloqueia a main thread
- **Async por padrão**: Scan e processamento assíncronos
- **Early-return**: Reduz branching desnecessário

## 📖 Exemplos Completos

Veja a pasta `src/main/java/com/seuprojeto/framework/example/` para exemplos de:

- ✅ Comando simples (`/gm`)
- ✅ Override seguro de comando vanilla (`/gamemode`)
- ✅ Subcomandos de dois níveis (`/admin player set`)
- ✅ Comandos assíncronos com `CompletionStage`
- ✅ Parser customizado
- ✅ Sugestões dinâmicas com `SuggestionProvider`

## 🔄 Reload

```java
framework.reload();  // Recarrega comandos sem perder cooldowns
```

## 🐛 Tratamento de Erros

A framework trata automaticamente:

- Falta de permissão
- Argumento inválido
- Target offline
- Subcomando não encontrado
- Erro interno
- Parsing error

Mensagens são enviadas via Adventure API com formatação MiniMessage.

## 📝 Licença

Este projeto está sob licença MIT. Veja o arquivo LICENSE para mais detalhes.

## 🤝 Contribuindo

Contribuições são bem-vindas! Por favor:

1. Siga os princípios de Clean Code e Objects Calisthenics
2. Mantenha a cobertura de testes
3. Documente mudanças significativas
4. Use early-return e Optional

## 📞 Suporte

Para questões e sugestões, abra uma issue no repositório.

---

**Desenvolvido com ❤️ seguindo as melhores práticas de engenharia de software.**
