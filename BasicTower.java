/**
 * Torre básica - a mais simples e barata
 * Bom dano, alcance médio, e ganha efeito de queimadura quando melhora
 */
import java.awt.Graphics2D;
import java.awt.Color;

public class BasicTower extends Tower {
    // Custo de construção
    public static final int COST = 50;

    public BasicTower(int x, int y) {
        // Alcance: 150px, Taxa de disparo: 60 ticks (bem lento no início)
        super(x, y, 150.0, 60);
        this.upgradeCost = 40; // Primeiro upgrade custa 40
    }

    /**
     * Cria o projétil que esta torre dispara
     * Dano aumenta com o nível, e no nível 2+ aplica queimadura
     */
    @Override
    protected Projectile createProjectile() {
        // Dano base: 10, aumenta 5 por nível
        int projectileDamage = 10 + (level - 1) * 5;
        
        // A partir do nível 2, os projéteis queimam os inimigos
        // Isso ajuda contra inimigos rápidos que passam rápido demais
        if (level >= 2) {
            int burnDuration = 30 + (level - 2) * 15; // Duração aumenta com nível
            return new Projectile(x, y, target, projectileDamage, 5.0, StatusEffect.BURN, burnDuration);
        }
        
        // Nível 1: projétil normal sem efeitos
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

        // CHAMADA CRITICA: Desenha o nivel e outras coisas da classe pai
        super.draw(g2d);
    }

    /**
     * Melhora os atributos da torre quando faz upgrade
     * Nível 2: mais alcance e atira mais rápido
     * Nível 3: ainda mais alcance e ainda mais rápido
     */
    @Override
    public void upgrade() {
        super.upgrade(); // Chama o método da classe pai (aumenta o level)
        
        if (level == 2) {
            this.range += 20;      // +20px de alcance
            this.fireRate -= 10;   // Atira 10 ticks mais rápido
        } else if (level == 3) {
            this.range += 30;      // +30px de alcance (total +50)
            this.fireRate -= 15;   // Atira 15 ticks mais rápido (total -25)
        }
    }
    
    @Override
    public int getBaseCost() {
        return COST;
    }
    
    @Override
    protected int calculateTotalUpgradeCost() {
        int total = 0;
        // BasicTower: primeiro upgrade custa 40, segundo custa 40 * 1.5 = 60
        if (level >= 2) {
            total += 40;
        }
        if (level >= 3) {
            total += 60; // 40 * 1.5
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