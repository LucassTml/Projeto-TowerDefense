// HeavyEnemy.java
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Point;
import java.util.List;

public class HeavyEnemy extends Enemy { // Extende Enemy

    // Definições de atributos para HeavyEnemy
    private static final double SPEED = 0.7; // Mais lento
    private static final int INITIAL_HEALTH = 80; // Mais vida
    private static final int DAMAGE = 25; // Causa mais dano
    private static final int KILL_REWARD = 20; // Recompensa maior
    
    // Resistências: fraco a SLOW (0.0), resistente a BURN (0.8), médio a FREEZE (0.4)
    private static final double SLOW_RESIST = 0.0;
    private static final double BURN_RESIST = 0.8;
    private static final double FREEZE_RESIST = 0.4;

    public HeavyEnemy(List<Point> path) {
        super(path, SPEED, INITIAL_HEALTH, DAMAGE, KILL_REWARD,
              SLOW_RESIST, BURN_RESIST, FREEZE_RESIST);
    }

    @Override
    public void draw(Graphics2D g2d) {
        int enemySize = 48; // Um pouco maior
        int drawX = (int)getX() - enemySize / 2;
        int drawY = (int)getY() - enemySize / 2;

        g2d.setColor(new Color(0, 150, 0)); // Verde escuro
        g2d.fillRect(drawX, drawY, enemySize, enemySize); // Quadrado verde
        g2d.setColor(Color.BLACK);
        g2d.drawRect(drawX, drawY, enemySize, enemySize);

        // Desenha a barra de vida acima do HeavyEnemy
        drawHealthBar(g2d, (int)getX(), (int)getY(), enemySize);
        
        // Desenha indicadores de status
        super.draw(g2d);
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
