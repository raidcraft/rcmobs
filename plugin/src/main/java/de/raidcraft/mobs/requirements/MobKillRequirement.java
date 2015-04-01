package de.raidcraft.mobs.requirements;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.action.requirement.Requirement;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.hero.Hero;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * @author Silthus
 */
public class MobKillRequirement implements Requirement<Player> {

    @Override
    public boolean test(Player player, ConfigurationSection config) {

        CharacterManager component = RaidCraft.getComponent(CharacterManager.class);
        Hero hero = component.getHero(player);
        CharacterTemplate lastKill = hero.getLastKill();
        return lastKill != null
                && lastKill instanceof Mob
                && ((Mob) lastKill).getId().equals(config.getString("mob"));
    }
}
