package de.raidcraft.mobs.api;

import de.raidcraft.skills.api.character.AbstractSkilledCharacter;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.character.CharacterType;
import de.raidcraft.skills.api.combat.ThreatTable;
import de.raidcraft.skills.api.exceptions.CombatException;
import de.raidcraft.skills.creature.CreatureAttachedLevel;
import de.raidcraft.util.LocationUtil;
import de.raidcraft.util.MathUtil;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

/**
 * @author Silthus
 */
public abstract class AbstractMob extends AbstractSkilledCharacter<Mob> implements Mob {

    private String id;

    public AbstractMob(LivingEntity entity) {

        super(entity);
        attachLevel(new CreatureAttachedLevel<CharacterTemplate>(this, 60));
    }

    @Override
    public String getId() {

        return id;
    }

    @Override
    public void setId(String id) {

        this.id = id;
    }

    @Override
    public CharacterType getCharacterType() {

        return CharacterType.CUSTOM_MOB;
    }

    @Override
    public CharacterTemplate getTarget() throws CombatException {

        Optional<CharacterTemplate> highestThreat = getHighestThreat();
        return highestThreat.isPresent() ? highestThreat.get() : null;
    }

    @Override
    public Optional<CharacterTemplate> getHighestThreat() {

        ThreatTable threatTable = getThreatTable();
        if (threatTable == null) return Optional.empty();
        ThreatTable.ThreatLevel highestThreat = threatTable.getHighestThreat();
        if (highestThreat == null) return Optional.empty();
        return Optional.ofNullable(highestThreat.getTarget());
    }

    @Override
    public CharacterTemplate getRandomTarget() {

        List<CharacterTemplate> targets = getInvolvedTargets();
        if (targets.isEmpty()) return null;
        return targets.get(MathUtil.RANDOM.nextInt(targets.size()));
    }

    @Override
    public CharacterTemplate getNearestTarget() {

        CharacterTemplate nearestTarget = null;
        double range = 0.0;
        for (CharacterTemplate target : getInvolvedTargets()) {
            double distance = LocationUtil.getDistance(target.getEntity().getLocation(), getEntity().getLocation());
            if (range == 0.0 || distance < range) {
                range = distance;
                nearestTarget = target;
            }
        }
        return nearestTarget;
    }

    @Override
    public CharacterTemplate getFarthestTarget() {

        CharacterTemplate farthestTarget = null;
        double range = 0.0;
        for (CharacterTemplate target : getInvolvedTargets()) {
            double distance = LocationUtil.getDistance(target.getEntity().getLocation(), getEntity().getLocation());
            if (range == 0.0 || distance > range) {
                range = distance;
                farthestTarget = target;
            }
        }
        return farthestTarget;
    }

    @Override
    public double getDefaultHealth() {

        return getMaxHealth();
    }
}
