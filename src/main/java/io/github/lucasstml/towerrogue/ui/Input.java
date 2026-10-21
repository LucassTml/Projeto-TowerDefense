package io.github.lucasstml.towerrogue.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread-safe input bridge. AWT listeners run on the Event Dispatch Thread and only
 * enqueue small records; the game loop thread drains the queue once per frame in
 * {@link #poll}. Game state is therefore only ever touched by one thread, with no locks.
 */
public final class Input implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    private sealed interface Event permits Move, Button, Wheel, Key, Typed {
    }

    private record Move(int x, int y) implements Event {
    }

    private record Button(int button, boolean down, int x, int y) implements Event {
    }

    private record Wheel(int rotation) implements Event {
    }

    private record Key(int code, boolean down) implements Event {
    }

    private record Typed(char c) implements Event {
    }

    private final ConcurrentLinkedQueue<Event> queue = new ConcurrentLinkedQueue<>();

    // ---- frame state, loop thread only ----
    private double rawX = -1000, rawY = -1000;
    public double mx = -1000, my = -1000;
    public boolean leftDown;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean clickConsumed;
    public int wheel;
    private final Set<Integer> down = new HashSet<>();
    private final Set<Integer> pressed = new HashSet<>();
    private final StringBuilder typed = new StringBuilder();
    public boolean shift;

    /** Drains queued events and maps window coordinates into the virtual 1280×720 space. */
    public void poll(double scale, double offsetX, double offsetY) {
        leftPressed = false;
        rightPressed = false;
        clickConsumed = false;
        wheel = 0;
        pressed.clear();
        typed.setLength(0);
        Event e;
        while ((e = queue.poll()) != null) {
            switch (e) {
                case Move m -> {
                    rawX = m.x();
                    rawY = m.y();
                }
                case Button b -> {
                    rawX = b.x();
                    rawY = b.y();
                    if (b.button() == MouseEvent.BUTTON1) {
                        leftDown = b.down();
                        if (b.down()) leftPressed = true;
                    } else if (b.button() == MouseEvent.BUTTON3 && b.down()) {
                        rightPressed = true;
                    }
                }
                case Wheel w -> wheel += w.rotation();
                case Key k -> {
                    if (k.down()) {
                        if (down.add(k.code())) pressed.add(k.code());
                    } else {
                        down.remove(k.code());
                    }
                }
                case Typed t -> typed.append(t.c());
            }
        }
        shift = down.contains(KeyEvent.VK_SHIFT);
        mx = (rawX - offsetX) / scale;
        my = (rawY - offsetY) / scale;
    }

    /** True once per left press; consumed by the first widget that handles it. */
    public boolean clicked() {
        return leftPressed && !clickConsumed;
    }

    public void consumeClick() {
        clickConsumed = true;
    }

    public boolean rightClicked() {
        return rightPressed;
    }

    public boolean keyPressed(int vk) {
        return pressed.contains(vk);
    }

    public String typed() {
        return typed.toString();
    }

    /** Swallows this frame's key presses and typed text (used while an overlay owns the keyboard). */
    public void clearKeys() {
        pressed.clear();
        typed.setLength(0);
    }

    // ---- AWT side (Event Dispatch Thread) ----

    @Override public void mouseMoved(MouseEvent e) { queue.add(new Move(e.getX(), e.getY())); }
    @Override public void mouseDragged(MouseEvent e) { queue.add(new Move(e.getX(), e.getY())); }
    @Override public void mousePressed(MouseEvent e) { queue.add(new Button(e.getButton(), true, e.getX(), e.getY())); }
    @Override public void mouseReleased(MouseEvent e) { queue.add(new Button(e.getButton(), false, e.getX(), e.getY())); }
    @Override public void mouseClicked(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { queue.add(new Move(-1000, -1000)); }
    @Override public void mouseWheelMoved(MouseWheelEvent e) { queue.add(new Wheel(e.getWheelRotation())); }
    @Override public void keyPressed(KeyEvent e) { queue.add(new Key(e.getKeyCode(), true)); }
    @Override public void keyReleased(KeyEvent e) { queue.add(new Key(e.getKeyCode(), false)); }
    @Override public void keyTyped(KeyEvent e) {
        if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) queue.add(new Typed(e.getKeyChar()));
    }
}
