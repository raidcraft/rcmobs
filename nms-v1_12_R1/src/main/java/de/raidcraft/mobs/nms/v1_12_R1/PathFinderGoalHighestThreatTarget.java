package de.raidcraft.mobs.nms.v1_12_R1;

import de.raidcraft.mobs.api.CustomNmsEntity;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.skills.api.character.CharacterTemplate;
import net.minecraft.server.v1_12_R1.EntityCreature;
import net.minecraft.server.v1_12_R1.EntityLiving;
import net.minecraft.server.v1_12_R1.PathfinderGoalTarget;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftLivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;

import java.util.Optional;

/**
 * @author mdoering
 */
public class PathFinderGoalHighestThreatTarget extends PathfinderGoalTarget {

    private final CustomNmsEntity entity;
    private EntityLiving currentTarget;

    public PathFinderGoalHighestThreatTarget(CustomNmsEntity entityCreature, boolean b) {

        super((EntityCreature) entityCreature, b);
        this.entity = entityCreature;
    }

    // shouldStart() the onStart() method and update the target
    @Override
    public boolean a() {

        if (!entity.getWrappedEntity().isPresent()) {
            return false;
        }
        Mob mob = entity.getWrappedEntity().get();
        Optional<CharacterTemplate> threat = mob.getHighestThreat();
        if (!threat.isPresent()) return false;
        currentTarget = ((CraftLivingEntity) threat.get().getEntity()).getHandle();
        return true;
    }

    // onStart() set the given target
    @Override
    public void c() {

        this.e.setGoalTarget(currentTarget, EntityTargetEvent.TargetReason.CUSTOM, true);
        super.c();
    }
}
