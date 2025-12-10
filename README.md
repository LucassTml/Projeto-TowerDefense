# Tower Defense em Java

Este é um protótipo de um jogo de Tower Defense desenvolvido em Java com a biblioteca Swing para a interface gráfica. O objetivo do jogo é impedir que ondas de inimigos cheguem até a sua base e a destruam.
O projeto foi desenvolvido com foco em aprender e aplicar conceitos de orientação a objetos, manipulação de interface gráfica e lógica de jogos.

## Funcionalidades

- **Mapa em Grid**: O jogo utiliza um mapa baseado em grid, onde é possível definir um caminho específico para os inimigos.
- **Inimigos com Movimentação**: Os inimigos seguem um caminho pré-definido, movendo-se do ponto de início ao fim.
- **Sistema de Ondas (Waves)**: Os inimigos são gerados em ondas, com um intervalo de tempo entre o nascimento de cada um.
- **Base com Vida**: Existe uma base que possui pontos de vida. Se um inimigo alcança o final do caminho, a base sofre dano. O jogo termina quando a vida da base chega a zero.
- **Interface Gráfica (HUD)**: Uma interface simples exibe informações essenciais, como a vida atual da base (com uma barra de vida) e o número da onda atual.
- **Duas Versões**: O projeto contém uma versão com interface gráfica (`GameGUI.java`) e uma versão que roda no console (`Game.java`).

## Como Executar

Para jogar, você precisa ter o Java JDK instalado em sua máquina.

1.  **Compile os arquivos `.java`:**
    ```bash
    javac src/*.java
    ```

2.  **Execute a versão com interface gráfica:**
    ```bash
    java src.GameGUI
    ```
    *Ou, se preferir, execute a versão baseada em texto no console:*
    ```bash
    java src.Game
    ```
