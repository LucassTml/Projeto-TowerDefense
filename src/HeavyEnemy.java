// HeavyEnemy.java
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Point;
import java.util.List;

public class HeavyEnemy extends Enemy { // Extende Enemy

    // Definições de atributos para HeavyEnemy
    private static final double SPEED = 0.75; // Mais lento
    private static final int INITIAL_HEALTH = 60; // Mais vida (ajustei para 60 para ser mais distinto do padrão 40)
    private static final int DAMAGE = 20; // Causa mais dano
    private static final int KILL_REWARD = 15; // Recompensa maior

    public HeavyEnemy(List<Point> path) {
        super(path, SPEED, INITIAL_HEALTH, DAMAGE, KILL_REWARD); // Chama o construtor da classe pai
    }

    @Override
    public void draw(Graphics2D g2d) {
        int enemySize = 45; // Um pouco maior
        int drawX = (int)getX() - enemySize / 2;
        int drawY = (int)getY() - enemySize / 2;

        g2d.setColor(Color.GREEN);
        g2d.fillRect(drawX, drawY, enemySize, enemySize); // Quadrado verde
        g2d.setColor(Color.BLACK);
        g2d.drawRect(drawX, drawY, enemySize, enemySize);

        // Desenha a barra de vida acima do HeavyEnemy
        drawHealthBar(g2d, (int)getX(), (int)getY(), enemySize);
    }

    private void drawHealthBar(Graphics2D g2d, int centerX, int centerY, int enemySize) {
        // Usa getVidaMaxima() e getVidaAtual() da classe pai
        double healthPercentage = (double) getVidaAtual() / getVidaMaxima();

        int barWidth = enemySize;
        int barHeight = 5;
        // Posição da barra de vida (acima e centralizada no inimigo)
        int barX = centerX - barWidth / 2;
        int barY = centerY - enemySize / 2 - barHeight - 2; // Acima do inimigo, com espaço

        g2d.setColor(Color.RED);
        g2d.fillRect(barX, barY, barWidth, barHeight);
        g2d.setColor(Color.GREEN);
        g2d.fillRect(barX, barY, (int)(barWidth * healthPercentage), barHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(barX, barY, barWidth, barHeight);
    }
}
