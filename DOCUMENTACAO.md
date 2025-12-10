# Documentação Técnica - Tower Defense Game

## 1. Diagrama de Classes (UML)

```
┌─────────────────────────────────────────────────────────────────┐
│                         GAME ARCHITECTURE                        │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────┐
│     MenuInicial     │
│  (JFrame)           │
├─────────────────────┤
│ - mapSizeCombo      │
│ - pathComplexityCombo│
│ - startButton       │
│ + startGame()       │
└──────────┬──────────┘
           │ cria
           ▼
┌─────────────────────┐      ┌──────────────────┐
│     GameConfig      │      │  MapGenerator    │
├─────────────────────┤      ├──────────────────┤
│ + rows              │      │ - random         │
│ + cols              │      │ + generatePath() │
│ + pathComplexity    │      └──────────────────┘
└──────────┬──────────┘
           │ usado por
           ▼
┌─────────────────────────────────────────────────────────────────┐
│                        GameGUI (JPanel)                         │
├─────────────────────────────────────────────────────────────────┤
│ - map: Map                                                      │
│ - base: Base                                                    │
│ - waveManager: WaveManager                                     │
│ - towers: List<Tower>                                          │
│ - projectiles: List<Projectile>                                 │
│ - playerMoney: int                                             │
│ - gameSpeed: int                                               │
│ - gameTimer: Timer                                             │
│ + gameLoop()                                                   │
│ + placeTower()                                                 │
│ + sellTower()                                                  │
│ + startWave()                                                  │
└──────────┬──────────────────────────────────────────────────────┘
           │ gerencia
           ├──────────────────┐
           │                  │
           ▼                  ▼
┌──────────────────┐  ┌──────────────────┐
│      Map         │  │      Base        │
├──────────────────┤  ├──────────────────┤
│ - grid: Tile[][] │  │ - vidaAtual: int │
│ + setPath()      │  │ - vidaMaxima: int│
│ + getTile()      │  │ + receberDano() │
└──────────┬───────┘  │ + estaDestruida()│
           │          └──────────────────┘
           │ contém
           ▼
┌──────────────────┐
│      Tile        │
├──────────────────┤
│ - isPath: boolean│
│ - canBuild: bool │
│ + canBuild()     │
└──────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      TOWER SYSTEM                                │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────┐
│        Tower (abstract)          │
├──────────────────────────────────┤
│ # x, y: int                      │
│ # range: double                  │
│ # fireRate: int                  │
│ # fireCooldown: int              │
│ # target: Enemy                  │
│ # level: int                     │
│ # upgradeCost: int               │
│ + findTarget()                   │
│ + update(): Projectile           │
│ + upgrade()                      │
│ + canUpgrade(): boolean          │
│ + getSellValue(): int            │
│ # createProjectile(): Projectile │ (abstract)
│ + getBaseCost(): int             │ (abstract)
└──────────┬───────────────────────┘
           │ extends
           ├──────────────┬──────────────┐
           │              │              │
           ▼              ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ BasicTower   │ │ SniperTower  │ │  IceTower    │
├──────────────┤ ├──────────────┤ ├──────────────┤
│ + COST = 50  │ │ + COST = 75  │ │ + COST = 60  │
│ + upgrade()  │ │ + upgrade()  │ │ + upgrade()  │
│ # createProj │ │ # createProj │ │ # createProj │
└──────────────┘ └──────────────┘ └──────────────┘
       │                 │                 │
       │                 │                 │
       └─────────────────┴─────────────────┘
                         │ cria
                         ▼
┌──────────────────────────────────┐
│        Projectile                │
├──────────────────────────────────┤
│ - x, y: double                   │
│ - speed: double                  │
│ - damage: int                    │
│ - target: Enemy                  │
│ - statusEffect: StatusEffect     │
│ - statusDuration: int            │
│ + update()                       │
│ + draw()                         │
└──────────┬───────────────────────┘
           │ atinge
           ▼
┌─────────────────────────────────────────────────────────────────┐
│                      ENEMY SYSTEM                                │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────┐
│          Enemy                   │
├──────────────────────────────────┤
│ # x, y: double                   │
│ # baseSpeed: double              │
│ # speed: double                   │
│ # damage: int                    │
│ # vidaAtual: int                 │
│ # vidaMaxima: int                │
│ # currentWaypoint: int           │
│ # path: List<Point>              │
│ # status: EnemyStatus           │
│ # slowResistance: double         │
│ # burnResistance: double        │
│ # freezeResistance: double      │
│ + update()                       │
│ + takeDamage()                   │
│ + applyStatusEffect()            │
│ + draw()                         │
└──────────┬───────────────────────┘
           │ extends
           ├──────────────┬──────────────┐
           │              │              │
           ▼              ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ FastEnemy    │ │ HeavyEnemy   │ │ Enemy (base) │
├──────────────┤ ├──────────────┤ ├──────────────┤
│ SPEED = 1.8  │ │ SPEED = 0.7  │ │ SPEED = 1.25│
│ HEALTH = 25  │ │ HEALTH = 80  │ │ HEALTH = 15 │
│ SLOW_RES=0.7 │ │ SLOW_RES=0.0 │ │ SLOW_RES=0.0│
│ BURN_RES=0.0 │ │ BURN_RES=0.8 │ │ BURN_RES=0.0│
└──────────────┘ └──────────────┘ └──────────────┘
       │                 │                 │
       │                 │                 │
       └─────────────────┴─────────────────┘
                         │ usa
                         ▼
┌──────────────────────────────────┐
│        EnemyStatus               │
├──────────────────────────────────┤
│ - activeEffects: Map<StatusEffect│
│ - slowMultiplier: double         │
│ - burnDamage: int                │
│ - burnTicks: int                 │
│ + applyEffect()                  │
│ + update()                       │
│ + getSlowMultiplier()            │
│ + getBurnDamage()                │
└──────────┬───────────────────────┘
           │ gerencia
           ▼
┌──────────────────────────────────┐
│      StatusEffect (enum)         │
├──────────────────────────────────┤
│ SLOW                             │
│ BURN                             │
│ FREEZE                           │
└──────────────────────────────────┘

┌──────────────────────────────────┐
│      WaveManager                │
├──────────────────────────────────┤
│ - currentWave: int              │
│ - activeEnemies: List<Enemy>    │
│ - spawnQueue: List<Enemy>       │
│ - spawnTimer: double            │
│ - random: Random                │
│ + startNextWave()               │
│ + update(): int                 │
│ + isWaveComplete(): boolean     │
└──────────────────────────────────┘
```

## 2. Justificativa das Decisões de Design

### 2.1 Arquitetura Geral

**Padrão MVC Simplificado:**
- **Model**: Classes `Tower`, `Enemy`, `Base`, `Map` - representam a lógica do jogo
- **View**: `GameGUI` (JPanel) - responsável por desenhar tudo na tela
- **Controller**: `GameGUI` também atua como controlador, processando eventos do mouse e teclado

**Justificativa:** Para um jogo simples, separar completamente Model/View/Controller seria over-engineering. O `GameGUI` centraliza a lógica de controle e renderização, simplificando a comunicação entre componentes.

### 2.2 Sistema de Torres

**Herança com Classe Abstrata:**
- `Tower` é uma classe abstrata que define a interface comum
- `BasicTower`, `SniperTower`, `IceTower` herdam e implementam comportamentos específicos

**Justificativa:**
- **Reutilização de código**: Todas as torres compartilham lógica de busca de alvo, cooldown, e upgrade
- **Polimorfismo**: Permite tratar todas as torres uniformemente em listas (`List<Tower>`)
- **Extensibilidade**: Fácil adicionar novos tipos de torres sem modificar código existente
- **Encapsulamento**: Cada torre controla seus próprios atributos (alcance, dano, custo)

**Método Abstrato `createProjectile()`:**
- Cada torre decide que tipo de projétil dispara
- Permite que torres diferentes tenham projéteis com diferentes efeitos

**Justificativa:** Padrão Template Method - define o algoritmo geral (buscar alvo, verificar cooldown) mas delega a criação do projétil para subclasses.

### 2.3 Sistema de Upgrades

**Design Escalonado:**
- Nível máximo: 3 (evita torres infinitamente poderosas)
- Custo aumenta 50% a cada upgrade (balanceamento econômico)
- Cada torre tem melhorias específicas por nível

**Justificativa:**
- **Balanceamento**: Limita o poder máximo e cria decisões estratégicas (upgrade vs construir nova torre)
- **Progressão**: Sensação de crescimento sem quebrar o jogo
- **Economia**: Custo crescente força o jogador a gerenciar recursos

**Cálculo de Valor de Venda:**
- Retorna 70% do investimento total (custo base + upgrades)
- Cada torre calcula seu próprio valor baseado nos upgrades feitos

**Justificativa:** Permite reorganização estratégica sem penalizar muito o jogador. 70% é um valor que permite recuperar recursos mas não é tão bom quanto não ter vendido.

### 2.4 Sistema de Status Effects

**Separação de Responsabilidades:**
- `StatusEffect` (enum): Define os tipos de efeitos
- `EnemyStatus`: Gerencia efeitos ativos e suas durações
- `Enemy`: Aplica efeitos na velocidade e vida

**Justificativa:**
- **Single Responsibility**: Cada classe tem uma responsabilidade clara
- **Extensibilidade**: Fácil adicionar novos efeitos (só adicionar no enum)
- **Manutenibilidade**: Lógica de status isolada facilita debug e testes

**Sistema de Resistências:**
- Cada tipo de inimigo tem resistências diferentes a cada efeito
- Cria estratégia: FastEnemy é fraco a BURN mas resistente a SLOW

**Justificativa:** Adiciona profundidade estratégica - jogador precisa escolher torres certas para cada tipo de inimigo.

### 2.5 Sistema de Inimigos

**Herança com Especialização:**
- `Enemy`: Classe base com comportamento padrão
- `FastEnemy`, `HeavyEnemy`: Especializações com atributos diferentes

**Justificativa:**
- **Diversidade**: Diferentes tipos criam desafios variados
- **Balanceamento**: Cada tipo tem pontos fortes e fracos
- **Visual**: Cores diferentes facilitam identificação rápida

**Sistema de Caminho (Path):**
- Inimigos seguem uma lista de pontos pré-definida
- Movimento baseado em waypoints

**Justificativa:** Simples de implementar e permite caminhos complexos. Alternativa seria pathfinding (A*), mas seria overkill para este jogo.

### 2.6 Geração de Mapas Aleatórios

**Algoritmo de Geração:**
- Começa no canto superior esquerdo
- Move aleatoriamente (direita ou baixo) até o destino
- Adiciona curvas extras baseado na complexidade

**Justificativa:**
- **Rejogabilidade**: Cada partida tem mapa diferente
- **Simplicidade**: Algoritmo simples mas efetivo
- **Configurável**: Complexidade permite ajustar dificuldade

**Por que não usar algoritmos mais complexos?**
- Para um Tower Defense simples, caminhos básicos são suficientes
- Algoritmos como maze generation seriam desnecessários
- Foco está na gameplay, não na geração procedural avançada

### 2.7 Sistema de Ondas

**Progressão Escalonada:**
- Primeiras 5 ondas têm progressão bem definida
- Ensina o jogador gradualmente
- Ondas 6+ aumentam dificuldade progressivamente

**Justificativa:**
- **Curva de Aprendizado**: Jogador aprende tipos de inimigos gradualmente
- **Balanceamento**: Evita frustração inicial e tédio depois
- **Escalabilidade**: Sistema permite ondas infinitas com dificuldade crescente

**Spawn com Intervalo:**
- Inimigos spawnam um de cada vez com intervalo fixo
- Cria ritmo controlado

**Justificativa:** Evita spawnar todos de uma vez (muito difícil) ou muito rápido (confuso). Intervalo dá tempo pro jogador reagir.

### 2.8 Interface Gráfica

**Swing com JPanel Customizado:**
- `GameGUI` estende `JPanel` e sobrescreve `paintComponent()`
- Timer controla o loop do jogo

**Justificativa:**
- **Simplicidade**: Swing é nativo do Java, sem dependências externas
- **Controle Total**: `paintComponent()` permite desenhar exatamente como queremos
- **Performance**: Adequada para este tipo de jogo (não precisa de 60 FPS)

**Estados do Jogo:**
- `BUILDING_PHASE`: Jogador pode construir torres
- `WAVE_IN_PROGRESS`: Onda em andamento, não pode construir

**Justificativa:** Cria fases distintas de gameplay - planejamento vs ação. Evita que jogador construa durante combate (que seria confuso).

### 2.9 Menu Inicial

**Separação de Configuração:**
- `MenuInicial`: Interface para escolher configurações
- `GameConfig`: Classe simples que armazena configurações
- `GameGUI`: Recebe configuração e cria jogo

**Justificativa:**
- **Separação de Responsabilidades**: Menu não precisa saber sobre gameplay
- **Reutilização**: Mesma configuração pode ser usada para diferentes modos
- **Testabilidade**: Fácil testar GameGUI com configurações diferentes

### 2.10 Controle de Velocidade

**Alteração de Intervalo do Timer:**
- Velocidade normal: 50ms entre frames
- Velocidade 3x: ~16ms entre frames

**Justificativa:**
- **Simplicidade**: Não precisa modificar lógica do jogo, só acelera tudo
- **Experiência do Usuário**: Permite acelerar partes chatas (construção)
- **Implementação**: Muito simples - só muda delay do Timer

## 3. Padrões de Design Utilizados

### 3.1 Template Method
**Onde:** Classe `Tower`
**Como:** Método `update()` define algoritmo geral, mas delega `createProjectile()` para subclasses
**Por quê:** Permite que todas as torres sigam mesmo padrão mas criem projéteis diferentes

### 3.2 Strategy (implícito)
**Onde:** Diferentes tipos de torres e inimigos
**Como:** Cada tipo tem estratégia diferente (dano vs efeitos vs alcance)
**Por quê:** Permite variar comportamento sem modificar código existente

### 3.3 Observer (implícito)
**Onde:** `GameGUI` observa eventos do mouse
**Como:** `MouseAdapter` notifica quando mouse é clicado
**Por quê:** Desacopla eventos de UI da lógica do jogo

### 3.4 Factory (simples)
**Onde:** `WaveManager.startNextWave()`
**Como:** Cria diferentes tipos de inimigos baseado na onda
**Por quê:** Centraliza lógica de criação e facilita balanceamento

## 4. Decisões de Balanceamento

### 4.1 Economia
- **Dinheiro inicial:** 100 - permite construir 2 torres básicas
- **Recompensas:** 5-20 por kill - suficiente para upgrades mas não excessivo
- **Custos:** Escalonados (50, 60, 75) - cria decisões estratégicas

### 4.2 Dano e Vida
- **Torres básicas:** Dano 10-20 - suficiente mas não OP
- **Inimigos:** Vida 15-80 - progressão gradual
- **Projéteis:** Velocidade 4-8 - rápidos o suficiente para acertar

### 4.3 Alcance e Taxa de Disparo
- **Alcance:** 150-300px - cobre área razoável do mapa
- **Fire Rate:** 60-100 ticks - não muito rápido, não muito lento
- **Upgrades:** Aumentam alcance e reduzem fire rate - melhorias significativas

### 4.4 Status Effects
- **SLOW:** 50% velocidade - perceptível mas não quebra o jogo
- **BURN:** 2 dano/tick - dano contínuo útil mas não OP
- **Duração:** 30-90 ticks - tempo suficiente para ser útil

## 5. Limitações e Melhorias Futuras

### 5.1 Limitações Atuais
- **Pathfinding:** Inimigos seguem caminho fixo (não há pathfinding dinâmico)
- **IA:** Inimigos não têm comportamento inteligente
- **Salvamento:** Não há sistema de save/load
- **Multiplayer:** Apenas single-player
- **Efeitos Visuais:** Limitados (sem animações, partículas)

### 5.2 Melhorias Possíveis
- **Mais tipos de torres:** Torres de área, buffs, debuffs
- **Mais tipos de inimigos:** Voadores, invisíveis, regeneradores
- **Sistema de achievements:** Conquistas e desafios
- **Diferentes modos:** Endless, Time Attack, Survival
- **Melhorias visuais:** Animações, efeitos de partícula, sprites
- **Sons e música:** Audio feedback para ações

## 6. Conclusão

Este projeto demonstra uma arquitetura bem estruturada usando conceitos de OOP:
- **Herança** para reutilização de código
- **Polimorfismo** para flexibilidade
- **Encapsulamento** para organização
- **Abstração** para simplificar complexidade

O código é extensível (fácil adicionar novos tipos) e mantível (responsabilidades claras). As decisões de design priorizam simplicidade sobre complexidade desnecessária, resultando em um código limpo e funcional.

