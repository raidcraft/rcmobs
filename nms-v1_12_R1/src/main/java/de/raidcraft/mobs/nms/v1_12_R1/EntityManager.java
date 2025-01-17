package de.raidcraft.mobs.nms.v1_12_R1;

import de.raidcraft.mobs.api.CustomNmsEntity;
import de.raidcraft.mobs.api.MobConfig;
import de.raidcraft.mobs.api.NmsEntityManager;
import de.raidcraft.util.ReflectionUtil;
import net.minecraft.server.v1_12_R1.*;

import java.util.List;

/**
 * @author mdoering
 */
public class EntityManager implements NmsEntityManager {

    @Override
    @SuppressWarnings("unchecked")
    public void loadEntity(CustomNmsEntity entity, MobConfig config) {

        entity.setDeathSound(config.getDeathSound());
        entity.setHurtSound(config.getHurtSound());
        if (entity instanceof EntityCreature) {
            // lets modify the aggro range
            ((EntityLiving) entity).getAttributeInstance(GenericAttributes.FOLLOW_RANGE).setValue(config.getAggroRange());
            // get the goal and target selector to add and modify goals
            PathfinderGoalSelector goalSelector = (PathfinderGoalSelector) ReflectionUtil.getPrivateField("goalSelector", EntityInsentient.class, entity);
            PathfinderGoalSelector targetSelector = (PathfinderGoalSelector) ReflectionUtil.getPrivateField("targetSelector", EntityInsentient.class, entity);
            // set some default targets or as configured
            EntityCreature creature = (EntityCreature) entity;
            List<String> targets = config.getTargets();
            if (targets == null || targets.isEmpty()) {
                targetSelector.a(3, new PathfinderGoalNearestAttackableTarget<>(creature, EntityPlayer.class, true));
            } else {
                for (String target : targets) {
                    Class<? extends EntityLiving> nmsClass = (Class<? extends EntityLiving>) ReflectionUtil.getNmsClass("net.minecraft.server", target);
                    if (nmsClass != null && EntityLiving.class.isAssignableFrom(nmsClass)) {
                        if (!config.isRanged()) {
                            goalSelector.a(4, new PathfinderGoalMeleeAttack(creature, 1.2D, true));
                        }
                        targetSelector.a(3, new PathfinderGoalNearestAttackableTarget<>(creature, (Class<? extends EntityLiving>) nmsClass, true));
                    }
                }
            }
        }
        entity.load(config);
    }
}
