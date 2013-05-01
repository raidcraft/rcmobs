package de.raidcraft.mobs;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.Component;
import de.raidcraft.mobs.api.Ability;
import de.raidcraft.mobs.api.AbilityInformation;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.api.UnknownAbilityException;
import de.raidcraft.skills.util.StringUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

    public Ability getAbility(String name, Mob mob, ConfigurationSection config) throws UnknownAbilityException {

        try {
            name = StringUtils.formatName(name);
            if (!abilities.containsKey(name)) {
                throw new UnknownAbilityException(mob.getName() + " tried to load unregistered ability " + name);
            }
            Constructor<? extends Ability> constructor = abilities.get(name);
            constructor.setAccessible(true);
            return constructor.newInstance(mob, config);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new UnknownAbilityException(e.getMessage());
        }
    }
}
