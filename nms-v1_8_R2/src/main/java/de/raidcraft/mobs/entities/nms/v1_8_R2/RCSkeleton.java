package de.raidcraft.mobs.entities.nms.v1_8_R2;

import de.raidcraft.mobs.api.CustomNmsEntity;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.util.ReflectionUtil;
import net.minecraft.server.v1_8_R2.EntityLiving;
import net.minecraft.server.v1_8_R2.EntityPlayer;
import net.minecraft.server.v1_8_R2.EntitySkeleton;
import net.minecraft.server.v1_8_R2.GenericAttributes;
import net.minecraft.server.v1_8_R2.PathfinderGoalArrowAttack;
import net.minecraft.server.v1_8_R2.PathfinderGoalFloat;
import net.minecraft.server.v1_8_R2.PathfinderGoalHurtByTarget;
import net.minecraft.server.v1_8_R2.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_8_R2.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_8_R2.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.v1_8_R2.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_8_R2.PathfinderGoalRandomStroll;
import net.minecraft.server.v1_8_R2.PathfinderGoalSelector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_8_R2.CraftWorld;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

/**
 * @author Silthus
 */
public class RCSkeleton extends EntitySkeleton implements CustomNmsEntity {

    private Optional<Mob> wrappedEntity = Optional.empty();
    private Location spawnLocation;
    private String deathSound;
    private String hurtSound;

    public RCSkeleton(org.bukkit.World world) {

        super(((CraftWorld) world).getHandle());

        // this will clear all default goals of this entity
        List goalB = (List) ReflectionUtil.getPrivateField("b", PathfinderGoalSelector.class, goalSelector);
        goalB.clear();
        List goalC = (List) ReflectionUtil.getPrivateField("c", PathfinderGoalSelector.class, goalSelector);
        goalC.clear();
        List targetB = (List) ReflectionUtil.getPrivateField("b", PathfinderGoalSelector.class, targetSelector);
        targetB.clear();
        List targetC = (List) ReflectionUtil.getPrivateField("c", PathfinderGoalSelector.class, targetSelector);
        targetC.clear();

        this.goalSelector.a(0, new PathfinderGoalFloat(this));
        this.goalSelector.a(6, new PathfinderGoalReturnToSpawn(this, spawnLocation, 0.8D, 25));
        this.goalSelector.a(7, new PathfinderGoalRandomStroll(this, 0.75D));
        this.goalSelector.a(8, new PathfinderGoalLookAtPlayer(this, EntityPlayer.class, 10.0F));
        this.goalSelector.a(8, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.a(1, new PathFinderGoalHighestThreatTarget(this, true));
        this.targetSelector.a(2, new PathfinderGoalHurtByTarget(this, true));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void load(ConfigurationSection config) {

        this.hurtSound = config.getString("sound.hurt");
        this.deathSound = config.getString("sound.death");
        // modify the attack radius
        this.getAttributeInstance(GenericAttributes.b).setValue(config.getDouble("aggro-range", 8.0));
        if (config.getBoolean("ranged", false)) {
            this.goalSelector.a(4, new PathfinderGoalArrowAttack(this, 1.0D, 20, 60, 15.0F));
        } else {
            this.goalSelector.a(4, new PathfinderGoalMeleeAttack(this, EntityPlayer.class, 1.2D, true));
        }
        List<String> targets = config.getStringList("targets");
        if (targets == null || targets.isEmpty()) {
            this.targetSelector.a(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityPlayer.class, true));
        } else {
            for (String target : targets) {
                Class<?> nmsClass = ReflectionUtil.getNmsClass("net.minecraft.server", target);
                if (nmsClass != null && EntityLiving.class.isAssignableFrom(nmsClass)) {
                    this.targetSelector.a(3, new PathfinderGoalNearestAttackableTarget<>(this, (Class<? extends EntityLiving>) nmsClass, true));
                }
            }
        }
    }

    // sound hurt
    @Override
    protected String bo() {

        if (hurtSound == null) {
            return super.bo();
        }
        return hurtSound;
    }

    // sound death
    @Override
    protected String bp() {

        if (deathSound == null) {
            return super.bp();
        }
        return deathSound;
    }

    @Override
    public void setWrappedEntity(Mob mob) {

        this.wrappedEntity = Optional.of(mob);
    }

    @Override
    public Optional<Mob> getWrappedEntity() {

        return wrappedEntity;
    }

    @Override
    public LivingEntity spawn(Location location) {

        this.spawnLocation = location;
        setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        ((CraftWorld) location.getWorld()).getHandle().addEntity(this);
        return (LivingEntity) Bukkit.getWorld(location.getWorld().getUID()).getEntities().stream()
                .filter(e -> e.getUniqueId().equals(getUniqueID()))
                .findFirst().get();
    }
}
