package de.raidcraft.mobs.api;

import de.raidcraft.skills.api.character.AbstractSkilledCharacter;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.character.CharacterType;
import de.raidcraft.skills.api.combat.ThreatTable;
import de.raidcraft.skills.api.effect.common.Combat;
import de.raidcraft.skills.api.exceptions.CombatException;
import de.raidcraft.skills.creature.CreatureAttachedLevel;
import de.raidcraft.util.LocationUtil;
import de.raidcraft.util.MathUtil;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

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

        return getHighestThreat();
    }

    @Override
    public CharacterTemplate getHighestThreat() throws CombatException {

        ThreatTable.ThreatLevel highestThreat = getThreatTable().getHighestThreat();
        if (highestThreat == null) {
            return null;
        }
        return highestThreat.getTarget();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CharacterTemplate> getInvolvedTargets() {

        Combat combat = getLastCombat();
        if (combat == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(combat.getInvolvedCharacters());
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