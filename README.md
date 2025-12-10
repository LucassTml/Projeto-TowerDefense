# Tower Defense em Java

Este é um jogo completo de Tower Defense desenvolvido em Java com a biblioteca Swing para a interface gráfica. O objetivo do jogo é impedir que ondas de inimigos cheguem até a sua base e a destruam, construindo e melhorando torres estratégicas.

## 🎮 Funcionalidades Principais

### Sistema de Jogo
- **Mapa em Grid**: Mapa baseado em grid com geração aleatória de caminhos
- **Menu Inicial**: Escolha do tamanho do mapa (8x8, 10x10, 12x12) e complexidade do caminho
- **Sistema de Ondas**: Ondas progressivas com 3 tipos diferentes de inimigos
- **Base com Vida**: Base com 100 HP - jogo termina quando chega a zero
- **Economia**: Sistema de dinheiro para construir e melhorar torres

### Sistema de Torres
- **3 Tipos de Torres**:
  - **Torre Básica** ($50): Dano médio, alcance médio, ganha efeito de queimadura no nível 2+
  - **Torre Sniper** ($75): Alto dano, longo alcance, taxa de disparo mais lenta
  - **Torre de Gelo** ($60): Dano baixo, aplica efeito de lentidão nos inimigos
- **Sistema de Upgrades**: Cada torre pode ser melhorada até nível 3
- **Venda de Torres**: Venda torres por 70% do investimento total

### Sistema de Inimigos
- **3 Tipos de Inimigos**:
  - **Básico** (Vermelho): Velocidade e vida médias, sem resistências especiais
  - **Rápido** (Azul): Muito rápido, menos vida, resistente a lentidão, fraco a queimadura
  - **Pesado** (Verde): Lento, muita vida, fraco a lentidão, resistente a queimadura

### Sistema de Efeitos de Status
- **SLOW**: Reduz velocidade do inimigo em 50%
- **BURN**: Causa dano contínuo ao longo do tempo
- **FREEZE**: Reduz velocidade drasticamente (80% mais lento)
- Cada tipo de inimigo tem resistências diferentes aos efeitos

### Interface
- **HUD Completo**: Mostra onda atual, vida da base, dinheiro do jogador
- **Barra de Vida**: Visualização gráfica da vida da base
- **Seleção de Torres**: Clique em torres para ver alcance e fazer upgrade/venda
- **Controle de Velocidade**: Botão para acelerar o jogo 3x ou voltar ao normal

## 🏗️ Arquitetura do Projeto

### Estrutura de Classes

```
src/
├── GameGUI.java          # Classe principal - gerencia interface e gameplay
├── MenuInicial.java      # Menu de configuração inicial
├── GameConfig.java       # Armazena configurações do jogo
├── MapGenerator.java     # Gera caminhos aleatórios
│
├── Tower.java            # Classe abstrata base para torres
├── BasicTower.java       # Torre básica com dano médio
├── SniperTower.java      # Torre de longo alcance
├── IceTower.java         # Torre que aplica lentidão
│
├── Enemy.java            # Classe base para inimigos
├── FastEnemy.java        # Inimigo rápido
├── HeavyEnemy.java       # Inimigo pesado
├── EnemyStatus.java      # Gerencia efeitos de status
├── StatusEffect.java     # Enum de tipos de efeitos
│
├── Projectile.java       # Projéteis disparados pelas torres
├── WaveManager.java      # Gerencia ondas de inimigos
├── Map.java              # Mapa do jogo
├── Tile.java             # Tile individual do mapa
├── Base.java             # Base do jogador
└── Game.java             # Versão console (legado)
```

### Padrões de Design Utilizados
- **Herança**: Torres e inimigos herdam de classes base
- **Polimorfismo**: Tratamento uniforme de diferentes tipos
- **Template Method**: Algoritmo de ataque definido na classe base
- **Factory Pattern**: Criação de inimigos baseada na onda atual

## 🚀 Como Executar

### Pré-requisitos
- Java JDK 8 ou superior instalado

### Compilação
```bash
javac src/*.java
```

### Execução
```bash
java src.GameGUI
```

O menu inicial aparecerá primeiro, permitindo escolher:
- **Tamanho do Mapa**: Pequeno (8x8), Médio (10x10), Grande (12x12)
- **Complexidade do Caminho**: Simples, Médio, Complexo

Após escolher, clique em "INICIAR JOGO" para começar.

## 🎯 Como Jogar

1. **Construir Torres**: Selecione um tipo de torre e clique em um tile verde do mapa
2. **Iniciar Ondas**: Clique em "Iniciar Onda" quando estiver pronto
3. **Melhorar Torres**: Clique em uma torre e use o botão "Upgrade"
4. **Vender Torres**: Clique em uma torre e use o botão "Vender" se precisar reorganizar
5. **Acelerar**: Use o botão "Velocidade" para acelerar o jogo 3x

### Dicas Estratégicas
- Use **Torre de Gelo** contra inimigos pesados (eles são fracos a lentidão)
- Use **Torre Básica** (nível 2+) contra inimigos rápidos (eles são fracos a queimadura)
- **Torre Sniper** é melhor para dano puro e longo alcance
- As primeiras 5 ondas têm progressão clara - use para aprender os tipos de inimigos

## 📊 Progressão das Ondas

- **Onda 1**: 5 inimigos básicos
- **Onda 2**: 8 inimigos básicos (mais vida)
- **Onda 3**: 10 inimigos (7 básicos + 3 rápidos)
- **Onda 4**: 12 inimigos (6 básicos + 6 rápidos)
- **Onda 5**: 15 inimigos (8 básicos + 5 rápidos + 2 pesados)
- **Ondas 6+**: Dificuldade aumenta progressivamente

## 📚 Documentação Técnica

Para documentação técnica completa incluindo diagramas UML e justificativas de design, consulte `DOCUMENTACAO.md`.

## 🛠️ Tecnologias Utilizadas

- **Java**: Linguagem de programação
- **Java Swing**: Biblioteca para interface gráfica
- **AWT**: Para gráficos 2D e eventos

## 📝 Notas de Desenvolvimento

Este projeto foi desenvolvido como exercício de programação orientada a objetos, demonstrando:
- Herança e polimorfismo
- Encapsulamento e abstração
- Design de arquitetura de jogos
- Balanceamento de gameplay
- Interface gráfica com Swing

## 🔮 Possíveis Melhorias Futuras

- Sistema de salvamento/carregamento
- Mais tipos de torres e inimigos
- Animações e efeitos visuais
- Sistema de sons e música
- Diferentes modos de jogo (Endless, Time Attack)
- Sistema de achievements
- Melhorias visuais (sprites, partículas)
