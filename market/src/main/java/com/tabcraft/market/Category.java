package com.tabcraft.market;

import org.bukkit.Material;

import java.util.List;

public record Category(String id, int slot, Material icon, String name, String title, List<MarketItem> items) {}
