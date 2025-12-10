// EnemyStatus.java
import java.util.HashMap;
import java.util.Map;

public class EnemyStatus {
    private Map<StatusEffect, Integer> activeEffects; // Efeito -> duração restante
    private double slowMultiplier; // Multiplicador de velocidade (1.0 = normal, 0.5 = 50% mais lento)
    private int burnDamage; // Dano de queimadura por tick
    private int burnTicks; // Ticks restantes de queimadura
    
    public EnemyStatus() {
        this.activeEffects = new HashMap<>();
        this.slowMultiplier = 1.0;
        this.burnDamage = 0;
        this.burnTicks = 0;
    }
    
    public void applyEffect(StatusEffect effect, int duration) {
        activeEffects.put(effect, duration);
        
        switch(effect) {
            case SLOW:
                slowMultiplier = 0.5; // 50% mais lento
                break;
            case FREEZE:
                slowMultiplier = 0.2; // 80% mais lento (quase parado)
                break;
            case BURN:
                burnDamage = 2; // 2 de dano por tick
                burnTicks = duration;
                break;
        }
    }
    
    public void update() {
        // Atualiza todos os efeitos ativos
        Map<StatusEffect, Integer> effectsToRemove = new HashMap<>();
        
        for (Map.Entry<StatusEffect, Integer> entry : activeEffects.entrySet()) {
            int newDuration = entry.getValue() - 1;
            if (newDuration <= 0) {
                effectsToRemove.put(entry.getKey(), 0);
            } else {
                activeEffects.put(entry.getKey(), newDuration);
            }
        }
        
        // Remove efeitos expirados
        for (StatusEffect effect : effectsToRemove.keySet()) {
            activeEffects.remove(effect);
            // Reseta o multiplicador se não houver mais slow/freeze
            if (effect == StatusEffect.SLOW || effect == StatusEffect.FREEZE) {
                if (!activeEffects.containsKey(StatusEffect.SLOW) && 
                    !activeEffects.containsKey(StatusEffect.FREEZE)) {
                    slowMultiplier = 1.0;
                } else if (activeEffects.containsKey(StatusEffect.FREEZE)) {
                    slowMultiplier = 0.2;
                } else if (activeEffects.containsKey(StatusEffect.SLOW)) {
                    slowMultiplier = 0.5;
                }
            }
            if (effect == StatusEffect.BURN) {
                burnDamage = 0;
                burnTicks = 0;
            }
        }
        
        // Atualiza queimadura
        if (burnTicks > 0) {
            burnTicks--;
            if (burnTicks <= 0) {
                burnDamage = 0;
            }
        }
    }
    
    public double getSlowMultiplier() {
        return slowMultiplier;
    }
    
    public int getBurnDamage() {
        return burnDamage;
    }
    
    public boolean hasEffect(StatusEffect effect) {
        return activeEffects.containsKey(effect);
    }
    
    public int getEffectDuration(StatusEffect effect) {
        return activeEffects.getOrDefault(effect, 0);
    }
}

