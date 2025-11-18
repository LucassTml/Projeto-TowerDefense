// BasicTower.java (verifique se está assim)
import java.awt.Graphics2D;
import java.awt.Color;

public class BasicTower extends Tower {
    public static final int COST = 50;

    public BasicTower(int x, int y) {
        super(x, y, 150.0, 60);
        this.upgradeCost = 40;
    }

    @Override
    protected Projectile createProjectile() {
        int projectileDamage = 10 + (level - 1) * 5;
        return new Projectile(x, y, target, projectileDamage, 5.0);
    }

    @Override
    public void draw(Graphics2D g2d) {
        // Desenho específico da BasicTower
        g2d.setColor(Color.GRAY);
        g2d.fillOval(x - 15, y - 15, 30, 30);
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawOval(x - 15, y - 15, 30, 30);
        g2d.setColor(Color.BLACK);
        g2d.fillRect(x - 5, y - 20, 10, 5);

        // CHAMADA CRÍTICA: Desenha o nível e outras coisas da classe pai
        super.draw(g2d);
    }

    @Override
    public void upgrade() {
        super.upgrade();
        if (level == 2) {
            this.range += 20;
            this.fireRate -= 10;
        } else if (level == 3) {
            this.range += 30;
            this.fireRate -= 15;
        }
    }
}