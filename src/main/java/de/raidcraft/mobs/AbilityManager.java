package de.raidcraft.mobs;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.Component;
import de.raidcraft.mobs.api.Ability;
import de.raidcraft.mobs.api.AbilityInformation;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.skills.util.StringUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Silthus
 */
public final class AbilityManager implements Component {

    private final MobsPlugin plugin;
    private final Map<String, Constructor<? extends Ability>> abilities = new HashMap<>();

    protected AbilityManager(MobsPlugin plugin) {

        this.plugin = plugin;
        RaidCraft.registerComponent(AbilityManager.class, this);
    }

    public void registerAbility(Class<? extends Ability> aClass) {

        if (!aClass.isAnnotationPresent(AbilityInformation.class)) {
            plugin.getLogger().warning("No ability information found for: " + aClass.getCanonicalName());
            return;
        }
        try {
            String abilityName = StringUtils.formatName(aClass.getAnnotation(AbilityInformation.class).value());
            abilities.put(abilityName, aClass.getConstructor(Mob.class, ConfigurationSection.class));
        } catch (NoSuchMethodException e) {
            plugin.getLogger().warning(e.getMessage());
        }
    }
}
