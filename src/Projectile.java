import java.awt.Graphics2D;
import java.awt.Color;

public class Projectile {
    private double x, y;
    private double speed;
    private int damage;
    private Enemy target;
    private boolean active; // Flag para marcar para remoção

    public Projectile(double x, double y, Enemy target, int damage, double speed) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.damage = damage;
        this.speed = speed;
        this.active = true;
    }

    public void update() {
        if (!active || target == null || target.isDead()) {
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
        active = false; // Marca para remoção
    }

    public boolean isActive() {
        return active;
    }

    public void draw(Graphics2D g2d) {
        if (!active) return;

        g2d.setColor(Color.ORANGE);
        g2d.fillOval((int)x - 3, (int)y - 3, 6, 6); // Projétil pequeno
    }
}