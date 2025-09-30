import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class GameGUI extends JPanel {
    private Map map;
    private Base base;
    private WaveManager waveManager;
    private java.util.List<Enemy> enemiesToRemove = new ArrayList<>();

    public GameGUI() {
        map = new Map(10, 10);
        base = new Base(100);

        int[][] pathCoords = {{0,0},{1,0},{2,0},{3,0},{4,0}};
        map.setPath(pathCoords);

        java.util.List<Point> path = new ArrayList<>();
        for (int[] p : pathCoords) {
            path.add(new Point(p[0] * 50, p[1] * 50)); // cada tile = 50px
        }

        waveManager = new WaveManager(path);

        Timer timer = new Timer(50, e -> gameLoop());
        timer.start();

        waveManager.startNextWave();
    }

    private void gameLoop() {
        waveManager.getEnemies().removeAll(enemiesToRemove);
        enemiesToRemove.clear();

        waveManager.update();
        for (Enemy enemy : new ArrayList<>(waveManager.getEnemies())) {
            if (enemy.reachedEnd()) {
                base.receberDano(enemy.getDamage());
                enemiesToRemove.add(enemy);
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g; // Usar Graphics2D para melhor controle

        // desenha grid
        for (int i = 0; i < map.getRows(); i++) {
            for (int j = 0; j < map.getCols(); j++) {
                Tile t = map.getTile(i, j);
                if (t.isPath()) {
                    g.setColor(Color.LIGHT_GRAY);
                } else {
                    g.setColor(Color.GREEN);
                }
                g.fillRect(i * 50, j * 50, 50, 50);
                g.setColor(Color.BLACK);
                g.drawRect(i * 50, j * 50, 50, 50);
            }
        }

        // desenha inimigos
        for (Enemy e : waveManager.getEnemies()) {
            int ex = (int)e.getX();
            int ey = (int)e.getY();
            int size = 40;

            g.setColor(Color.RED);
            g.fillOval(ex, ey, size, size);

            // contorno
            g.setColor(Color.BLACK);
            g.drawOval(ex, ey, size, size);
        }

        // HUD
        Font hudFont = new Font("Verdana", Font.BOLD, 16);
        g.setFont(hudFont);
        g.setColor(Color.BLACK);

        String waveText = "Onda: " + waveManager.getCurrentWave();
        String healthText = "Base: " + base.getVidaAtual() + "/" + base.getVidaMaxima();
        g.drawString(waveText, 10, 390);
        g.drawString(healthText, 10, 420);

        // Desenha a barra de vida da base
        int barWidth = 150;
        int barHeight = 20;
        int barX = 10;
        int barY = 430;

        // (vida atual) verde
        double healthPercentage = (double) base.getVidaAtual() / base.getVidaMaxima();
        int greenWidth = (int) (barWidth * healthPercentage);

        // Desenha o fundo da barra (vermelho escuro)
        g.setColor(new Color(100, 0, 0));
        g.fillRect(barX, barY, barWidth, barHeight);

        // frente da barra (verde)
        g.setColor(Color.GREEN);
        g.fillRect(barX, barY, greenWidth, barHeight);

        // borda preta
        g.setColor(Color.BLACK);
        g.drawRect(barX, barY, barWidth, barHeight);

    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Tower Defense Projeto Alpha");
        GameGUI gamePanel = new GameGUI();
        frame.add(gamePanel);
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}