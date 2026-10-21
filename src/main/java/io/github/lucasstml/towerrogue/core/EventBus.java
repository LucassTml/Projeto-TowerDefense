package io.github.lucasstml.towerrogue.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Minimal synchronous publish/subscribe bus keyed by event class.
 *
 * <p>The battle simulation publishes facts ("enemy killed", "damage about to be dealt")
 * and relics subscribe to them. This keeps the simulation free of relic-specific code:
 * a new relic is a new subscriber, not a new {@code if} in the core loop.
 *
 * <p>Handlers run in subscription order, which is deterministic (relic order), so the
 * bus never introduces nondeterminism.
 */
public final class EventBus {
    private final Map<Class<?>, List<Consumer<Object>>> handlers = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <E> void subscribe(Class<E> type, Consumer<? super E> handler) {
        handlers.computeIfAbsent(type, k -> new ArrayList<>()).add((Consumer<Object>) handler);
    }

    public void publish(Object event) {
        List<Consumer<Object>> list = handlers.get(event.getClass());
        if (list == null) return;
        // Index loop: a handler may subscribe new handlers without breaking iteration.
        for (int i = 0; i < list.size(); i++) list.get(i).accept(event);
    }
}
