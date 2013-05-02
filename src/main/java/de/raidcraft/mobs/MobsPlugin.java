package de.raidcraft.mobs;

import de.raidcraft.api.BasePlugin;
import de.raidcraft.mobs.commands.MobCommands;

/**
 * @author Silthus
 */
public class MobsPlugin extends BasePlugin {

    private MobManager mobManager;

    @Override
    public void enable() {

        registerCommands(MobCommands.class);
        this.mobManager = new MobManager(this);
    }

    @Override
    public void disable() {


    }

    public void reload() {

        this.mobManager.reload();
    }

    public MobManager getMobManager() {

        return mobManager;
    }
}
