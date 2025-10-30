import java.awt.Graphics2D;
import java.awt.Color;

public class BasicTower extends Tower {

    public static final int COST = 25; // Custo em dinheiro

    public BasicTower(int x, int y) {
        // super(x, y, range, fireRate)
        super(x, y, 100.0, 30); // Alcance de 100px, recarga de 30 ticks
    }

    @Override
    protected Projectile createProjectile() {
        // new Projectile(startX, startY, target, damage, speed)
        return new Projectile(x, y, target, 5, 4.0);
    }

    @Override
    public void draw(Graphics2D g2d) {
        // Desenho da BasicTower (sobrescreve o padrão)
        g2d.setColor(Color.GRAY);
        g2d.fillRect(x - 15, y - 15, 30, 30); // Quadrado cinza
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x - 15, y - 15, 30, 30);
    }
}