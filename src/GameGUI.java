import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class GameGUI extends JPanel {
    private Map map;
    private Base base;
    private WaveManager waveManager;

    private List<Tower> towers = new ArrayList<>();
    private List<Projectile> projectiles = new ArrayList<>();
    private int playerMoney = 100; // Economia inicial

    private enum GameState { BUILDING_PHASE, WAVE_IN_PROGRESS }
    private GameState currentState;
    private int selectedTowerType = 0; // 0 = Nada, 1 = Basic, 2 = Sniper

    // Referências aos botões do JFrame principal, passados no construtor
    private JButton startWaveButton;
    private JRadioButton basicTowerButton;
    private JRadioButton sniperTowerButton;

    private JButton upgradeTowerButton; // Botão de upgrade
    private Tower selectedTower;        // Torre atualmente selecionada

    // Necessário para acessar o ButtonGroup no placeTower (para desselecionar)
    private static ButtonGroup towerGroup;


    // Construtor do GameGUI
    public GameGUI(JButton startWaveButton, JRadioButton basicTowerButton, JRadioButton sniperTowerButton) {
        // --- Configuração do JPanel ---
        this.setLayout(null); // Usar layout nulo para posicionar componentes manualmente (botão de upgrade)
        this.setFocusable(true); // Permite que o painel receba eventos de teclado, se necessário


        // --- Inicialização de elementos do jogo ---
        map = new Map(10, 10); // Grid de 10x10
        base = new Base(100); // Base com 100 de vida

        // Caminho dos inimigos
        int[][] pathCoords = {{0,0},{1,0},{2,0},{3,0},{4,0},{4,1},{4,2},{4,3},{3,3},{2,3},{1,3},{0,3}}; // Exemplo de caminho mais longo
        map.setPath(pathCoords);

        java.util.List<Point> path = new ArrayList<>();
        for (int[] p : pathCoords) {
            path.add(new Point(p[0] * 50 + 25, p[1] * 50 + 25)); // Caminho para o centro dos tiles
        }
        waveManager = new WaveManager(path);


        // --- Atribui as referências dos botões da UI ---
        this.startWaveButton = startWaveButton;
        this.basicTowerButton = basicTowerButton;
        this.sniperTowerButton = sniperTowerButton;


        // --- Configuração e adição do Botão de Upgrade (Corrigido para inicializar antes de updateUIElements) ---
        upgradeTowerButton = new JButton("Upgrade"); // Inicializa o botão
        upgradeTowerButton.setBounds(680, 500, 150, 30); // Posição. Ajuste conforme necessário no seu layout
        upgradeTowerButton.addActionListener(e -> {
            if (selectedTower != null) {
                if (selectedTower.canUpgrade()) {
                    if (playerMoney >= selectedTower.getUpgradeCost()) {
                        playerMoney -= selectedTower.getUpgradeCost();
                        selectedTower.upgrade();
                        JOptionPane.showMessageDialog(this, "Torre atualizada para o Nível " + selectedTower.getLevel() + "!");
                    } else {
                        JOptionPane.showMessageDialog(this, "Dinheiro insuficiente para o upgrade!");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Torre já está no nível máximo!");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Nenhuma torre selecionada para upgrade.");
            }
            repaint(); // Redesenha para atualizar HUD e botão de upgrade
        });
        this.add(upgradeTowerButton); // Adiciona o botão de upgrade ao JPanel


        // --- Configuração do estado inicial e loop do jogo ---
        this.currentState = GameState.BUILDING_PHASE;
        updateUIElements(); // AGORA, quando updateUIElements() é chamado, upgradeTowerButton já existe!

        // Timer principal do jogo
        Timer timer = new Timer(50, e -> gameLoop()); // 50ms = 20 FPS
        timer.start();


        // --- MouseListener para Construção E Seleção de Torre ---
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Se um RadioButton de torre estiver selecionado, tente construir
                if (basicTowerButton.isSelected()) {
                    placeTower(e.getX(), e.getY(), 1); // 1 para BasicTower
                } else if (sniperTowerButton.isSelected()) {
                    placeTower(e.getX(), e.getY(), 2); // 2 para SniperTower
                } else {
                    // Se nenhum RadioButton de torre estiver selecionado, tente selecionar uma torre existente
                    selectedTower = null; // Limpa a seleção anterior
                    // Verifica se o clique foi na área do mapa (evita selecionar torres clicando nos controles)
                    if (e.getX() < map.getRows() * 50 && e.getY() < map.getCols() * 50) {
                        for (Tower t : towers) {
                            // Calcula a distância do clique para o centro da torre
                            // O x e y da torre são o centro do tile (e.g., 25, 75, 125...)
                            double dx = e.getX() - t.getX();
                            double dy = e.getY() - t.getY();
                            double distance = Math.sqrt(dx * dx + dy * dy);
                            // Assumindo um raio de seleção de 25px (metade do tamanho de um tile de 50px)
                            if (distance <= 25) { // O raio de seleção é um pouco maior que a torre para facilitar
                                selectedTower = t;
                                System.out.println("Torre selecionada: Nível " + t.getLevel() + ", Custo upgrade: $" + t.getUpgradeCost());
                                break; // Selecionou uma torre, para de procurar
                            }
                        }
                    }
                }
                repaint(); // Redesenha para mostrar a seleção (ex: círculo de alcance)
            }
        });

    } // Fim do construtor GameGUI


    // Método para iniciar a onda
    public void startWave() {
        if (currentState == GameState.BUILDING_PHASE) {
            waveManager.startNextWave();
            currentState = GameState.WAVE_IN_PROGRESS;
            selectedTower = null; // Desseleciona a torre ao iniciar a onda
        }
    }

    // Método para selecionar o tipo de torre
    public void setSelectedTowerType(int type) {
        this.selectedTowerType = type;
        // Ao selecionar um tipo de torre para construção, desmarcar qualquer torre selecionada para upgrade
        selectedTower = null;
        repaint(); // Para remover o highlight da torre selecionada, se houver
        System.out.println("Tipo de torre selecionado: " + type);
    }

    // Método para colocar a torre
    private void placeTower(int mouseX, int mouseY, int type) { // Adicionado 'type' como parâmetro
        int gridX = mouseX / 50; // Coordenada de grid X
        int gridY = mouseY / 50; // Coordenada de grid Y

        // Verifica se clicou dentro dos limites do mapa
        if (gridX < 0 || gridX >= map.getRows() || gridY < 0 || gridY >= map.getCols()) {
            // Clicou fora do mapa, ignora ou dá feedback
            //System.out.println("Clicou fora do mapa ou na área de controle.");
            return;
        }

        Tile tile = map.getTile(gridX, gridY);

        if (tile != null && tile.canBuild()) {
            int centerX = gridX * 50 + 25; // Centro do tile em pixels
            int centerY = gridY * 50 + 25; // Centro do tile em pixels

            if (type == 1 && playerMoney >= BasicTower.COST) { // BasicTower
                playerMoney -= BasicTower.COST;
                towers.add(new BasicTower(centerX, centerY));
                tile.setCanBuild(false);
                JOptionPane.showMessageDialog(this, "Torre Básica construída!");
                if (towerGroup != null) towerGroup.clearSelection(); // Desseleciona o radio button
                selectedTowerType = 0; // Resetar tipo de torre selecionado
            } else if (type == 2 && playerMoney >= SniperTower.COST) { // SniperTower
                playerMoney -= SniperTower.COST;
                towers.add(new SniperTower(centerX, centerY));
                tile.setCanBuild(false);
                JOptionPane.showMessageDialog(this, "Torre Sniper construída!");
                if (towerGroup != null) towerGroup.clearSelection(); // Desseleciona o radio button
                selectedTowerType = 0;
            } else if (type != 0) { // Tentou construir mas não tinha dinheiro
                JOptionPane.showMessageDialog(this, "Dinheiro insuficiente para construir a torre.");
            }
        } else if (type != 0) { // Tentou construir em local inválido
            JOptionPane.showMessageDialog(this, "Não pode construir: Local inválido (caminho ou já ocupado).");
        }
    }


    // Lógica principal do jogo (gameLoop)
    private void gameLoop() {
        if (currentState == GameState.WAVE_IN_PROGRESS) {
            int moneyFromKills = waveManager.update();
            playerMoney += moneyFromKills;

            List<Enemy> enemiesReachedEnd = new ArrayList<>();
            for (Enemy enemy : waveManager.getEnemies()) {
                if (enemy.reachedEnd()) {
                    base.receberDano(enemy.getDamage());
                    enemiesReachedEnd.add(enemy);
                }
            }
            waveManager.getEnemies().removeAll(enemiesReachedEnd);

            if (base.estaDestruida()) {
                JOptionPane.showMessageDialog(this, "GAME OVER! Sua base foi destruída na onda " + waveManager.getCurrentWave() + "!");
                System.exit(0); // Sai do programa
            }

            for (Tower tower : towers) {
                Projectile p = tower.update(waveManager.getEnemies());
                if (p != null) {
                    projectiles.add(p);
                }
            }

            List<Projectile> projectilesToRemove = new ArrayList<>();
            for (Projectile p : projectiles) {
                p.update();
                if (!p.isActive()) {
                    projectilesToRemove.add(p);
                }
            }
            projectiles.removeAll(projectilesToRemove);

            if (waveManager.isWaveComplete() && waveManager.getCurrentWave() > 0) {
                currentState = GameState.BUILDING_PHASE;
                JOptionPane.showMessageDialog(this, "Onda " + waveManager.getCurrentWave() + " completada! Prepare-se para a próxima.");
                selectedTower = null; // Limpa a seleção ao fim da onda
            }
        }

        updateUIElements(); // Sempre atualiza o HUD e botões
        repaint(); // Redesenha a tela
    }


    // Atualiza os elementos da UI (botões, texto)
    private void updateUIElements() {
        if (currentState == GameState.BUILDING_PHASE) {
            startWaveButton.setEnabled(true);
            startWaveButton.setText("Iniciar Onda " + (waveManager.getCurrentWave() + 1));
        } else {
            startWaveButton.setEnabled(false);
            startWaveButton.setText("Onda " + waveManager.getCurrentWave() + " em progresso...");
        }

        basicTowerButton.setEnabled(playerMoney >= BasicTower.COST);
        sniperTowerButton.setEnabled(playerMoney >= SniperTower.COST);

        basicTowerButton.setText("Torre Básica ($" + BasicTower.COST + ")");
        sniperTowerButton.setText("Torre Sniper ($" + SniperTower.COST + ")");

        // MODIFICAÇÃO: Atualiza o texto do botão de upgrade dinamicamente
        if (selectedTower != null && selectedTower.canUpgrade()) {
            upgradeTowerButton.setText("Upgrade ($" + selectedTower.getUpgradeCost() + ")");
            upgradeTowerButton.setEnabled(playerMoney >= selectedTower.getUpgradeCost());
        } else if (selectedTower != null && !selectedTower.canUpgrade()) {
            upgradeTowerButton.setText("Máx. Nível");
            upgradeTowerButton.setEnabled(false); // Desabilita se já no nível máximo
        } else {
            upgradeTowerButton.setText("Upgrade");
            upgradeTowerButton.setEnabled(false); // Desabilita se nenhuma torre selecionada
        }
    }


    // Método para desenhar componentes (paintComponent)
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Desenho do mapa
        for (int i = 0; i < map.getRows(); i++) {
            for (int j = 0; j < map.getCols(); j++) {
                Tile t = map.getTile(i, j);
                if (t.isPath()) {
                    g.setColor(Color.LIGHT_GRAY);
                } else {
                    g.setColor(Color.GREEN);
                }
                g.fillRect(i * 50, j * 50, 50, 50);
                g.setColor(Color.BLACK);
                g.drawRect(i * 50, j * 50, 50, 50);
            }
        }

        // Desenho dos inimigos
        for (Enemy e : waveManager.getEnemies()) {
            e.draw(g2d);
        }

        // Desenho das torres
        for (Tower t : towers) {
            t.draw(g2d);
        }

        // Desenhar o alcance da torre selecionada
        if (selectedTower != null) {
            g2d.setColor(new Color(255, 255, 0, 80)); // Amarelo transparente
            // O x e y da torre são o centro, então subtrai o range para o canto superior esquerdo do círculo
            g2d.fillOval(selectedTower.getX() - (int)selectedTower.range,
                    selectedTower.getY() - (int)selectedTower.range,
                    (int)selectedTower.range * 2, // Diâmetro
                    (int)selectedTower.range * 2); // Diâmetro
            g2d.setColor(Color.YELLOW);
            g2d.drawOval(selectedTower.getX() - (int)selectedTower.range,
                    selectedTower.getY() - (int)selectedTower.range,
                    (int)selectedTower.range * 2,
                    (int)selectedTower.range * 2);
        }

        // Desenho dos projéteis
        for (Projectile p : projectiles) {
            p.draw(g2d);
        }

        // Desenho do HUD
        Font hudFont = new Font("Verdana", Font.BOLD, 16);
        g.setFont(hudFont);
        g.setColor(Color.BLACK);

        String waveText = "Onda: " + waveManager.getCurrentWave();
        String healthText = "Base: " + base.getVidaAtual() + "/" + base.getVidaMaxima();
        String moneyText = "Dinheiro: $" + playerMoney;

        g.drawString(waveText, 10, 390);
        g.drawString(healthText, 10, 420);
        g.drawString(moneyText, 10, 450);

        int barWidth = 150;
        int barHeight = 20;
        int barX = 10;
        int barY = 460;

        double healthPercentage = (double) base.getVidaAtual() / base.getVidaMaxima();
        int greenWidth = (int) (barWidth * healthPercentage);

        g.setColor(new Color(100, 0, 0));
        g.fillRect(barX, barY, barWidth, barHeight);
        g.setColor(Color.GREEN);
        g.fillRect(barX, barY, greenWidth, barHeight);
        g.setColor(Color.BLACK);
        g.drawRect(barX, barY, barWidth, barHeight);
    }


    // Método main para iniciar o JFrame (janela principal)
    public static void main(String[] args) {
        JFrame frame = new JFrame("Tower Defense Projeto Alpha");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout()); // Usa BorderLayout para organizar os painéis

        // --- 1. Criar os botões de controle ---
        JButton startWaveButton = new JButton("Carregando..."); // Botão para iniciar ondas

        JRadioButton basicTowerButton = new JRadioButton("Torre Básica ($" + BasicTower.COST + ")");
        JRadioButton sniperTowerButton = new JRadioButton("Torre Sniper ($" + SniperTower.COST + ")");

        // Agrupar os RadioButtons (para que só um seja selecionado por vez)
        towerGroup = new ButtonGroup(); // Instancia o ButtonGroup aqui
        towerGroup.add(basicTowerButton);
        towerGroup.add(sniperTowerButton);

        // --- 2. Criar o painel do jogo (GameGUI) ---
        // Passamos os botões para o construtor do GameGUI para que ele possa gerenciá-los
        GameGUI gamePanel = new GameGUI(startWaveButton, basicTowerButton, sniperTowerButton);


        // --- 3. Criar o painel de controle para os botões e adicionar os botões ---
        JPanel controlPanel = new JPanel(); // Painel para os botões na parte inferior
        controlPanel.add(startWaveButton);
        controlPanel.add(basicTowerButton);
        controlPanel.add(sniperTowerButton);

        // Adicionar o botão de upgrade ao painel de controle
        controlPanel.add(gamePanel.upgradeTowerButton); // Pega o botão já criado e adicionado ao gamePanel


        // --- 4. Adicionar ActionListeners aos botões ---
        startWaveButton.addActionListener(e -> gamePanel.startWave());

        basicTowerButton.addActionListener(e -> gamePanel.setSelectedTowerType(1));
        sniperTowerButton.addActionListener(e -> gamePanel.setSelectedTowerType(2));


        // --- 5. Adicionar os painéis ao Frame principal ---
        frame.add(gamePanel, BorderLayout.CENTER); // O painel do jogo ocupa o centro
        frame.add(controlPanel, BorderLayout.SOUTH); // O painel de controle fica na parte inferior

        // --- Configurações finais do Frame ---
        frame.setSize(850, 700); // Ajusta o tamanho da janela para acomodar a UI lateral, se houver
        frame.setResizable(false); // Opcional: impede o redimensionamento da janela
        frame.setLocationRelativeTo(null); // Centraliza a janela na tela
        frame.setVisible(true); // Torna a janela visível
    }
}
