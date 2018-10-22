package de.raidcraft.mobs.api;

import de.raidcraft.api.random.RDSTable;

public interface MobConfig {

    String getName();

    String getCustomEntityType();

    int getMinDamage();

    int getMaxDamage();

    int getMinlevel();

    int getMaxLevel();

    int getMinHealth();

    int getMaxHealth();

    boolean isResetHealth();

    boolean isElite();

    boolean isRare();

    boolean isSpawningNaturally();

    boolean isWaterMob();

    boolean isPassive();

    boolean isBaby();

    boolean isAggro();

    boolean isRanged();

    boolean isGiveExp();

    boolean isItemPickup();

    String getHurtSound();

    float getHurtSoundPitch();

    String getDeathSound();

    float getDeathSoundPitch();

    org.bukkit.configuration.ConfigurationSection getAbilities();

    org.bukkit.configuration.ConfigurationSection getEquipment();

    java.util.List<RDSTable> getLootTables();

    void addLootTable(RDSTable table);

    void removeLootTable(RDSTable table);

    void clearLootTables();

    void setName(String name);

    void setMinDamage(int minDamage);

    void setMaxDamage(int maxDamage);

    void setMinlevel(int minlevel);

    void setMaxLevel(int maxLevel);

    void setMinHealth(int minHealth);

    void setMaxHealth(int maxHealth);

    void setResetHealth(boolean resetHealth);

    void setElite(boolean elite);

    void setRare(boolean rare);

    void setSpawningNaturally(boolean spawningNaturally);

    void setWaterMob(boolean waterMob);

    void setPassive(boolean passive);

    void setBaby(boolean baby);

    void setAggro(boolean aggro);

    void setRanged(boolean ranged);

    void setGiveExp(boolean giveExp);

    void setItemPickup(boolean itemPickup);

    void setHurtSound(String hurtSound);

    void setHurtSoundPitch(float hurtSoundPitch);

    void setDeathSound(String deathSound);

    void setDeathSoundPitch(float deathSoundPitch);

    void setAbilities(org.bukkit.configuration.ConfigurationSection abilities);

    void setEquipment(org.bukkit.configuration.ConfigurationSection equipment);
}
