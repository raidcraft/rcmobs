package de.raidcraft.mobs.circuits;

import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.circuits.ic.AbstractIC;
import com.sk89q.craftbook.circuits.ic.AbstractICFactory;
import com.sk89q.craftbook.circuits.ic.ChipState;
import com.sk89q.craftbook.circuits.ic.IC;
import com.sk89q.craftbook.circuits.ic.ICFactory;
import com.sk89q.craftbook.circuits.ic.ICVerificationException;
import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.MobsPlugin;
import de.raidcraft.mobs.SpawnableMob;
import de.raidcraft.mobs.UnknownMobException;
import de.raidcraft.util.LocationUtil;
import org.bukkit.Server;
import org.bukkit.block.BlockFace;

/**
 * @author Silthus
 */
public class SpawnCustomCreature extends AbstractIC {

    private SpawnableMob spawnableMob;

    public SpawnCustomCreature(Server server, ChangedSign sign, ICFactory factory) {

        super(server, sign, factory);
    }

    @Override
    public void load() {

        try {
            this.spawnableMob = RaidCraft.getComponent(MobsPlugin.class).getMobManager().getSpwanableMob(getSign().getLine(2));
        } catch (UnknownMobException ignored) {
            // this should have already been checked by the verify ic
        }
    }

    @Override
    public String getTitle() {

        return "Spawn Custom Entity";
    }

    @Override
    public String getSignTitle() {

        return "SPAWN CUSTOM ENTITY";
    }

    @Override
    public void trigger(ChipState chipState) {

        if (chipState.getInput(0)) {
            // spawn the custom creature
            spawnableMob.spawn(LocationUtil.getNextFreeSpace(getBackBlock(), BlockFace.UP).getLocation());
        }
    }

    public static class SpawnCustomCreatureFactory extends AbstractICFactory {

        public SpawnCustomCreatureFactory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign changedSign) {

            return new SpawnCustomCreature(getServer(), changedSign, this);
        }

        @Override
        public String getShortDescription() {

            return "Spawns custom mob.";
        }

        @Override
        public String getLongDescription() {

            return "Spawns the defined custom mob out of the RCMobs Plugin";
        }

        @Override
        public String[] getLineHelp() {

            return new String[] {
                "<mob name>",
                "[amount]"
            };
        }

        @Override
        public void verify(ChangedSign sign) throws ICVerificationException {

            try {
                Integer.parseInt(sign.getLine(3));
                // check if the creature exists
                RaidCraft.getComponent(MobsPlugin.class).getMobManager().getSpwanableMob(sign.getLine(2));
            } catch (NumberFormatException e) {
                throw new ICVerificationException("Line 4 needs to be a number (mob amount).");
            } catch (UnknownMobException e) {
                throw new ICVerificationException(e.getMessage());
            }
        }
    }
}
