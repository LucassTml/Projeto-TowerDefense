package io.github.lucasstml.towerrogue.run;

import java.util.ArrayList;
import java.util.List;

/** Inventory of the merchant node the player is currently visiting. */
public final class ShopStock {
    public enum Kind { TOWER, RELIC }

    public static final class Item {
        public final Kind kind;
        public final String id;
        public final int price;
        public boolean sold;

        public Item(Kind kind, String id, int price) {
            this.kind = kind;
            this.id = id;
            this.price = price;
        }
    }

    public final List<Item> items = new ArrayList<>();
    public int forgePrice;
    public int healPrice;
    public boolean forgeUsed;
    public boolean healUsed;
}
