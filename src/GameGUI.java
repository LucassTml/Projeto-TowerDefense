import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe principal que gerencia a interface gráfica do jogo Tower Defense
 * Responsável por desenhar tudo na tela e gerenciar interações do usuário
 */
public class GameGUI extends JPanel {
    // Componentes principais do jogo
    private Map map;
    private Base base;
    private WaveManager waveManager;

    // Listas de entidades do jogo
    private List<Tower> towers = new ArrayList<>();
    private List<Projectile> projectiles = new ArrayList<>();
    
    // Dinheiro do jogador - começa com 100 pra dar uma chance inicial
    private int playerMoney = 100;

    // Estados do jogo: construção de torres ou onda em progresso
    private enum GameState { BUILDING_PHASE, WAVE_IN_PROGRESS }
    private GameState currentState;
    
    // Tipo de torre selecionada para construção (0 = nenhuma)
    private int selectedTowerType = 0; // 0 = Nada, 1 = Basic, 2 = Sniper, 3 = Ice
    
    // Sistema de velocidade - permite acelerar o jogo quando fica chato
    private int gameSpeed = 1; // 1 = normal, 3 = 3x mais rápido
    private Timer gameTimer;

    // Referências aos botões do JFrame principal, passados no construtor
    private JButton startWaveButton;
    private JRadioButton basicTowerButton;
    private JRadioButton sniperTowerButton;

    private JButton upgradeTowerButton; // Botão de upgrade
    private JButton sellTowerButton;     // Botão de venda
    private Tower selectedTower;        // Torre atualmente selecionada
    
    // Referência ao botão de torre de gelo (se existir)
    private JRadioButton iceTowerButton;

    // Necessário para acessar o ButtonGroup no placeTower (para desselecionar)
    private static ButtonGroup towerGroup;

    // Getter para o botão de upgrade (necessário para acessar do main)
    public JButton getUpgradeTowerButton() {
        return upgradeTowerButton;
    }
    
    // Getter para o botão de venda (necessário para acessar do main)
    public JButton getSellTowerButton() {
        return sellTowerButton;
    }
    
    /**
     * Altera a velocidade do jogo alterando o intervalo do timer
     * Quanto menor o intervalo, mais rápido o jogo roda
     */
    public void setGameSpeed(int speed) {
        this.gameSpeed = speed;
        
        // Calcula novo intervalo baseado na velocidade desejada
        int baseInterval = 50; // 50ms = ~20 FPS (velocidade normal)
        int newInterval = baseInterval / speed;
        
        // Proteção: não deixa ficar muito rápido pra não travar o PC
        if (newInterval < 10) newInterval = 10;
        
        // Aplica o novo intervalo ao timer
        gameTimer.setDelay(newInterval);
        gameTimer.setInitialDelay(newInterval);
    }
    
    public int getGameSpeed() {
        return gameSpeed;
    }


    // Construtor do GameGUI (compatibilidade com código antigo)
    public GameGUI(JButton startWaveButton, JRadioButton basicTowerButton, JRadioButton sniperTowerButton) {
        this(new GameConfig(10, 10, 2), startWaveButton, basicTowerButton, sniperTowerButton, null);
    }
    
    // Construtor principal com configuração
    public GameGUI(GameConfig config) {
        this(config, null, null, null, null);
    }
    
    // Construtor completo
    public GameGUI(GameConfig config, JButton startWaveButton, JRadioButton basicTowerButton, 
                   JRadioButton sniperTowerButton, JRadioButton iceTowerButton) {
        // --- Configuração do JPanel ---
        this.setLayout(null); // Usar layout nulo para posicionar componentes manualmente (botão de upgrade)
        this.setFocusable(true); // Permite que o painel receba eventos de teclado, se necessário


        // --- Inicialização de elementos do jogo ---
        map = new Map(config.rows, config.cols);
        base = new Base(100); // Base com 100 de vida

        // Gera caminho aleatório usando MapGenerator
        MapGenerator mapGen = new MapGenerator();
        int[][] pathCoords;
        switch(config.pathComplexity) {
            case 1:
                pathCoords = mapGen.generateSimplePath(config.rows, config.cols);
                break;
            case 2:
                pathCoords = mapGen.generateMediumPath(config.rows, config.cols);
                break;
            case 3:
                pathCoords = mapGen.generateComplexPath(config.rows, config.cols);
                break;
            default:
                pathCoords = mapGen.generateMediumPath(config.rows, config.cols);
        }
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
        this.iceTowerButton = iceTowerButton;


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

        // --- Configuração e adição do Botão de Venda ---
        sellTowerButton = new JButton("Vender"); // Inicializa o botão
        sellTowerButton.setBounds(680, 540, 150, 30); // Posição abaixo do botão de upgrade
        sellTowerButton.addActionListener(e -> {
            if (selectedTower != null) {
                sellTower(selectedTower);
            } else {
                JOptionPane.showMessageDialog(this, "Nenhuma torre selecionada para vender.");
            }
            repaint(); // Redesenha para atualizar HUD
        });
        this.add(sellTowerButton); // Adiciona o botão de venda ao JPanel


        // --- Configuração do estado inicial e loop do jogo ---
        this.currentState = GameState.BUILDING_PHASE;
        updateUIElements(); // AGORA, quando updateUIElements() é chamado, upgradeTowerButton já existe!

        // Timer principal do jogo
        gameTimer = new Timer(50, e -> gameLoop()); // 50ms = 20 FPS
        gameTimer.start();


        // --- MouseListener para Construção E Seleção de Torre ---
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Se um RadioButton de torre estiver selecionado, tente construir
                if (basicTowerButton.isSelected()) {
                    placeTower(e.getX(), e.getY(), 1); // 1 para BasicTower
                } else if (sniperTowerButton.isSelected()) {
                    placeTower(e.getX(), e.getY(), 2); // 2 para SniperTower
                } else if (iceTowerButton != null && iceTowerButton.isSelected()) {
                    placeTower(e.getX(), e.getY(), 3); // 3 para IceTower
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

    // Método para inicializar UI (chamado do menu)
    public void initializeUI(JButton startWaveButton, JRadioButton basicTowerButton, 
                             JRadioButton sniperTowerButton, JRadioButton iceTowerButton) {
        this.startWaveButton = startWaveButton;
        this.basicTowerButton = basicTowerButton;
        this.sniperTowerButton = sniperTowerButton;
        this.iceTowerButton = iceTowerButton;
    }
    
    // Método para selecionar o tipo de torre
    public void setSelectedTowerType(int type) {
        this.selectedTowerType = type;
        // Ao selecionar um tipo de torre para construção, desmarcar qualquer torre selecionada para upgrade
        selectedTower = null;
        repaint(); // Para remover o highlight da torre selecionada, se houver
        System.out.println("Tipo de torre selecionado: " + type);
    }
    
    // Método para vender uma torre
    private void sellTower(Tower tower) {
        // Calcula o valor de venda
        int sellValue = tower.getSellValue();
        
        // Confirmação antes de vender
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Vender esta torre por $" + sellValue + "?",
            "Confirmar Venda",
            JOptionPane.YES_NO_OPTION
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            // Adiciona o dinheiro ao jogador
            playerMoney += sellValue;
            
            // Libera o tile onde a torre estava
            int gridX = tower.getX() / 50;
            int gridY = tower.getY() / 50;
            Tile tile = map.getTile(gridX, gridY);
            if (tile != null) {
                tile.setCanBuild(true);
            }
            
            // Remove a torre da lista
            towers.remove(tower);
            
            // Desseleciona a torre
            selectedTower = null;
            
            JOptionPane.showMessageDialog(this, "Torre vendida por $" + sellValue + "!");
        }
    }

    /**
     * Tenta colocar uma torre na posição clicada
     * @param mouseX coordenada X do mouse em pixels
     * @param mouseY coordenada Y do mouse em pixels
     * @param type tipo de torre (1=Basic, 2=Sniper, 3=Ice)
     */
    private void placeTower(int mouseX, int mouseY, int type) {
        // Converte coordenadas de pixel para coordenadas do grid
        // Cada tile tem 50x50 pixels
        int gridX = mouseX / 50;
        int gridY = mouseY / 50;

        // Verifica se clicou dentro dos limites do mapa
        if (gridX < 0 || gridX >= map.getRows() || gridY < 0 || gridY >= map.getCols()) {
            // Clicou fora do mapa - ignora silenciosamente
            return;
        }

        Tile tile = map.getTile(gridX, gridY);

        // Só constrói se o tile permitir (não é caminho e não tem torre)
        if (tile != null && tile.canBuild()) {
            // Calcula o centro do tile em pixels (pra torre ficar centralizada)
            int centerX = gridX * 50 + 25;
            int centerY = gridY * 50 + 25;

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
            } else if (type == 3 && playerMoney >= IceTower.COST) { // IceTower
                playerMoney -= IceTower.COST;
                towers.add(new IceTower(centerX, centerY));
                tile.setCanBuild(false);
                JOptionPane.showMessageDialog(this, "Torre de Gelo construída!");
                if (towerGroup != null) towerGroup.clearSelection();
                selectedTowerType = 0;
            } else if (type == 3 && playerMoney < IceTower.COST) {
                JOptionPane.showMessageDialog(this, "Dinheiro insuficiente para construir a Torre de Gelo.");
            } else if (type != 0) { // Tentou construir mas não tinha dinheiro
                JOptionPane.showMessageDialog(this, "Dinheiro insuficiente para construir a torre.");
            }
        } else if (type != 0) { // Tentou construir em local inválido
            JOptionPane.showMessageDialog(this, "Não pode construir: Local inválido (caminho ou já ocupado).");
        }
    }


    /**
     * Loop principal do jogo - roda a cada frame
     * Atualiza inimigos, torres, projéteis, e verifica condições de vitória/derrota
     */
    private void gameLoop() {
        // Só atualiza o jogo se estiver em uma onda
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
        if (iceTowerButton != null) {
            iceTowerButton.setEnabled(playerMoney >= IceTower.COST);
            iceTowerButton.setText("Torre Gelo ($" + IceTower.COST + ")");
        }

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
        
        // Atualiza o botão de venda
        if (selectedTower != null) {
            int sellValue = selectedTower.getSellValue();
            sellTowerButton.setText("Vender ($" + sellValue + ")");
            sellTowerButton.setEnabled(true);
        } else {
            sellTowerButton.setText("Vender");
            sellTowerButton.setEnabled(false); // Desabilita se nenhuma torre selecionada
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
        // Mostra o menu inicial primeiro
        MenuInicial menu = new MenuInicial();
        menu.setVisible(true);
        
        // Espera até o jogo ser iniciado
        while (!menu.isGameStarted()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        // Obtém a configuração do menu
        GameConfig config = menu.getConfig();
        
        // Cria a janela do jogo
        JFrame frame = new JFrame("Tower Defense Projeto Alpha");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // --- 1. Criar os botões de controle ---
        JButton startWaveButton = new JButton("Carregando...");

        JRadioButton basicTowerButton = new JRadioButton("Torre Básica ($" + BasicTower.COST + ")");
        JRadioButton sniperTowerButton = new JRadioButton("Torre Sniper ($" + SniperTower.COST + ")");
        JRadioButton iceTowerButton = new JRadioButton("Torre Gelo ($" + IceTower.COST + ")");

        // Agrupar os RadioButtons
        towerGroup = new ButtonGroup();
        towerGroup.add(basicTowerButton);
        towerGroup.add(sniperTowerButton);
        towerGroup.add(iceTowerButton);

        // --- 2. Criar o painel do jogo (GameGUI) com a configuração ---
        GameGUI gamePanel = new GameGUI(config, startWaveButton, basicTowerButton, sniperTowerButton, iceTowerButton);

        // --- 3. Criar o painel de controle ---
        JPanel controlPanel = new JPanel();
        controlPanel.add(startWaveButton);
        controlPanel.add(basicTowerButton);
        controlPanel.add(sniperTowerButton);
        controlPanel.add(iceTowerButton);
        controlPanel.add(gamePanel.getUpgradeTowerButton());
        controlPanel.add(gamePanel.getSellTowerButton());
        
        // Botão de velocidade (3x / Normal)
        JButton speedButton = new JButton("Velocidade: 1x");
        speedButton.addActionListener(e -> {
            if (gamePanel.getGameSpeed() == 1) {
                gamePanel.setGameSpeed(3);
                speedButton.setText("Velocidade: 3x");
            } else {
                gamePanel.setGameSpeed(1);
                speedButton.setText("Velocidade: 1x");
            }
        });
        controlPanel.add(speedButton);

        // --- 4. Adicionar ActionListeners ---
        startWaveButton.addActionListener(e -> gamePanel.startWave());
        basicTowerButton.addActionListener(e -> gamePanel.setSelectedTowerType(1));
        sniperTowerButton.addActionListener(e -> gamePanel.setSelectedTowerType(2));
        iceTowerButton.addActionListener(e -> gamePanel.setSelectedTowerType(3));

        // --- 5. Adicionar os painéis ao Frame ---
        frame.add(gamePanel, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);

        // --- Configurações finais ---
        frame.setSize(850, 700);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
