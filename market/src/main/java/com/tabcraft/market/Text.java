package com.tabcraft.market;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

final class Text {
    private static final LegacyComponentSerializer LS = LegacyComponentSerializer.legacyAmpersand();

    private Text() {}

    /** Esya adi / lore icin: italik kapali, & renk kodlari acik. */
    static Component c(String s) {
        return LS.deserialize(s).decoration(TextDecoration.ITALIC, false);
    }

    /** Sohbet mesaji / baslik icin. */
    static Component chat(String s) {
        return LS.deserialize(s);
    }
}
