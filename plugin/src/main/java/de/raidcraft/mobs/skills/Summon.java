package de.raidcraft.mobs.skills;

import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.util.StringUtil;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.action.requirement.Requirement;
import de.raidcraft.api.action.requirement.RequirementException;
import de.raidcraft.api.action.RequirementFactory;
import de.raidcraft.api.action.requirement.RequirementResolver;
import de.raidcraft.api.random.RDSTable;
import de.raidcraft.mobs.api.AbstractMob;
import de.raidcraft.mobs.util.CustomMobUtil;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.SkillsPlugin;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.combat.EffectType;
import de.raidcraft.skills.api.exceptions.CombatException;
import de.raidcraft.skills.api.hero.Hero;
import de.raidcraft.skills.api.persistance.SkillProperties;
import de.raidcraft.skills.api.profession.Profession;
import de.raidcraft.skills.api.resource.Resource;
import de.raidcraft.skills.api.skill.AbstractLevelableSkill;
import de.raidcraft.skills.api.skill.SkillInformation;
import de.raidcraft.skills.api.trigger.CommandTriggered;
import de.raidcraft.skills.effects.Summoned;
import de.raidcraft.skills.tables.THeroSkill;
import de.raidcraft.skills.util.ConfigUtil;
import de.raidcraft.skills.util.StringUtils;
import de.raidcraft.util.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Silthus
 */
@SkillInformation(
        name = "Summon",
        description = "Beschwört eine Kreatur die für den Beschwörer kämpft.",
        types = {EffectType.MAGICAL, EffectType.SILENCABLE, EffectType.SUMMON}
)
public class Summon extends AbstractLevelableSkill implements CommandTriggered {

    private static CharacterManager CHARACTER_MANAGER;

    private final Map<String, SummonedCreatureConfig> creatureConfigs = new HashMap<>();
    private final Map<EntityType, List<CharacterTemplate>> summonedCreatures = new HashMap<>();
    private String resource;

    public Summon(Hero hero, SkillProperties data, Profession profession, THeroSkill database) {

        super(hero, data, profession, database);
    }

    @Override
    public void load(ConfigurationSection data) {

        resource = data.getString("resource", "souls");
        ConfigurationSection creatures = data.getConfigurationSection("creatures");
        if (creatures == null) return;
        for (String key : creatures.getKeys(false)) {
            try {
                SummonedCreatureConfig config = new SummonedCreatureConfig(key, data.getConfigurationSection("creatures." + key), this);
                creatureConfigs.put(key, config);
            } catch (InvalidConfigurationException e) {
                RaidCraft.LOGGER.warning(e.getMessage());
            }
        }
    }

    @Override
    public void onLevelGain() {

        super.onLevelGain();
        boolean match = true;
        for (SummonedCreatureConfig config : creatureConfigs.values()) {
            for (Requirement<Player> requirement : config.requirements) {
                if (!requirement.test(holder.getPlayer())) {
                    match = false;
                    break;
                }
            }
            if (match) {
                getHolder().sendMessage(ChatColor.GREEN + "Du kannst eine neue Kreatur beschwören: " + config.getFriendlyName());
            }
        }
    }

    @Override
    public void runCommand(CommandContext args) throws CombatException {

        if (args.argsLength() < 1) {
            throw new CombatException(
                    "Du musst mindestens eine Kreatur zum beschwören angeben: /cast " + getFriendlyName() + " <anzahl> <kreatur>\n" +
                            StringUtil.joinString(creatureConfigs.keySet(), ", ", 0)
            );
        }

        SummonedCreatureConfig config = findMatchingCreature(args.getString(0));

        if (!config.isMeetingAllRequirements(holder.getPlayer())) {
            throw new CombatException(config.getResolveReason(holder.getPlayer()));
        }

        int amount = args.getInteger(1, 1);
        int maxAmount = config.getAmount();
        if (amount > maxAmount) {
            amount = maxAmount;
            getHolder().sendMessage(ChatColor.RED + "Du kannst maximal " + maxAmount + " " + config.getFriendlyName() + " beschwören.");
        }

        for (CharacterTemplate summoned : summonCreatures(config, amount)) {
            EntityType type = summoned.getEntity().getType();
            if (!summonedCreatures.containsKey(type)) {
                summonedCreatures.put(type, new ArrayList<>());
            }
            if (maxAmount < summonedCreatures.get(type).size() && !summonedCreatures.get(type).isEmpty()) {
                // we need to kill the first one
                removeEffect(summonedCreatures.get(type).get(0), Summoned.class);
            }
            summonedCreatures.get(type).add(summoned);
        }
    }

    private SummonedCreatureConfig findMatchingCreature(String name) throws CombatException {

        name = StringUtils.formatName(name);

        List<String> foundConfigs = new ArrayList<>();
        for (SummonedCreatureConfig config : creatureConfigs.values()) {
            if (config.name.startsWith(name) || StringUtils.formatName(config.getFriendlyName()).startsWith(name)) {
                foundConfigs.add(config.name);
            }
        }

        if (foundConfigs.size() > 1) {
            throw new CombatException("Es gibt mehrere beschwörbare Kreaturen mit dem Namen " + name + ":\n" +
                    StringUtil.joinString(foundConfigs, ", ", 0));
        }
        if (foundConfigs.size() < 1) {
            throw new CombatException("Du kennst keine beschwörbaren Kreaturen mit dem Namen: " + name);
        }
        return creatureConfigs.get(foundConfigs.get(0));
    }

    public List<CharacterTemplate> summonCreatures(SummonedCreatureConfig config, int amount) throws CombatException {

        Resource resource = getHolder().getResource(this.resource);
        if (CHARACTER_MANAGER == null) {
            CHARACTER_MANAGER = RaidCraft.getComponent(SkillsPlugin.class).getCharacterManager();
        }
        // lets check resources first
        if (resource != null) {
            double value = ConfigUtil.getTotalValue(this, config.resourceCost) * amount;
            double newValue = resource.getCurrent() - value;
            if (newValue < resource.getDefault()) {
                throw new CombatException("Nicht genügend " + resource.getFriendlyName()
                        + ". Es werden " + value + " " + resource.getFriendlyName() + " benötigt.");
            }
            resource.setCurrent(newValue);
        }

        List<CharacterTemplate> summonedCreatures = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            SummonedCreature creature;
            Location targetBlock = getTargetBlock();
            if (config.customEntityName != null) {
                Optional<SummonedCreature> mob = CustomMobUtil.spawnNMSEntity(config.customEntityName, targetBlock, SummonedCreature.class, config.config);
                if (mob.isPresent()) {
                    creature = mob.get();
                } else {
                    RaidCraft.LOGGER.warning("failed to summon entity: " + config.customEntityName
                            + " in " + de.raidcraft.util.ConfigUtil.getFileName(config.config));
                    continue;
                }
            } else {
                creature = CHARACTER_MANAGER.spawnCharacter(
                        config.entityType,
                        targetBlock.add(0, 1, 0),
                        SummonedCreature.class,
                        config
                );
            }
            // add the summoned effect that will kill the creature when the summon ends
            addEffect(creature, Summoned.class);
            // also add some exp to the skill and display the combatlog message
            getAttachedLevel().addExp(config.expForSummon);
            summonedCreatures.add(creature);
            getHolder().combatLog(this, config.getFriendlyName() + " mit " + creature.getMaxHealth() + " Leben " +
                    "und " + creature.getDamage() + " Schaden beschworen.");
        }
        return summonedCreatures;
    }

    public static class SummonedCreature extends AbstractMob {

        public SummonedCreature(LivingEntity entity, SummonedCreatureConfig config) {

            super(entity);
            usingHealthBar = false;
            setMaxHealth(config.getMaxHealth());
            setHealth(getMaxHealth());
            setDamage(config.getDamage());
            setName("Kreatur von " + config.skill.getHolder().getName());
            // set a bow if its a skeleton
            // TODO: switch over to custom mobs for this
            if (getEntity().getType() == EntityType.SKELETON) {
                getEntity().getEquipment().setItemInHand(new ItemStack(Material.BOW));
            }
        }

        @Override
        public Location getSpawnLocation() {

            return getEntity().getLocation();
        }

        @Override
        public boolean isRare() {

            return false;
        }

        @Override
        public boolean isElite() {

            return false;
        }

        @Override
        public boolean isSpawningNaturally() {

            return false;
        }

        @Override
        public boolean isWaterMob() {

            return false;
        }

        @Override
        public Optional<RDSTable> getLootTable() {

            return Optional.empty();
        }
    }

    public class SummonedCreatureConfig implements RequirementResolver<Player> {

        private final String name;
        private final Summon skill;
        private String friendlyName;
        private EntityType entityType;
        private String customEntityName;
        private int expForSummon;
        private List<Requirement<Player>> requirements = new ArrayList<>();
        private ConfigurationSection resourceCost;
        private ConfigurationSection amount;
        private ConfigurationSection minDamage;
        private ConfigurationSection maxDamage;
        private ConfigurationSection minHealth;
        private ConfigurationSection maxHealth;
        private ConfigurationSection config;

        public SummonedCreatureConfig(String name, ConfigurationSection config, Summon skill) throws InvalidConfigurationException {

            this.name = name;
            this.skill = skill;
            this.config = config;
            load(config);
        }

        private void load(final ConfigurationSection config) throws InvalidConfigurationException {

            this.friendlyName = config.getString("name", name);
            this.resourceCost = config.getConfigurationSection("resource-cost");

            this.entityType = EntityType.valueOf(config.getString("type"));
            this.customEntityName = config.getString("custom-type");
            if (entityType == null || customEntityName == null) {
                throw new InvalidConfigurationException("No Entity with the type " + config.getString("type") + " found!");
            }

            expForSummon = config.getInt("exp", 0);

            Bukkit.getScheduler().runTaskLater(RaidCraft.getComponent(SkillsPlugin.class), () -> {

                try {
                    requirements.addAll(RequirementFactory.getInstance().createRequirements(getName(),
                            config.getConfigurationSection("requirements"),
                            Player.class
                    ));
                } catch (RequirementException e) {
                    RaidCraft.LOGGER.warning(e.getMessage() + " in " + de.raidcraft.util.ConfigUtil.getFileName(config));
                    requirements.clear();
                }
            }, 1L);

            amount = config.getConfigurationSection("amount");

            minDamage = config.getConfigurationSection("min-damage");
            maxDamage = config.getConfigurationSection("max-damage");
            if (maxDamage == null) maxDamage = minDamage;

            minHealth = config.getConfigurationSection("min-health");
            maxHealth = config.getConfigurationSection("max-health");
            if (maxHealth == null) maxHealth = minHealth;
        }

        public int getAmount() {

            return (int) ConfigUtil.getTotalValue(skill, amount);
        }

        public int getDamage() {

            int minDamage = (int) ConfigUtil.getTotalValue(skill, this.minDamage);
            int maxDamage = (int) ConfigUtil.getTotalValue(skill, this.maxDamage);
            return MathUtil.RANDOM.nextInt(maxDamage - minDamage + 1) + minDamage;
        }

        public int getMaxHealth() {

            int minHealth = (int) ConfigUtil.getTotalValue(skill, this.minHealth);
            int maxHealth = (int) ConfigUtil.getTotalValue(skill, this.maxHealth);
            return MathUtil.RANDOM.nextInt(maxHealth - minHealth + 1) + minHealth;
        }

        @Override
        public List<Requirement<Player>> getRequirements() {

            return requirements;
        }

        public String getFriendlyName() {

            return friendlyName;
        }
    }
}
