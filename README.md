# Titan Mod (NeoForge 1.21.1)

- **M1**: entidade do Titã com vida (1000), escudo regenerável (400), dono e comando de spawn.
- **M2**: embarcar, pilotar (WASD + mouse), dash com 2 cargas e HUD do piloto.

## Como rodar
1. Requisitos: JDK 21 e IntelliJ IDEA (ou VS Code com extensões Java).
2. Abra esta pasta como projeto Gradle e espere a sincronização.
3. Rode `./gradlew runClient` (Windows: `gradlew.bat runClient`).
4. Crie um mundo com cheats e use `/titan spawn`.

## Controles
- **Clique direito** no Titã (sendo o dono): embarcar. Titã sem dono passa a ser de quem clicar primeiro.
- **Agachado + clique direito**: mostra vida e escudo.
- **WASD + mouse**: o Titã anda e gira para onde você mira.
- **C**: dash na direção em que você anda (2 cargas, 3 s para recuperar cada uma). Dá para trocar a tecla em Controles.
- **Shift**: desembarcar (padrão do Minecraft para veículos).

## Comandos (precisam de OP)
- `/titan spawn`: cria um Titã onde você está
- `/titan shield <0-400>`: ajusta o escudo do Titã mais próximo
- `/titan clear`: remove seus Titãs

## Pontos para ajustar no teste
- Altura da cabine: `.passengerAttachments(4.0F)` em `registry/ModEntities.java`.
- Velocidade: atributo `MOVEMENT_SPEED` e `DASH_STRENGTH` em `entity/TitanEntity.java`.

## Estrutura
- `entity/`: TitanEntity, TitanAIMode
- `registry/`: ModEntities
- `network/`: DashPayload, ModNetwork
- `client/`: renderer placeholder, tecla de dash, eventos de cliente, HUD
- `command/`: /titan
