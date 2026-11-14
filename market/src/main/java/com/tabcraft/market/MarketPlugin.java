package com.tabcraft.market;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class MarketPlugin extends JavaPlugin implements CommandExecutor {

    private Eco eco;
    private MarketMenus menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        eco = new Eco(this);
        menus = new MarketMenus(this, eco);
        menus.load();
        getServer().getPluginManager().registerEvents(menus, this);

        getCommand("market").setExecutor(this);
        getCommand("bakiye").setExecutor(this);
        getCommand("marketadmin").setExecutor(this);
        getLogger().info("Market aktif.");
    }

    // ---------------------------------------------------------------- mesajlar

    void send(CommandSender to, String key, String... kv) {
        String msg = getConfig().getString("mesajlar." + key, key);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            msg = msg.replace(kv[i], kv[i + 1]);
        }
        to.sendMessage(Text.chat(getConfig().getString("mesajlar.prefix", "") + msg));
    }

    // ---------------------------------------------------------------- komutlar

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);

        if (name.equals("market")) {
            if (!(sender instanceof Player p)) {
                send(sender, "sadece-oyuncu");
                return true;
            }
            menus.openMain(p);
            return true;
        }

        if (name.equals("bakiye")) {
            if (!(sender instanceof Player p)) {
                send(sender, "sadece-oyuncu");
                return true;
            }
            send(p, "bakiye",
                    "{para}", MarketMenus.fmt(eco.get(p, Currency.PARA)),
                    "{kristal}", MarketMenus.fmt(eco.get(p, Currency.KRISTAL)));
            return true;
        }

        // /marketadmin
        if (!sender.hasPermission("market.admin")) {
            send(sender, "yetki-yok");
            return true;
        }
        if (a.length == 1 && a[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            menus.load();
            send(sender, "yenilendi");
            return true;
        }
        if (a.length != 4) {
            send(sender, "kullanim");
            return true;
        }

        Currency cur;
        if (a[0].equalsIgnoreCase("para")) {
            cur = Currency.PARA;
        } else if (a[0].toLowerCase(Locale.ROOT).startsWith("kr")) {
            cur = Currency.KRISTAL;
        } else {
            send(sender, "kullanim");
            return true;
        }

        Player target = Bukkit.getPlayerExact(a[2]);
        if (target == null) {
            send(sender, "oyuncu-yok", "{oyuncu}", a[2]);
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(a[3]);
        } catch (NumberFormatException e) {
            send(sender, "kullanim");
            return true;
        }
        if (amount < 0) {
            send(sender, "kullanim");
            return true;
        }

        String op = a[1].toLowerCase(Locale.ROOT);
        switch (op) {
            case "ver" -> eco.give(target, cur, amount);
            case "al" -> eco.take(target, cur, amount);
            case "ayarla" -> eco.set(target, cur, amount);
            default -> {
                send(sender, "kullanim");
                return true;
            }
        }
        send(sender, "admin-tamam",
                "{oyuncu}", target.getName(),
                "{bakiye}", MarketMenus.money(cur, eco.get(target, cur)));
        return true;
    }
}
