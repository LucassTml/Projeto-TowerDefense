/**
 * Gerencia as ondas de inimigos
 * Controla quando spawnar, quantos spawnar, e quais tipos
 */
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WaveManager {
    private int currentWave = 0;
    private List<Enemy> activeEnemies = new ArrayList<>(); // Inimigos que estão no mapa agora
    private List<Point> path; // Caminho que os inimigos seguem

    // Fila de inimigos esperando para nascer
    // Spawna um de cada vez pra não ficar muito caótico
    private List<Enemy> spawnQueue = new ArrayList<>();
    
    // Timer para controlar o espaçamento entre spawns
    private double spawnTimer = 0;
    private final double spawnInterval = 60; // Spawna um inimigo a cada 60 ticks
    
    // Random pra escolher tipos de inimigos aleatoriamente
    private Random random = new Random();

    public WaveManager(List<Point> path) {
        this.path = path;
    }

    /**
     * Inicia a próxima onda de inimigos
     * As primeiras 5 ondas têm progressão bem definida pra ensinar o jogador
     * Depois disso, fica mais difícil progressivamente
     */
    public void startNextWave() {
        currentWave++;
        spawnQueue.clear(); // Limpa qualquer inimigo que sobrou da onda anterior

        // Define quantos inimigos vão spawnar nesta onda
        int numEnemiesThisWave;
        if (currentWave == 1) {
            numEnemiesThisWave = 5; // Onda 1: 5 inimigos básicos
        } else if (currentWave == 2) {
            numEnemiesThisWave = 8; // Onda 2: 8 inimigos básicos
        } else if (currentWave == 3) {
            numEnemiesThisWave = 10; // Onda 3: 10 inimigos (7 básicos + 3 rápidos)
        } else if (currentWave == 4) {
            numEnemiesThisWave = 12; // Onda 4: 12 inimigos (6 básicos + 6 rápidos)
        } else if (currentWave == 5) {
            numEnemiesThisWave = 15; // Onda 5: 15 inimigos (8 básicos + 5 rápidos + 2 pesados)
        } else {
            // Ondas 6+: Progressão exponencial
            numEnemiesThisWave = 15 + (currentWave - 5) * 3;
        }

        // Cria cada inimigo da onda
        for (int i = 0; i < numEnemiesThisWave; i++) {
            // Onda 1: Só básicos pra não assustar o jogador
            if (currentWave == 1) {
                spawnQueue.add(new Enemy(path, 1.25, 15, 10, 5));
            } 
            // Onda 2: Ainda só básicos, mas com mais vida
            else if (currentWave == 2) {
                spawnQueue.add(new Enemy(path, 1.25, 20, 10, 5));
            } 
            // Onda 3: Introduz inimigos rápidos (70% básicos, 30% rápidos)
            // Assim o jogador aprende a lidar com velocidade
            else if (currentWave == 3) {
                if (random.nextDouble() < 0.7) {
                    spawnQueue.add(new Enemy(path, 1.25, 20, 10, 5));
                } else {
                    spawnQueue.add(new FastEnemy(path));
                }
            } 
            // Onda 4: Mais rápidos aparecem (50/50)
            else if (currentWave == 4) {
                if (random.nextDouble() < 0.5) {
                    spawnQueue.add(new Enemy(path, 1.25, 25, 10, 5));
                } else {
                    spawnQueue.add(new FastEnemy(path));
                }
            } 
            // Onda 5: Introduz inimigos pesados (todos os tipos aparecem)
            else if (currentWave == 5) {
                double typeChance = random.nextDouble();
                if (typeChance < 0.5) {
                    spawnQueue.add(new Enemy(path, 1.25, 25, 10, 5));
                } else if (typeChance < 0.85) {
                    spawnQueue.add(new FastEnemy(path));
                } else {
                    spawnQueue.add(new HeavyEnemy(path));
                }
            } 
            // Ondas 6+: Mistura completa e vida aumenta com a onda
            // Fica progressivamente mais difícil
            else {
                double typeChance = random.nextDouble();
                if (typeChance < 0.4) {
                    // Vida aumenta 2 por onda depois da 5ª
                    spawnQueue.add(new Enemy(path, 1.25, 25 + (currentWave - 5) * 2, 10, 5));
                } else if (typeChance < 0.75) {
                    spawnQueue.add(new FastEnemy(path));
                } else {
                    spawnQueue.add(new HeavyEnemy(path));
                }
            }
        }
    }

    /**
     * Atualiza o gerenciador de ondas a cada frame
     * Spawna inimigos, atualiza os que estão vivos, e remove os mortos
     * @return dinheiro ganho com kills nesta atualização
     */
    public int update() {
        int moneyEarned = 0;

        // Spawna um inimigo da fila se já passou tempo suficiente
        spawnTimer++;
        if (spawnTimer >= spawnInterval && !spawnQueue.isEmpty()) {
            // Pega o primeiro da fila e coloca no jogo
            activeEnemies.add(spawnQueue.remove(0));
            spawnTimer = 0; // Reseta o timer
        }

        // Atualiza todos os inimigos que estão no mapa
        // IMPORTANTE: usa lista temporária pra evitar erro ao remover durante iteração
        // (ConcurrentModificationException - já me deu dor de cabeça isso)
        List<Enemy> enemiesToRemove = new ArrayList<>();
        for (Enemy e : activeEnemies) {
            e.update(); // Move o inimigo pelo caminho
            
            // Se morreu, adiciona recompensa e marca pra remover
            if (!e.isAlive()) {
                moneyEarned += e.getKillReward();
                enemiesToRemove.add(e);
            }
        }
        
        // Remove todos os inimigos que morreram
        activeEnemies.removeAll(enemiesToRemove);

        return moneyEarned;
    }

    // Getters
    public List<Enemy> getEnemies() {
        return activeEnemies;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public boolean isWaveComplete() {
        // A onda está completa se não há inimigos ativos E a fila de spawn está vazia
        return activeEnemies.isEmpty() && spawnQueue.isEmpty();
    }
}