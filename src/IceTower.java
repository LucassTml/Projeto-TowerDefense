// IceTower.java
import java.awt.Graphics2D;
import java.awt.Color;

public class IceTower extends Tower {
    public static final int COST = 60;

    public IceTower(int x, int y) {
        super(x, y, 180.0, 80);
        this.upgradeCost = 50;
    }

    @Override
    protected Projectile createProjectile() {
        int projectileDamage = 8 + (level - 1) * 4;
        int slowDuration = 60 + (level - 1) * 20; // Duração do slow em ticks
        return new Projectile(x, y, target, projectileDamage, 4.0, StatusEffect.SLOW, slowDuration);
    }

    @Override
    public void draw(Graphics2D g2d) {
        // Desenho específico da IceTower (azul claro)
        g2d.setColor(new Color(150, 200, 255));
        g2d.fillOval(x - 15, y - 15, 30, 30);
        g2d.setColor(new Color(100, 150, 255));
        g2d.drawOval(x - 15, y - 15, 30, 30);
        g2d.setColor(Color.WHITE);
        g2d.fillOval(x - 8, y - 8, 16, 16);

        // CHAMADA CRITICA: Desenha o nivel e outras coisas da classe pai
        super.draw(g2d);
    }

    @Override
    public void upgrade() {
        super.upgrade();
        if (level == 2) {
            this.range += 30;
            this.fireRate -= 15;
        } else if (level == 3) {
            this.range += 40;
            this.fireRate -= 20;
        }
    }
    
    @Override
    public int getBaseCost() {
        return COST;
    }
    
    @Override
    protected int calculateTotalUpgradeCost() {
        int total = 0;
        if (level >= 2) {
            total += 50;
        }
        if (level >= 3) {
            total += 75; // 50 * 1.5
        }
        return total;
    }
    
    @Override
    public int getSellValue() {
        int baseValue = COST;
        int totalUpgradeCost = calculateTotalUpgradeCost();
        return (int)((baseValue + totalUpgradeCost) * 0.7);
    }
}

