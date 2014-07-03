package de.raidcraft.mobs.requirements;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.action.requirement.Requirement;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.hero.Hero;
import org.bukkit.entity.Player;

/**
 * @author Silthus
 */
public class MobKillRequirement implements Requirement<Player> {

    @Override
    public boolean test(Player player) {

        CharacterManager component = RaidCraft.getComponent(CharacterManager.class);
        Hero hero = component.getHero(player);
        CharacterTemplate lastKill = hero.getLastKill();
        return lastKill != null
                && lastKill.getEntity().hasMetadata("RC_MOB_ID")
                && lastKill.getEntity().getMetadata("RC_MOB_ID").stream()
                .anyMatch(value -> value.asString().equalsIgnoreCase(getConfig().getString("mob")));
    }
}
