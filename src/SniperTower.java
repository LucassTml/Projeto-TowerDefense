import java.awt.Graphics2D;
import java.awt.Color;

public class SniperTower extends Tower {

    public static final int COST = 75; // Custo em dinheiro

    public SniperTower(int x, int y) {
        // super(x, y, range, fireRate)
        super(x, y, 250.0, 100); // Alcance de 250px, recarga de 100 ticks
    }

    @Override
    protected Projectile createProjectile() {
        // new Projectile(startX, startY, target, damage, speed)
        return new Projectile(x, y, target, 25, 8.0); // Dano alto, projétil rápido
    }

    @Override
    public void draw(Graphics2D g2d) {
        // Desenho da SniperTower
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(x - 12, y - 12, 24, 24); // Quadrado
        g2d.setColor(Color.RED);
        g2d.fillRect(x - 2, y - 18, 4, 18); // "Cano"
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x - 12, y - 12, 24, 24);
    }
}