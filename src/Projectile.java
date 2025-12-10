import java.awt.Graphics2D;
import java.awt.Color;

public class Projectile {
    private double x, y;
    private double speed;
    private int damage;
    private Enemy target;
    private boolean active; // Flag para marcar para remoção
    
    // Status effect que este projétil aplica
    private StatusEffect statusEffect;
    private int statusDuration;

    public Projectile(double x, double y, Enemy target, int damage, double speed) {
        this(x, y, target, damage, speed, null, 0);
    }
    
    public Projectile(double x, double y, Enemy target, int damage, double speed, 
                     StatusEffect statusEffect, int statusDuration) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.damage = damage;
        this.speed = speed;
        this.active = true;
        this.statusEffect = statusEffect;
        this.statusDuration = statusDuration;
    }

    public void update() {
        if (!active || target == null || !target.isAlive()) {
            active = false; // Desativa se o alvo morreu
            return;
        }

        // Mira no centro do inimigo
        double targetX = target.getX() + 20;
        double targetY = target.getY() + 20;

        // Lógica de movimento (homing)
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // Verifica se colidiu (ou está muito perto)
        if (distance < speed) {
            hitTarget();
        } else {
            // Move-se em direção ao alvo
            x += (dx / distance) * speed;
            y += (dy / distance) * speed;
        }
    }

    private void hitTarget() {
        target.takeDamage(damage);
        // Aplica status effect se houver
        if (statusEffect != null && statusDuration > 0) {
            target.applyStatusEffect(statusEffect, statusDuration);
        }
        active = false; // Marca para remoção
    }

    public boolean isActive() {
        return active;
    }

    public void draw(Graphics2D g2d) {
        if (!active) return;

        // Cor baseada no status effect
        if (statusEffect == StatusEffect.SLOW || statusEffect == StatusEffect.FREEZE) {
            g2d.setColor(new Color(100, 150, 255)); // Azul para slow/freeze
        } else if (statusEffect == StatusEffect.BURN) {
            g2d.setColor(new Color(255, 100, 0)); // Laranja para burn
        } else {
            g2d.setColor(Color.ORANGE); // Laranja padrão
        }
        g2d.fillOval((int)x - 3, (int)y - 3, 6, 6); // Projétil pequeno
    }
}