# Configuração JitPack

Este projeto está configurado para funcionar com JitPack.

## Como usar no seu projeto

Adicione ao seu `build.gradle.kts`:

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.SEU_USUARIO:CommandFramework:VERSION")
}
```

## Notas importantes

1. **Group ID**: O group ID será `com.github.SEU_USUARIO` onde `SEU_USUARIO` é o seu nome de usuário do GitHub
2. **Versões**: Use tags do Git (ex: `v0.1.0`) ou commits (ex: `abc1234`)
3. **Build**: O JitPack vai compilar automaticamente quando você fizer o primeiro build

## Configuração do build.gradle.kts

O projeto já está configurado com:
- ✅ Java 21
- ✅ Maven Publish
- ✅ Dependências corretas (api vs compileOnly)
- ✅ Javadoc e Sources JARs

## Troubleshooting

Se o JitPack falhar:
1. Verifique se o repositório é público
2. Verifique se há uma tag/commit válido
3. Veja os logs em: https://jitpack.io/com/github/SEU_USUARIO/CommandFramework/VERSION/build.log

