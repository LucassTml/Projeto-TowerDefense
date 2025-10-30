import java.awt.Point;
import java.awt.Graphics2D;
import java.util.List;

public abstract class Tower {
    protected int x, y; // Coordenadas em pixels (centro da torre)
    protected double range;
    protected int fireRate; // Ticks do timer entre os disparos
    protected int fireCooldown;
    protected Enemy target;

    public Tower(int x, int y, double range, int fireRate) {
        this.x = x;
        this.y = y;
        this.range = range;
        this.fireRate = fireRate;
        this.fireCooldown = 0; // Pode atirar imediatamente
    }

    // Encontra um alvo (o primeiro inimigo dentro do alcance)
    protected void findTarget(List<Enemy> enemies) {
        if (target != null && (target.isDead() || !isInRange(target))) {
            target = null; // Limpa o alvo se ele morreu ou saiu do alcance
        }

        if (target == null) {
            for (Enemy e : enemies) {
                if (isInRange(e)) {
                    target = e;
                    break;
                }
            }
        }
    }

    // Verifica se um inimigo está no alcance
    protected boolean isInRange(Enemy e) {
        // Distância Euclidiana (Pitagoras)
        double dx = x - (e.getX() + 20); // +20 para mirar no centro do inimigo (40x40)
        double dy = y - (e.getY() + 20);
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance <= range;
    }

    // Método principal de atualização da torre
    public Projectile update(List<Enemy> enemies) {
        findTarget(enemies);

        if (fireCooldown > 0) {
            fireCooldown--;
        }

        if (target != null && fireCooldown <= 0) {
            fireCooldown = fireRate; // Reinicia o cooldown
            return createProjectile(); // Dispara
        }

        return null; // Nenhum projétil disparado
    }

    // Cada torre concreta implementará como seu projétil é criado
    protected abstract Projectile createProjectile();

    // Método para desenhar a torre (e seu alcance, se estiver selecionada)
    public void draw(Graphics2D g2d) {
        // Desenha a base da torre
        g2d.setColor(java.awt.Color.BLUE);
        g2d.fillOval(x - 15, y - 15, 30, 30); // Círculo de 30px centrado
        g2d.setColor(java.awt.Color.BLACK);
        g2d.drawOval(x - 15, y - 15, 30, 30);
    }

    // Getters
    public int getX() { return x; }
    public int getY() { return y; }
}