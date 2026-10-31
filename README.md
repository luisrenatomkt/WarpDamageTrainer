# Warp Damage Trainer

Mod **cliente** para **Minecraft 1.8.9 + Forge 11.15.1.2318** que cria um teste de resistência a dano configurável (*Damage Tank Test*), inspirado no conceito de Warp Damage. Interface em português do Brasil.

| Item | Valor |
|---|---|
| Nome / ID | Warp Damage Trainer / `warpdmgtrainer` |
| Versão | 1.1.1 |
| Minecraft / Loader | 1.8.9 / Forge `1.8.9-11.15.1.2318-1.8.9` |
| Build | ForgeGradle 2.1-SNAPSHOT, Gradle **2.14.1**, Java **8**, mappings `stable_22` |

## Estado da validação (leia antes de usar)

* **A compilação com Forge NÃO foi executada** no ambiente em que este projeto foi escrito (sem Gradle, sem acesso à rede, sem os JARs do Minecraft/Forge). O primeiro build real será o do GitHub Actions. É possível que apareça algum erro de compilação ou de resolução de dependência na primeira execução; veja "Solução de problemas".
* **Testado de verdade (28/28 OK):** a lógica pura, sem Minecraft — tabela de dano e delay, temporizador, contagem de golpes, dano configurado × registrado, recorde, serialização do histórico. Rode com `sh tools/run-logic-tests.sh`.
* **Não testado dentro do jogo:** interface, HUD, aplicação do dano, comando e teclas. Nada disso foi executado no Minecraft.

## Remoção automática de itens dropados (novo na 1.1.0)

Enquanto um teste está ativo (rodando ou pausado) **no modo LOCAL (single-player)** e o jogador está vivo, todo item que você dropar (Q, Ctrl+Q, arrastar para fora do inventário) desaparece na hora.

* **Ligar:** automático ao clicar em **INICIAR TESTE** (ou `/warpdmg iniciar`). Não há botão separado.
* **Desligar:** automático ao **morrer** (o evento de morte desarma antes dos drops da morte, então o que cair na morte fica no chão), ao encerrar o teste ou ao sair do mundo. Ao renascer, continua desligado até você iniciar o teste de novo.
* **Como funciona:** o item é cancelado em `EntityJoinWorldEvent` no servidor integrado, antes de existir como entidade. Não há item no chão, acúmulo de entidades, duplicação nem mexida no inventário. Só itens que nascem na posição exata de drop do jogador são removidos; drops de blocos e mobs não são afetados.
* **Multiplayer:** não funciona. O servidor tem a autoridade sobre as entidades de item, e o mod é cliente. Em servidores o modo MONITOR não remove nada. Em partida aberta para LAN, só os seus drops são afetados.
* **HUD:** mostra "Itens: remoção ATIVA (N)" durante o teste.

## Como funciona

**Modo A — Treino local (single-player).** Ao iniciar, a cada intervalo escolhido o mod chama `EntityPlayerMP.attackEntityFrom` no servidor integrado (agendado com `addScheduledTask`). É dano real do jogo: armadura, Resistência, absorção e a invulnerabilidade temporária (10 ticks) são respeitadas. Para cada golpe o mod mede a vida+absorção antes/depois e registra o dano **efetivamente aplicado**; tentativas bloqueadas (criativo, invulnerabilidade temporária, armadura reduzindo a zero) **não** contam como golpe recebido. O teste exige Sobrevivência ou Aventura; caso contrário mostra uma mensagem.

**Modo B — Servidor multiplayer.** Mod 100% cliente não pode fazer o servidor aplicar dano, então em qualquer servidor o mod funciona **somente como monitor**: não envia pacotes, não aplica dano, não mexe em anticheat; apenas registra a perda real de vida e o tempo de sobrevivência (de qualquer fonte de dano). A arquitetura para um servidor que autorize o recurso é a interface `damage/DamageApplier`; só uma implementação feita em conjunto com esse servidor deve ser adicionada. Este projeto não inclui nenhuma.

**Temporizador.** Contagem regressiva por `System.nanoTime()`, consultada a cada tick e a cada frame, sem threads. Nunca dispara mais de um golpe por consulta: se o jogo travar, os intervalos perdidos são apenas contados ("intervalos perdidos por lag"). O relógio congela quando o jogo está pausado (ESC no single-player) ou o teste está em pausa. O primeiro golpe ocorre após um intervalo completo. O golpe executa no próximo tick do servidor (até ~50 ms de atraso, uniforme em todos os golpes).

**Delays ≤ 0,50 s:** a invulnerabilidade temporária do Minecraft reduz ou anula os golpes. O mod informa isso nos resultados (no chat) em vez de contar golpes fictícios.

**Melhor tempo:** considera testes encerrados por morte ou manualmente (interrupções por fechar o mundo não contam). Vale para qualquer combinação de dano/delay; o histórico guarda as configurações de cada teste.

## Uso

* **K** abre o painel (configurável em Controles → "Warp Damage Trainer"). **L** liga/desliga o HUD.
* Painel: escolha dano (1 a 5 corações) e delay (2,00 a 0,25 s), **INICIAR TESTE**, **PAUSAR/RETOMAR**, **ENCERRAR**, **RESETAR ESTATÍSTICAS**, HUD ativado/desativado. Dano/delay só mudam com o teste parado.
* Ao morrer, encerrar ou finalizar o teste, os resultados completos aparecem no **chat** (não há mais tela de resultados).
* Comando `/warpdmg [abrir|iniciar|pausar|encerrar|resetar|hud|status|dano <corações>|delay <s ou ms>]`.
* Configurações e estatísticas: `config/warpdmgtrainer.cfg` (formato Forge).

## Enviar para o GitHub e compilar

1. Crie um repositório vazio em github.com (ex.: `warp-damage-trainer`).
2. Na pasta do projeto:
   ```bash
   git init
   git add .
   git commit -m "Warp Damage Trainer 1.0.0"
   git branch -M main
   git remote add origin https://github.com/SEU_USUARIO/warp-damage-trainer.git
   git push -u origin main
   ```
3. O push na branch `main` (ou `master`) dispara o workflow `.github/workflows/build.yml`. Também dá para rodar manualmente: aba **Actions → Build Warp Damage Trainer → Run workflow**.
4. Ao terminar, abra a execução e baixe o artefato **WarpDamageTrainer-1.1.0** (um .zip contendo o `.jar`).

**Caminho do .jar:** `build/libs/warpdmgtrainer-1.1.1.jar` (no runner e no build local). No GitHub: página da execução → seção *Artifacts*.

O workflow usa Java 8 (Temurin), baixa o **Gradle 2.14.1** (o Gradle do runner é novo demais para ForgeGradle 2.x), roda `gradle setupCiWorkspace build` e falha se a compilação falhar ou se o `.jar` não existir.

**Gradle Wrapper:** não vai no pacote (o `gradle-wrapper.jar` é binário e não pôde ser gerado sem rede). Para gerar localmente, com Gradle 2.14.1 instalado: `gradle wrapper --gradle-version 2.14.1`. O workflow não precisa dele.

**Build local (opcional):** JDK 8 + Gradle 2.14.1 → `gradle setupDecompWorkspace build`.

## Instalar no Minecraft

1. Instale o Minecraft Forge **1.8.9 – 11.15.1.2318** (instalador oficial) e abra o jogo uma vez com o perfil Forge.
2. Copie `warpdmgtrainer-1.1.1.jar` para `.minecraft/mods/`.
3. Abra o Minecraft com o perfil Forge 1.8.9. O mod é só de cliente e não precisa estar no servidor.

## Estrutura

```
src/main/java/com/warpdmgtrainer/
  WarpDamageTrainer.java      classe principal, teclas, registros
  ClientEvents.java           tick, render tick, teclado, abertura de telas
  config/ConfigManager        configurações (.cfg Forge)
  config/PersistenceManager   recorde, total e histórico (.cfg Forge)
  core/PrecisionTimer         temporizador sem rajadas
  core/ResistanceTestManager  máquina de estados do teste
  core/StatsCollector, SessionResult
  damage/                     DamageApplier (local / monitor / ponto de extensão)
  gui/TrainerGui              painel nativo (GuiScreen)
  hud/TrainerHud              HUD
  command/TrainerCommand      /warpdmg
  model/, util/
tools/                        testes da lógica pura
```

## Solução de problemas

* **Falha ao resolver `ForgeGradle:2.1-SNAPSHOT`** (provável ponto mais frágil, pois depende de repositórios legados): veja o log do passo "Compilar". Será preciso apontar `buildscript.repositories` para uma fonte que ainda hospede o ForgeGradle 2.1 e o Forge 1.8.9 userdev.
* **Erro de compilação de símbolo/método:** os nomes seguem MCP `stable_22`, mas não foram verificados contra o Forge. Envie o log para ajuste. Pontos de maior risco: `Property.set(...)` em `ConfigManager`/`PersistenceManager`, e `RenderGameOverlayEvent.type` em `TrainerHud`.
* **"Teste precisa de condições que permitam receber dano":** use Sobrevivência/Aventura, sem invulnerabilidade.
