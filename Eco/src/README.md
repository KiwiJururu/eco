# Living Ecology 0.0.1 — somente `src`

Copie esta pasta `src` para a raiz de um MDK Forge 1.20.1.

## Protótipo implementado

- Cow: rebanho, pânico local, memória de ameaça, proteção ambiental, repouso, home range.
- Wolf: matilha, retirada quando ferido, compartilhamento de alvos, flanqueamento simples, defesa territorial, guerra com Zombies, fronteira regulada com Spiders.
- Spider: ninho, defesa por zonas, teias graduais, uso territorial, simbiose local com Zombies, fronteira regulada com Wolves.
- Zombie: horda, propagação de alvo, memória de última posição, ajuda a Spiders, guerra com Wolves, consolidação territorial e chefe de horda.
- Atributos 0-100, personalidade, estados 0-V, Adaptação situacional, Únicos/Chefes.
- Territórios persistentes comprimidos (`SavedData`), relações adquiridas, terra de ninguém, conquista simples.
- Situação Ambiental esparsa e recuperação por tempo lógico.
- Simulação ecológica acelerável via `/livingecology timescale <0.01..100>`.
- Pegada de teias com orçamento físico fixo; aumentar a timescale não aumenta explosivamente alterações de blocos.

## Debug

Obtenha o item:

`/give @s livingecology:debug_analyzer`

- Clique direito em Cow/Wolf/Spider/Zombie: mostra atributos, estados, adaptação, memória, território, relações e ambiente.
- Clique direito em um bloco ou no ar: inspeciona a ecologia local.

Comandos:

- `/livingecology timescale` — consulta escala ecológica.
- `/livingecology timescale 10` — acelera apenas a simulação abstrata.
- `/livingecology debug` — relatório ecológico da posição atual.

## Filosofia de tempo

A lógica usa `ServerLevel#getGameTime()` e diferenças de ticks. Não há `System.currentTimeMillis()`, `sleep`, nem loops de catch-up por tick perdido. A simulação offline resolve o tempo decorrido matematicamente. O orçamento de materialização de blocos permanece por tick real do servidor.

## Dependências

Nenhuma biblioteca de terceiros é exigida pelo código do protótipo além de Minecraft/Forge do MDK.

## Nota sobre o MDK

Esta entrega contém somente `src`, como solicitado. Se o seu MDK ainda estiver com o identificador do mod de exemplo em `gradle.properties`/`build.gradle`, mantenha o workspace e altere apenas esse identificador para `livingecology` para que ele corresponda ao `@Mod` e ao `mods.toml` desta pasta.

Para o primeiro teste, substitua a pasta `src` de exemplo do MDK por esta e rode o cliente pelo Gradle do próprio MDK.
