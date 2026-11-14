package com.tabcraft.market;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * PARA: Vault + bir ekonomi plugini (EssentialsX vb.) varsa onu kullanir, yoksa bakiyeler.yml.
 * KRISTAL: her zaman bakiyeler.yml.
 */
final class Eco {
    private final MarketPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    private Object vault;
    private Method vDeposit;
    private Method vWithdraw;
    private Method vBalance;

    Eco(MarketPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bakiyeler.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    private boolean vault() {
        if (vault != null) return true;
        Plugin v = plugin.getServer().getPluginManager().getPlugin("Vault");
        if (v == null || !v.isEnabled()) return false;
        try {
            Class<?> c = Class.forName("net.milkbowl.vault.economy.Economy", true, v.getClass().getClassLoader());
            RegisteredServiceProvider<?> rsp = plugin.getServer().getServicesManager().getRegistration(c);
            if (rsp == null) return false;
            Object provider = rsp.getProvider();
            vDeposit = c.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            vWithdraw = c.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            vBalance = c.getMethod("getBalance", OfflinePlayer.class);
            vault = provider;
            plugin.getLogger().info("Vault ekonomisi bulundu, PARA birimi Vault uzerinden calisacak.");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private String path(OfflinePlayer p, Currency c) {
        return c.name().toLowerCase() + "." + p.getUniqueId();
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("bakiyeler.yml kaydedilemedi: " + e.getMessage());
        }
    }

    double get(OfflinePlayer p, Currency c) {
        if (c == Currency.PARA && vault()) {
            try {
                return ((Number) vBalance.invoke(vault, p)).doubleValue();
            } catch (Throwable t) {
                plugin.getLogger().warning("Vault bakiye okunamadi: " + t.getMessage());
            }
        }
        return data.getDouble(path(p, c), 0.0);
    }

    void give(OfflinePlayer p, Currency c, double amount) {
        if (amount <= 0) return;
        if (c == Currency.PARA && vault()) {
            try {
                vDeposit.invoke(vault, p, amount);
                return;
            } catch (Throwable t) {
                plugin.getLogger().warning("Vault para eklenemedi: " + t.getMessage());
            }
        }
        data.set(path(p, c), data.getDouble(path(p, c), 0.0) + amount);
        save();
    }

    boolean take(OfflinePlayer p, Currency c, double amount) {
        if (amount <= 0) return true;
        if (get(p, c) < amount) return false;
        if (c == Currency.PARA && vault()) {
            try {
                vWithdraw.invoke(vault, p, amount);
                return true;
            } catch (Throwable t) {
                plugin.getLogger().warning("Vault para alinamadi: " + t.getMessage());
                return false;
            }
        }
        data.set(path(p, c), data.getDouble(path(p, c), 0.0) - amount);
        save();
        return true;
    }

    void set(OfflinePlayer p, Currency c, double amount) {
        double cur = get(p, c);
        if (amount > cur) {
            give(p, c, amount - cur);
        } else if (amount < cur) {
            take(p, c, cur - amount);
        }
    }
}
