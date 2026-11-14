package com.tabcraft.market;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MarketMenus implements Listener {

    static final class MarketHolder implements InventoryHolder {
        final Category category; // null = ana menu
        Inventory inventory;

        MarketHolder(Category category) {
            this.category = category;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final MarketPlugin plugin;
    private final Eco eco;
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private String mainTitle = "SUNUCU MAĞAZASI";

    MarketMenus(MarketPlugin plugin, Eco eco) {
        this.plugin = plugin;
        this.eco = eco;
    }

    // ---------------------------------------------------------------- config

    void load() {
        FileConfiguration cfg = plugin.getConfig();
        categories.clear();
        mainTitle = cfg.getString("ana-menu.baslik", "SUNUCU MAĞAZASI");

        ConfigurationSection cats = cfg.getConfigurationSection("kategoriler");
        if (cats == null) return;

        for (String id : cats.getKeys(false)) {
            ConfigurationSection s = cats.getConfigurationSection(id);
            if (s == null) continue;

            Material icon = Material.matchMaterial(s.getString("ikon", "STONE"));
            if (icon == null || !icon.isItem()) icon = Material.STONE;
            int slot = Math.max(0, Math.min(26, s.getInt("slot", 13)));
            String name = s.getString("isim", id);
            String title = s.getString("baslik", name);

            List<MarketItem> items = new ArrayList<>();
            for (Map<?, ?> m : s.getMapList("esyalar")) {
                MarketItem it = parseItem(id, m);
                if (it != null) items.add(it);
            }
            categories.put(id, new Category(id, slot, icon, name, title, items));
        }
        plugin.getLogger().info(categories.size() + " kategori yuklendi.");
    }

    private static String val(Map<?, ?> m, String key, String def) {
        Object o = m.get(key);
        return o == null ? def : String.valueOf(o);
    }

    private static List<String> list(Map<?, ?> m, String key) {
        List<String> out = new ArrayList<>();
        Object o = m.get(key);
        if (o instanceof List<?> l) {
            for (Object x : l) out.add(String.valueOf(x));
        }
        return out;
    }

    private MarketItem parseItem(String cat, Map<?, ?> m) {
        String matName = val(m, "malzeme", "");
        Material mat = Material.matchMaterial(matName);
        if (mat == null || !mat.isItem() || mat.isAir()) {
            plugin.getLogger().warning("[" + cat + "] gecersiz malzeme: " + matName);
            return null;
        }
        double price;
        try {
            price = Double.parseDouble(val(m, "fiyat", "0"));
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("[" + cat + "] gecersiz fiyat: " + matName);
            return null;
        }
        Currency cur = val(m, "para-birimi", "PARA").toUpperCase(Locale.ROOT).startsWith("KR")
                ? Currency.KRISTAL : Currency.PARA;
        boolean buy = val(m, "islem", "SAT").toUpperCase(Locale.ROOT).startsWith("AL");
        boolean give = Boolean.parseBoolean(val(m, "esya-ver", "false"));

        EntityType spawner = null;
        String st = val(m, "spawner-turu", "");
        if (!st.isEmpty()) {
            try {
                spawner = EntityType.valueOf(st.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[" + cat + "] gecersiz spawner-turu: " + st);
            }
        }
        return new MarketItem(mat, val(m, "isim", pretty(mat)), price, cur, buy,
                list(m, "aciklama"), list(m, "komutlar"), give, spawner);
    }

    private static String pretty(Material m) {
        StringBuilder sb = new StringBuilder();
        for (String part : m.name().toLowerCase(Locale.ROOT).split("_")) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- format

    static String fmt(double v) {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(2);
        return nf.format(v);
    }

    static String money(Currency c, double v) {
        return c == Currency.KRISTAL ? fmt(v) + " Kristal" : fmt(v) + "$";
    }

    private static String priceText(MarketItem it) {
        return fmt(it.price()) + (it.currency() == Currency.KRISTAL ? " KRİSTAL" : "");
    }

    // ---------------------------------------------------------------- menus

    void openMain(Player p) {
        MarketHolder h = new MarketHolder(null);
        Inventory inv = Bukkit.createInventory(h, 27, Text.chat(mainTitle));
        h.inventory = inv;
        for (Category c : categories.values()) {
            ItemStack st = new ItemStack(c.icon());
            ItemMeta meta = st.getItemMeta();
            if (meta != null) {
                meta.displayName(Text.c("&e" + c.name()));
                st.setItemMeta(meta);
            }
            inv.setItem(c.slot(), st);
        }
        p.openInventory(inv);
    }

    void openCategory(Player p, Category c) {
        int count = Math.min(c.items().size(), 45);
        int rows = Math.max(1, (count + 8) / 9) + 1;
        int size = rows * 9;

        MarketHolder h = new MarketHolder(c);
        Inventory inv = Bukkit.createInventory(h, size, Text.chat(c.title()));
        h.inventory = inv;

        for (int i = 0; i < count; i++) {
            inv.setItem(i, display(c.items().get(i)));
        }

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta bm = back.getItemMeta();
        if (bm != null) {
            bm.displayName(Text.c(plugin.getConfig().getString("geri-butonu", "&cGeri")));
            back.setItemMeta(bm);
        }
        inv.setItem(size - 9, back);
        p.openInventory(inv);
    }

    private ItemStack display(MarketItem it) {
        ItemStack st = new ItemStack(it.material());
        ItemMeta meta = st.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.c("&f" + it.name()));
            meta.lore(lore(it));
            st.setItemMeta(meta);
        }
        return st;
    }

    private List<Component> lore(MarketItem it) {
        List<Component> l = new ArrayList<>();
        String price = priceText(it);
        if (it.buy()) {
            l.add(Text.c(""));
            for (String d : it.description()) l.add(Text.c(d));
            if (!it.description().isEmpty()) l.add(Text.c(""));
            for (String line : plugin.getConfig().getStringList("al-lore")) {
                l.add(Text.c(line.replace("{fiyat}", price)));
            }
        } else {
            for (String line : plugin.getConfig().getStringList("esya-lore")) {
                l.add(Text.c(line.replace("{fiyat}", price)));
            }
        }
        return l;
    }

    // ---------------------------------------------------------------- events

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof MarketHolder h)) return;

        // Menude hicbir esya tasinamaz / alinamaz (shift-click, sayi tuslari dahil)
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory clicked = e.getClickedInventory();
        if (clicked == null || !clicked.equals(top)) return;

        int slot = e.getSlot();

        if (h.category == null) {
            for (Category c : categories.values()) {
                if (c.slot() == slot) {
                    Bukkit.getScheduler().runTask(plugin, () -> openCategory(p, c));
                    return;
                }
            }
            return;
        }

        if (slot == top.getSize() - 9) {
            Bukkit.getScheduler().runTask(plugin, () -> openMain(p));
            return;
        }
        if (slot < 0 || slot >= h.category.items().size() || slot >= 45) return;

        MarketItem it = h.category.items().get(slot);
        ClickType t = e.getClick();

        if (it.buy()) {
            if (t == ClickType.LEFT || t == ClickType.SHIFT_LEFT) buy(p, it);
            return;
        }

        int amount;
        if (t == ClickType.MIDDLE) {
            amount = Integer.MAX_VALUE;   // envanterdeki hepsi
        } else if (t == ClickType.LEFT) {
            amount = 1;
        } else if (t == ClickType.SHIFT_LEFT) {
            amount = 64;
        } else {
            return;
        }
        sell(p, it, amount);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof MarketHolder) {
            e.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------- sell / buy

    private static boolean sellable(ItemStack s) {
        if (!s.getEnchantments().isEmpty()) return false;
        ItemMeta m = s.getItemMeta();
        return m == null || !m.hasDisplayName();
    }

    private void sell(Player p, MarketItem it, int max) {
        PlayerInventory inv = p.getInventory();
        ItemStack[] cont = inv.getStorageContents();
        int removed = 0;

        for (int i = 0; i < cont.length && removed < max; i++) {
            ItemStack s = cont[i];
            if (s == null || s.getType() != it.material() || !sellable(s)) continue;
            int take = Math.min(s.getAmount(), max - removed);
            removed += take;
            if (take == s.getAmount()) {
                cont[i] = null;
            } else {
                s.setAmount(s.getAmount() - take);
            }
        }

        if (removed == 0) {
            plugin.send(p, "esya-yok", "{esya}", it.name());
            return;
        }
        inv.setStorageContents(cont);

        double total = it.price() * removed;
        eco.give(p, it.currency(), total);
        plugin.send(p, "satildi",
                "{adet}", String.valueOf(removed),
                "{esya}", it.name(),
                "{tutar}", money(it.currency(), total));
    }

    private void buy(Player p, MarketItem it) {
        if (it.commands().isEmpty() && !it.giveItem()) {
            plugin.send(p, "ayarlanmamis");
            return;
        }
        if (eco.get(p, it.currency()) < it.price() || !eco.take(p, it.currency(), it.price())) {
            plugin.send(p, "yetersiz-bakiye", "{tutar}", money(it.currency(), it.price()));
            return;
        }
        if (it.giveItem()) giveStack(p, it);
        for (String cmd : it.commands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{oyuncu}", p.getName()));
        }
        plugin.send(p, "satin-alindi", "{esya}", it.name(), "{tutar}", money(it.currency(), it.price()));
    }

    private void giveStack(Player p, MarketItem it) {
        ItemStack st = new ItemStack(it.material());
        ItemMeta meta = st.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.c("&f" + it.name()));
            if (it.spawnerType() != null && it.material() == Material.SPAWNER
                    && meta instanceof BlockStateMeta bsm
                    && bsm.getBlockState() instanceof CreatureSpawner cs) {
                cs.setSpawnedType(it.spawnerType());
                bsm.setBlockState(cs);
            }
            st.setItemMeta(meta);
        }
        Map<Integer, ItemStack> left = p.getInventory().addItem(st);
        for (ItemStack rest : left.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), rest);
        }
    }
}
