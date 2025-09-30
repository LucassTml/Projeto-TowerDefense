import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class WaveManager {
    private int currentWave = 0;
    private List<Enemy> activeEnemies = new ArrayList<>();
    private List<Point> path;

    // Fila para inimigos que ainda nao nasceram
    private List<Enemy> spawnQueue = new ArrayList<>();
    private double spawnTimer = 0;
    private final double spawnInterval = 60;

    public WaveManager(List<Point> path) {
        this.path = path;
    }

    public void startNextWave() {
        currentWave++;
        spawnQueue.clear(); // Limpa a fila

        // Adiciona os inimigos na fila de espera, em vez de diretamente no jogo
        for (int i = 0; i < 5 + currentWave; i++) {
            Enemy e = new Enemy(path, 1.25, 10, 10);
            spawnQueue.add(e);
        }
    }

    public void update() {
        // Spawn
        spawnTimer++;
        if (spawnTimer >= spawnInterval && !spawnQueue.isEmpty()) {
            activeEnemies.add(spawnQueue.remove(0)); // Move o primeiro da fila para o jogo
            spawnTimer = 0; // Reinicia o temporizador
        }

        // Atualiza os inimigos ativos no jogo
        for (Enemy e : new ArrayList<>(activeEnemies)) {
            e.update();
            if (e.isDead()) {   // se ta morto
                activeEnemies.remove(e);
            }
        }
    }

    // gets
    public List<Enemy> getEnemies() {
        return activeEnemies;
    }

    public int getCurrentWave() {
        return currentWave;
    }
}