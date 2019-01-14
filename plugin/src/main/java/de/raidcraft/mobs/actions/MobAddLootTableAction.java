package de.raidcraft.mobs.actions;

import de.faldoria.loot.itemsintegration.ItemsIntegrationComponent;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.action.action.Action;
import de.raidcraft.api.random.RDSTable;
import de.raidcraft.mobs.MobManager;
import de.raidcraft.mobs.SpawnableMob;
import de.raidcraft.mobs.UnknownMobException;
import de.raidcraft.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;

public class MobAddLootTableAction implements Action {

    @Information(
            value = "mob.add.loottable",
            aliases = {"mob.add.loottable"},
            desc = "Adds the given loot-table to the mob.",
            conf = {
                    "mob: name of the mob",
                    "loot-table: loot-table to add to the mob config"
            }
    )
    @Override
    public void accept(Object type, ConfigurationSection config) {

        try {
            SpawnableMob mob = RaidCraft.getComponent(MobManager.class).getSpwanableMob(config.getString("mob"));
            RDSTable lootTable = RaidCraft.getComponent(ItemsIntegrationComponent.class)
                    .getLevelDependantLootTable(config.getString("loot-table"), mob.getConfig().getMinlevel());
            if (lootTable != null) {
                mob.getConfig().addLootTable(lootTable);
            } else {
                RaidCraft.LOGGER.warning("Invalid loot-table " + config.getString("loot-table") + " in config: " + ConfigUtil.getFileName(config));
            }
        } catch (UnknownMobException e) {
            RaidCraft.LOGGER.warning(e.getMessage());
        }
    }
}
