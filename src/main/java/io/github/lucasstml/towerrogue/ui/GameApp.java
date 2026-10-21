package io.github.lucasstml.towerrogue.ui;

import io.github.lucasstml.towerrogue.content.Content;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;

/**
 * Window + game loop.
 *
 * <p>Swing's repaint model is not used for the game: a dedicated thread runs a classic
 * fixed-timestep loop ("Fix Your Timestep") and draws with active rendering through a
 * {@link BufferStrategy}. Logic always advances in 1/60 s steps however fast or slow the
 * machine renders, and everything is drawn at a virtual 1280×720 resolution, scaled and
 * letterboxed to the window, so the layout never depends on the screen size.
 */
public final class GameApp {
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    private static final double DT = 1.0 / 60.0;
    private static final double MAX_FPS = 144;

    private final JFrame frame = new JFrame("Tower Rogue");
    private final Canvas canvas = new Canvas();
    private final Input input = new Input();
    private final App app;
    private volatile boolean running = true;
    private volatile boolean switchingDisplay;
    private boolean fullscreen;

    private GameApp(Content content) {
        this.app = new App(content, input);
    }

    public static void launch(Content content) {
        SwingUtilities.invokeLater(() -> new GameApp(content).start());
    }

    private void start() {
        canvas.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        canvas.setBackground(Color.BLACK);
        canvas.setIgnoreRepaint(true);
        canvas.setFocusTraversalKeysEnabled(false);
        canvas.addMouseListener(input);
        canvas.addMouseMotionListener(input);
        canvas.addMouseWheelListener(input);
        canvas.addKeyListener(input);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                running = false;
            }
        });
        frame.add(canvas);
        frame.pack();
        frame.setMinimumSize(new Dimension(800, 480));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        canvas.createBufferStrategy(2);
        canvas.requestFocusInWindow();
        Thread loop = new Thread(this::loop, "game-loop");
        loop.setDaemon(false);
        loop.start();
    }

    private void loop() {
        long last = System.nanoTime();
        double accumulator = 0;
        while (running && !app.quitRequested()) {
            long now = System.nanoTime();
            double frameTime = Math.min(0.25, (now - last) / 1e9);
            last = now;
            accumulator += frameTime;

            int cw = Math.max(1, canvas.getWidth()), ch = Math.max(1, canvas.getHeight());
            double scale = Math.min(cw / (double) WIDTH, ch / (double) HEIGHT);
            double ox = (cw - WIDTH * scale) / 2, oy = (ch - HEIGHT * scale) / 2;
            input.poll(scale, ox, oy);
            if (input.keyPressed(KeyEvent.VK_F11)) SwingUtilities.invokeLater(this::toggleFullscreen);

            try {
                while (accumulator >= DT) {
                    app.update(DT);
                    accumulator -= DT;
                }
                render(frameTime, cw, ch, scale, ox, oy);
            } catch (Throwable t) {
                app.crashed(t);
                accumulator = 0;
            }
            Toolkit.getDefaultToolkit().sync();
            long spent = System.nanoTime() - now;
            long budget = (long) (1e9 / MAX_FPS);
            if (spent < budget) {
                try {
                    Thread.sleep((budget - spent) / 1_000_000, (int) ((budget - spent) % 1_000_000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        app.shutdown();
        SwingUtilities.invokeLater(() -> {
            frame.dispose();
            System.exit(0);
        });
    }

    private void render(double frameTime, int cw, int ch, double scale, double ox, double oy) {
        BufferStrategy bs = canvas.getBufferStrategy();
        if (bs == null || switchingDisplay) return;
        try {
            renderTo(bs, frameTime, cw, ch, scale, ox, oy);
        } catch (IllegalStateException e) {
            // The buffer strategy was replaced (fullscreen toggle); skip this frame.
        }
    }

    private void renderTo(BufferStrategy bs, double frameTime, int cw, int ch, double scale, double ox, double oy) {
        do {
            do {
                Graphics2D g = (Graphics2D) bs.getDrawGraphics();
                try {
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, cw, ch);
                    g.translate(ox, oy);
                    g.scale(scale, scale);
                    g.clipRect(0, 0, WIDTH, HEIGHT);
                    applyHints(g);
                    app.renderFrame(g, frameTime);
                } finally {
                    g.dispose();
                }
            } while (bs.contentsRestored());
            bs.show();
        } while (bs.contentsLost());
    }

    public static void applyHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    private void toggleFullscreen() {
        switchingDisplay = true;
        try {
            switchDisplayMode();
        } finally {
            switchingDisplay = false;
        }
    }

    private void switchDisplayMode() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        fullscreen = !fullscreen;
        frame.dispose();
        frame.setUndecorated(fullscreen);
        if (fullscreen && device.isFullScreenSupported()) {
            device.setFullScreenWindow(frame);
        } else {
            device.setFullScreenWindow(null);
            frame.pack();
            frame.setLocationRelativeTo(null);
        }
        frame.setVisible(true);
        canvas.createBufferStrategy(2);
        canvas.requestFocusInWindow();
    }
}
