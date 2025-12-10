// FastEnemy.java
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Point;
import java.util.List;

public class FastEnemy extends Enemy { // Extende Enemy

    // Definições de atributos para FastEnemy
    private static final double SPEED = 1.8; // Mais rápido
    private static final int INITIAL_HEALTH = 25; // Menos vida
    private static final int DAMAGE = 5; // Menos dano
    private static final int KILL_REWARD = 12; // Recompensa padrão
    
    // Resistências: resistente a SLOW (0.7), fraco a BURN (0.0), médio a FREEZE (0.3)
    private static final double SLOW_RESIST = 0.7;
    private static final double BURN_RESIST = 0.0;
    private static final double FREEZE_RESIST = 0.3;

    public FastEnemy(List<Point> path) {
        super(path, SPEED, INITIAL_HEALTH, DAMAGE, KILL_REWARD, 
              SLOW_RESIST, BURN_RESIST, FREEZE_RESIST);
    }

    @Override
    public void draw(Graphics2D g2d) {
        int enemySize = 32; // Um pouco menor
        int drawX = (int)getX() - enemySize / 2;
        int drawY = (int)getY() - enemySize / 2;

        g2d.setColor(new Color(50, 100, 255)); // Azul brilhante
        g2d.fillOval(drawX, drawY, enemySize, enemySize);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(drawX, drawY, enemySize, enemySize);
        
        // Desenha indicadores de status
        super.draw(g2d);
    }
}
