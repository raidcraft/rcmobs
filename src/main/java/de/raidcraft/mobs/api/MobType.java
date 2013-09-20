package de.raidcraft.mobs.api;

import de.raidcraft.util.EnumUtils;
import org.bukkit.ChatColor;

/**
 * @author Silthus
 */
public enum MobType {

    WEAK(ChatColor.GRAY),
    COMMON(ChatColor.WHITE),
    STRONG(ChatColor.GREEN),
    TOUGH(ChatColor.GOLD),
    ELITE(ChatColor.DARK_BLUE),
    HEROIC(ChatColor.RED),
    BOSS(ChatColor.DARK_RED);

    private final ChatColor nameColor;

    private MobType(ChatColor nameColor) {

        this.nameColor = nameColor;
    }

    public ChatColor getNameColor() {

        return nameColor;
    }

    public static MobType fromString(String name) {

        return EnumUtils.getEnumFromString(MobType.class, name);
    }
}
