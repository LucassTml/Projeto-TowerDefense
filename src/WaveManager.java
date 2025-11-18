// WaveManager.java
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random; // Importar a classe Random

public class WaveManager {
    private int currentWave = 0;
    private List<Enemy> activeEnemies = new ArrayList<>();
    private List<Point> path;

    // Fila para inimigos que ainda nao nasceram
    private List<Enemy> spawnQueue = new ArrayList<>();
    private double spawnTimer = 0;
    private final double spawnInterval = 60; // 60 ticks = 1 segundo (se o timer do GameGUI for 16ms/tick)
    private Random random = new Random(); // Instancia Random para escolher tipos de inimigos

    public WaveManager(List<Point> path) {
        this.path = path;
    }

    public void startNextWave() {
        currentWave++;
        spawnQueue.clear(); // Limpa a fila de spawn para a nova onda

        int numEnemiesThisWave = 5 + currentWave * 2; // Exemplo: Aumenta o número de inimigos a cada onda

        for (int i = 0; i < numEnemiesThisWave; i++) {
            // Lógica para spawnar diferentes tipos de inimigos baseada na onda atual
            // Isso permite que o jogo se torne progressivamente mais desafiador

            if (currentWave < 2) { // Ondas 1 e 2: Apenas inimigos básicos
                spawnQueue.add(new Enemy(path, 1.25, 10, 10, 5));
            } else if (currentWave < 4) { // Ondas 3 e 4: Inimigos básicos e rápidos
                double typeChance = random.nextDouble(); // Gera um número entre 0.0 e 1.0
                if (typeChance < 0.6) { // 60% de chance de ser um inimigo básico
                    spawnQueue.add(new Enemy(path, 1.25, 10, 10, 5));
                } else { // 40% de chance de ser um inimigo rápido
                    spawnQueue.add(new FastEnemy(path));
                }
            } else { // Ondas 5 em diante: Mistura de básicos, rápidos e pesados
                double typeChance = random.nextDouble();
                if (typeChance < 0.5) { // 50% de chance de ser um inimigo básico
                    spawnQueue.add(new Enemy(path, 1.25, 10, 10, 5));
                } else if (typeChance < 0.8) { // 30% de chance de ser um inimigo rápido (50% + 30% = 80%)
                    spawnQueue.add(new FastEnemy(path));
                } else { // 20% de chance de ser um inimigo pesado (80% + 20% = 100%)
                    spawnQueue.add(new HeavyEnemy(path));
                }
            }
        }
    }

    public int update() {
        int moneyEarned = 0;

        // Lógica de Spawn de inimigos da fila
        spawnTimer++;
        if (spawnTimer >= spawnInterval && !spawnQueue.isEmpty()) {
            activeEnemies.add(spawnQueue.remove(0)); // Move o primeiro da fila para a lista de ativos
            spawnTimer = 0; // Reinicia o temporizador
        }

        // Atualiza todos os inimigos ativos no jogo
        // Usa uma lista temporária para evitar ConcurrentModificationException ao remover inimigos
        List<Enemy> enemiesToRemove = new ArrayList<>();
        for (Enemy e : activeEnemies) {
            e.update(); // Move o inimigo
            if (!e.isAlive()) {
                moneyEarned += e.getKillReward(); // Adiciona recompensa se o inimigo morreu
                enemiesToRemove.add(e);
            }
        }
        activeEnemies.removeAll(enemiesToRemove); // Remove os inimigos que morreram ou chegaram ao fim

        return moneyEarned; // Retorna o dinheiro ganho nesta atualização
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