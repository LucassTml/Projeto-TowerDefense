/**
 * Classe abstrata que representa uma torre no jogo
 * Todas as torres (Básica, Sniper, Gelo) herdam desta classe
 */
import java.awt.Point;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.List;

public abstract class Tower {
    // Posição da torre no mapa (em pixels, centro do tile)
    protected int x, y;
    
    // Alcance de ataque da torre
    protected double range;
    
    // Taxa de disparo: quantos ticks precisa esperar entre cada tiro
    // Quanto menor, mais rápido atira
    protected int fireRate;
    
    // Contador de cooldown - quando chega a 0, pode atirar
    protected int fireCooldown;
    
    // Inimigo que está sendo atacado no momento
    protected Enemy target;

    // Sistema de upgrades
    protected int level;          // Nível atual (1, 2 ou 3)
    protected int upgradeCost;     // Quanto custa para fazer upgrade

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
    
    // Cada torre concreta deve retornar seu custo base de construção
    public abstract int getBaseCost();

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

    // --- Sistema de Upgrades ---
    
    /**
     * Verifica se a torre pode ser melhorada
     * Limitei em 3 níveis pra não ficar muito OP
     */
    public boolean canUpgrade() {
        return level < 3;
    }

    /**
     * Faz upgrade da torre
     * As subclasses sobrescrevem este método pra aumentar atributos específicos
     * (alcance, dano, etc)
     */
    public void upgrade() {
        if (canUpgrade()) {
            level++;
            
            // Aumenta o custo do próximo upgrade em 50%
            // Assim fica mais caro conforme melhora (balanceamento)
            upgradeCost = (int)(upgradeCost * 1.5);
            
            // Debug - pode remover depois se quiser
            System.out.println("Torre no nivel " + level + ", proximo upgrade custa: " + upgradeCost);
        }
    }

    // --- Métodos de Venda ---
    // Calcula o valor de venda da torre (70% do custo base + upgrades gastos)
    public int getSellValue() {
        int baseValue = getBaseCost();
        int totalUpgradeCost = 0;
        
        // Calcula o custo total de upgrades gastos
        // Nível 2: custo do primeiro upgrade
        // Nível 3: custo do primeiro + segundo upgrade
        if (level >= 2) {
            // Custo do upgrade para nível 2 (upgradeCost inicial)
            int firstUpgradeCost = (int)(baseValue * 0.8); // Aproximação do custo inicial
            totalUpgradeCost += firstUpgradeCost;
        }
        if (level >= 3) {
            // Custo do upgrade para nível 3 (1.5x do anterior)
            int firstUpgradeCost = (int)(baseValue * 0.8);
            int secondUpgradeCost = (int)(firstUpgradeCost * 1.5);
            totalUpgradeCost += secondUpgradeCost;
        }
        
        // Retorna 70% do valor total investido (custo base + upgrades)
        return (int)((baseValue + totalUpgradeCost) * 0.7);
    }
    
    // Método auxiliar para calcular o custo total de upgrades gastos
    // Este método será sobrescrito nas subclasses para usar os valores corretos
    protected int calculateTotalUpgradeCost() {
        int total = 0;
        int baseCost = getBaseCost();
        
        // Aproximação: assume que o primeiro upgrade custa 80% do custo base
        // e o segundo custa 1.5x do primeiro
        if (level >= 2) {
            int firstUpgrade = (int)(baseCost * 0.8);
            total += firstUpgrade;
        }
        if (level >= 3) {
            int firstUpgrade = (int)(baseCost * 0.8);
            int secondUpgrade = (int)(firstUpgrade * 1.5);
            total += secondUpgrade;
        }
        
        return total;
    }

    // --- Getters ---
    public int getLevel() { return level; }
    public int getUpgradeCost() { return upgradeCost; }
    public int getX() { return x; }
    public int getY() { return y; }
    public double getRange() { return range; } // Getter para o alcance
}