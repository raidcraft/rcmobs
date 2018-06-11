package de.raidcraft.mobs.nms.v1_12_R1;

import de.raidcraft.mobs.api.CustomNmsEntity;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.util.ReflectionUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

/**
 * @author Silthus
 */
@Getter
@Setter
public class RCZombie extends EntityZombie implements CustomNmsEntity {

    private Optional<Mob> wrappedEntity = Optional.empty();
    private Location spawnLocation;
    private String deathSound;
    private String hurtSound;

    public RCZombie(org.bukkit.World world) {

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
