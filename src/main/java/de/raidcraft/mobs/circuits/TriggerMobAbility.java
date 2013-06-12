package de.raidcraft.mobs.circuits;

import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.circuits.ic.AbstractIC;
import com.sk89q.craftbook.circuits.ic.AbstractICFactory;
import com.sk89q.craftbook.circuits.ic.ChipState;
import com.sk89q.craftbook.circuits.ic.IC;
import com.sk89q.craftbook.circuits.ic.ICFactory;
import com.sk89q.craftbook.circuits.ic.ICVerificationException;
import com.sk89q.craftbook.util.ICUtil;
import com.sk89q.craftbook.util.LocationUtil;
import com.sk89q.worldedit.Vector;
import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.ability.Ability;
import de.raidcraft.skills.api.ability.Useable;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.exceptions.CombatException;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * @author Silthus
 */
public class TriggerMobAbility extends AbstractIC {

    private Vector radius;
    private Location offset;
    private String ability;

    public TriggerMobAbility(Server server, ChangedSign sign, ICFactory factory) {

        super(server, sign, factory);
    }

    @Override
    public void load() {

        radius = ICUtil.parseRadius(getSign(), 2);
        offset = ICUtil.parseBlockLocation(getSign(), 2).getLocation();
        ability = getSign().getLine(3);
    }

    @Override
    public String getTitle() {

        return "Trigger Custom Mob Ability";
    }

    @Override
    public String getSignTitle() {

        return "TRIG MOB ABILI";
    }

    @Override
    public void trigger(ChipState chipState) {

        if (chipState.getInput(0)) {
            for (Entity entity : LocationUtil.getNearbyEntities(offset, radius)) {
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }
                CharacterTemplate character = RaidCraft.getComponent(CharacterManager.class).getCharacter((LivingEntity) entity);
                if (!(character instanceof Mob)) {
                    continue;
                }
                Ability<Mob> mobAbility = ((Mob) character).getAbility(ability);
                if (mobAbility == null) {
                    RaidCraft.LOGGER.warning("Unknown mob ability used on IC at: " + getSign().getX() + "," + getSign().getY() + "," + getSign().getZ());
                }
                if (mobAbility instanceof Useable) {
                    try {
                        ((Useable) mobAbility).use();
                    } catch (CombatException ignored) {
                        // ignore mob errors
                    }
                }
            }
        }
    }

    public static class TriggerMobAbilityFactory extends AbstractICFactory {

        public TriggerMobAbilityFactory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign changedSign) {

            return new TriggerMobAbility(getServer(), changedSign, this);
        }

        @Override
        public String getShortDescription() {

            return "Triggers the ability of a custom mob.";
        }

        @Override
        public String getLongDescription() {

            return "Triggers the ability of the defined custom mob in the defined area.";
        }

        @Override
        public String[] getLineHelp() {

            return new String[] {
                    "r=x:y:z",
                    "<ability>"
            };
        }

        @Override
        public void verify(ChangedSign sign) throws ICVerificationException {

            ICUtil.verifySignLocationSyntax(sign, 2);
        }
    }

}
