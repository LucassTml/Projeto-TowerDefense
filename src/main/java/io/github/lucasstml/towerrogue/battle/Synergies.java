package io.github.lucasstml.towerrogue.battle;

import io.github.lucasstml.towerrogue.content.StatMod;
import io.github.lucasstml.towerrogue.content.SynergyDef;
import io.github.lucasstml.towerrogue.content.TowerDef;

import java.util.ArrayList;
import java.util.List;

/** Tracks which board synergies are active for the current set of towers. */
public final class Synergies {

    /**
     * @param count  towers with the tag (or, for "requires all" synergies, how many required tags are present)
     * @param needed count required for the next tier (or the max tier when complete)
     * @param tier   index of the highest active tier, -1 if inactive
     */
    public record Status(SynergyDef def, int count, int needed, int tier) {
        public boolean active() {
            return tier >= 0;
        }
    }

    private final List<SynergyDef> defs;
    private List<Status> statuses = List.of();

    Synergies(List<SynergyDef> defs) {
        this.defs = defs;
    }

    void recompute(List<Tower> towers) {
        List<Status> out = new ArrayList<>();
        for (SynergyDef s : defs) {
            int count;
            if (!s.requiresAll().isEmpty()) {
                count = 0;
                for (String tag : s.requiresAll()) {
                    for (Tower t : towers) {
                        if (t.def.hasTag(tag)) {
                            count++;
                            break;
                        }
                    }
                }
                boolean complete = count == s.requiresAll().size();
                out.add(new Status(s, count, s.requiresAll().size(), complete ? 0 : -1));
                continue;
            }
            count = 0;
            for (Tower t : towers) if (t.def.hasTag(s.tag())) count++;
            int tier = -1;
            for (int i = 0; i < s.tiers().size(); i++) if (count >= s.tiers().get(i).count()) tier = i;
            int needed = tier + 1 < s.tiers().size() ? s.tiers().get(tier + 1).count() : s.tiers().get(s.tiers().size() - 1).count();
            out.add(new Status(s, count, needed, tier));
        }
        statuses = List.copyOf(out);
    }

    /** Stat mods every active synergy grants to a tower of this type. */
    List<StatMod> modsFor(TowerDef def) {
        List<StatMod> mods = new ArrayList<>();
        for (Status st : statuses) {
            if (!st.active()) continue;
            SynergyDef.Tier tier = st.def().tiers().get(st.tier());
            boolean applies = "all".equals(tier.scope()) || (st.def().tag() != null && def.hasTag(st.def().tag()));
            if (applies) mods.addAll(tier.mods());
        }
        return mods;
    }

    public List<Status> statuses() {
        return statuses;
    }
}
