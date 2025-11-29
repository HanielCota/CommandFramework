package com.github.hanielcota.commandframework.cooldown;

import java.util.UUID;

/**
 * Chave única para identificar um cooldown específico.
 * Combina o UUID do sender, nome do comando e nome do subcomando.
 *
 * @param senderId       UUID do jogador que executou o comando
 * @param commandName   Nome do comando
 * @param subCommandName Nome do subcomando (pode ser null para comando default)
 */
public record CooldownKey(UUID senderId, String commandName, String subCommandName) {
}


