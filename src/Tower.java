// Tower.java
import java.awt.Point;
import java.awt.Graphics2D;
import java.awt.Color;      // Importado para usar cores
import java.awt.Font;       // Importado para definir a fonte do texto
import java.awt.FontMetrics; // Importado para medir o texto
import java.util.List;

public abstract class Tower {
    protected int x, y; // Coordenadas em pixels (centro da torre)
    protected double range;
    protected int fireRate; // Ticks do timer entre os disparos
    protected int fireCooldown;
    protected Enemy target;

    // Atributos de upgrade
    protected int level;
    protected int upgradeCost;

    public Tower(int x, int y, double range, int fireRate) {
        this.x = x;
        this.y = y;
        this.range = range;
        this.fireRate = fireRate;
        this.fireCooldown = 0; // Pode atirar imediatamente
        this.level = 1; // Todas as torres começam no nível 1
        this.upgradeCost = 50; // Custo inicial de upgrade padrão
    }

    // --- Métodos de Lógica do Jogo (mantidos inalterados) ---
    public void findTarget(List<Enemy> enemies) {
        target = null;
        double closestDistance = range + 1;

        for (Enemy e : enemies) {
            if (e.isAlive() && isInRange(e.getX(), e.getY())) {
                double distance = Point.distance(x, y, e.getX(), e.getY());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    target = e;
                }
            }
        }
    }

    public boolean isInRange(double enemyX, double enemyY) {
        return Point.distance(x, y, enemyX, enemyY) <= range;
    }

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

    // --- Métodos de Desenho e Upgrade ---

    // Método para desenhar a torre (inclui o desenho do nível)
    // As subclasses (BasicTower, SniperTower) devem chamar super.draw(g2d)
    // para que este código do nível seja executado.
    public void draw(Graphics2D g2d) {
        // O corpo da torre é desenhado nas subclasses (BasicTower, SniperTower).
        // Este método na classe base Tower é responsável por desenhar o nível.

        // Desenha o nível da torre (após a torre ser desenhada pela subclasse)
        g2d.setColor(Color.WHITE); // Cor do texto do nível (branco, para alto contraste)
        g2d.setFont(new Font("Arial", Font.BOLD, 10)); // Fonte menor e negrito

        String levelText = String.valueOf(level);

        // Calcula as métricas da fonte para centralizar o texto
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(levelText);
        int textHeight = fm.getHeight();
        int ascent = fm.getAscent(); // Parte da fonte acima da linha de base

        // Desenha o texto do nível, centralizando-o na posição (x, y) da torre
        g2d.drawString(levelText, x - textWidth / 2, y + ascent / 2 - (textHeight / 4)); // Ajuste fino no Y
    }

    // --- Métodos de Upgrade ---
    public boolean canUpgrade() {
        // Exemplo: Limitar o nível máximo
        return level < 3; // Permite upgrades até o nível 3
    }

    public void upgrade() {
        if (canUpgrade()) {
            level++;
            // A lógica de aumento de atributos é implementada nas subclasses (sobrescrevendo este método)
            // O custo do próximo upgrade também aumentará
            upgradeCost = (int)(upgradeCost * 1.5); // Aumenta o custo em 50% para o próximo nível
            System.out.println("Torre no nivel " + level + ", proximo upgrade custa: " + upgradeCost); // Debug
        }
    }

    // --- Getters ---
    public int getLevel() { return level; }
    public int getUpgradeCost() { return upgradeCost; }
    public int getX() { return x; }
    public int getY() { return y; }
    public double getRange() { return range; } // Getter para o alcance
}