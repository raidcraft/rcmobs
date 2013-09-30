package de.raidcraft.mobs.tables;

import com.avaje.ebean.validation.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Silthus
 */
@Entity
@Table(name = "rcmobs_mob_spawn_location")
public class MobSpawnLocation {

    @Id
    private int id;
    @NotNull
    private String mob;
    private int x;
    private int y;
    private int z;
    private String world;
    private double cooldown;

    public int getId() {

        return id;
    }

    public void setId(int id) {

        this.id = id;
    }

    public String getMob() {

        return mob;
    }

    public void setMob(String mob) {

        this.mob = mob;
    }

    public int getX() {

        return x;
    }

    public void setX(int x) {

        this.x = x;
    }

    public int getY() {

        return y;
    }

    public void setY(int y) {

        this.y = y;
    }

    public int getZ() {

        return z;
    }

    public void setZ(int z) {

        this.z = z;
    }

    public String getWorld() {

        return world;
    }

    public void setWorld(String world) {

        this.world = world;
    }

    public double getCooldown() {

        return cooldown;
    }

    public void setCooldown(double cooldown) {

        this.cooldown = cooldown;
    }
}
