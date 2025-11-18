// SniperTower.java (verifique se está assim)
import java.awt.Graphics2D;
import java.awt.Color;

public class SniperTower extends Tower {
    public static final int COST = 75;

    public SniperTower(int x, int y) {
        super(x, y, 250.0, 100);
        this.upgradeCost = 70;
    }

    @Override
    protected Projectile createProjectile() {
        int projectileDamage = 25 + (level - 1) * 15;
        return new Projectile(x, y, target, projectileDamage, 8.0);
    }

    @Override
    public void draw(Graphics2D g2d) {
        // Desenho específico da SniperTower
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(x - 12, y - 12, 24, 24);
        g2d.setColor(Color.RED);
        g2d.fillRect(x - 2, y - 18, 4, 18);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x - 12, y - 12, 24, 24);

        // CHAMADA CRÍTICA: Desenha o nível e outras coisas da classe pai
        super.draw(g2d);
    }

    @Override
    public void upgrade() {
        super.upgrade();
        if (level == 2) {
            this.range += 50;
        } else if (level == 3) {
            this.range += 75;
            this.fireRate -= 5;
        }
    }
}