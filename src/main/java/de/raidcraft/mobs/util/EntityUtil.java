package de.raidcraft.mobs.util;

import de.raidcraft.RaidCraft;
import net.minecraft.server.v1_5_R3.EntityCreature;
import net.minecraft.server.v1_5_R3.EntityLiving;
import net.minecraft.server.v1_5_R3.PathfinderGoalPanic;
import net.minecraft.server.v1_5_R3.PathfinderGoalSelector;
import org.bukkit.craftbukkit.v1_5_R3.entity.CraftCreature;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;

import java.lang.reflect.Field;

/**
 * @author Silthus
 */
public class EntityUtil {

    public static void addPanicMode(LivingEntity entity) {

        if (entity instanceof Creature) {
            try {
                EntityCreature handle = ((CraftCreature) entity).getHandle();
                PathfinderGoalPanic goal = new PathfinderGoalPanic(handle, 0.38F);
                Field field = EntityLiving.class.getField("goalSelector");
                field.setAccessible(true);
                PathfinderGoalSelector selector = (PathfinderGoalSelector) field.get(handle);
                selector.a(1, goal);
                field.set(handle, selector);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                RaidCraft.LOGGER.warning(e.getMessage());
            }
        }
    }
}
