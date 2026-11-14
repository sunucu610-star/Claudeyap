package com.tabcraft.market;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.List;

/**
 * buy = false -> sunucu senden satin alir (SAT): oyuncu esyasini satar, para kazanir.
 * buy = true  -> oyuncu satin alir (AL): para/kristal oder.
 */
public record MarketItem(
        Material material,
        String name,
        double price,
        Currency currency,
        boolean buy,
        List<String> description,
        List<String> commands,
        boolean giveItem,
        EntityType spawnerType) {}
