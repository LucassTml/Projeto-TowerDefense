// FastEnemy.java
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Point;
import java.util.List;

public class FastEnemy extends Enemy { // Extende Enemy

    // Definições de atributos para FastEnemy
    private static final double SPEED = 1.5; // Mais rápido
    private static final int INITIAL_HEALTH = 20; // Menos vida
    private static final int DAMAGE = 5; // Menos dano
    private static final int KILL_REWARD = 10; // Recompensa padrão

    public FastEnemy(List<Point> path) {
        super(path, SPEED, INITIAL_HEALTH, DAMAGE, KILL_REWARD); // Chama o construtor da classe pai
    }

    @Override
    public void draw(Graphics2D g2d) {
        int enemySize = 30; // Um pouco menor
        int drawX = (int)getX() - enemySize / 2;
        int drawY = (int)getY() - enemySize / 2;

        g2d.setColor(Color.BLUE);
        g2d.fillOval(drawX, drawY, enemySize, enemySize);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(drawX, drawY, enemySize, enemySize);
        // Não desenha barra de vida para inimigos rápidos
    }
}
