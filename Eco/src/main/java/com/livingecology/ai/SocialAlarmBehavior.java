package com.livingecology.ai;

import com.livingecology.data.AttributeType;
import com.livingecology.data.BehaviorFamily;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.data.StateType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

import java.util.List;
import java.util.Optional;

/**
 * Local social warning for passive herds and explicitly audited aquatic/flying species.
 *
 * This is deliberately not a global group brain: the receiver must be close enough to perceive a
 * same-species groupmate, and the reported threat must still be within a plausible extended
 * perception radius. Confidence decays as information is relayed, so a warning may cross a few
 * nearby animals without becoming permanent territory-wide telepathy.
 */
public final class SocialAlarmBehavior {
    private SocialAlarmBehavior() {}

    public static void tick(Mob mob, ServerLevel level, SpeciesProfile profile) {
        Optional<PassiveLandSpeciesPolicy> passiveLand = PassiveLandSpeciesPolicy.of(profile.species());
        boolean passiveHerd = profile.behaviorFamily() == BehaviorFamily.PASSIVE_HERD
                && passiveLand.isEmpty();
        boolean auditedAquatic = AquaticSpeciesPolicy.of(profile.species())
                .map(AquaticSpeciesPolicy::acceptsSocialAlarm).orElse(false);
        boolean auditedFlying = FlyingColonySpeciesPolicy.of(profile.species())
                .map(FlyingColonySpeciesPolicy::acceptsSocialAlarm).orElse(false);
        boolean auditedPassiveLand = passiveLand.map(PassiveLandSpeciesPolicy::acceptsSocialAlarm)
                .orElse(false);
        if (!passiveHerd && !auditedAquatic && !auditedFlying && !auditedPassiveLand) return;
        if (mob instanceof TamableAnimal tame && tame.isTame()) return;
        if (mob instanceof AbstractHorse horse && !(horse instanceof Camel) && horse.isTamed()) return;
        if (MobMindData.resolveThreat(mob, level).isPresent()) return;

        int social = MobMindData.getAttribute(mob, AttributeType.SOCIABILITY);
        if (social < 45) return;

        double signalRange = 4.0D + social * 0.10D;
        double perceptionRange = BehaviorUtil.perceptionRange(mob,
                MobMindData.getAttribute(mob, AttributeType.PERCEPTION));
        List<Mob> herd = BehaviorUtil.nearbySameSpecies(mob, level, signalRange);

        int inspected = 0;
        for (Mob ally : herd) {
            if (inspected++ >= 12) break;
            if (!mob.hasLineOfSight(ally)) continue;

            LivingEntity threat = legalThreatFrom(ally, level);
            if (threat == null) continue;
            if (SpeciesType.from(threat).orElse(null) == profile.species()) continue;
            if (mob.distanceTo(threat) > perceptionRange * 1.70D) continue;

            // A current target is a strong alarm. Memory-only reports need a meaningful score so weak,
            // stale impressions are not amplified indefinitely through a herd.
            boolean directAlarm = ally.getTarget() == threat;
            int sourceScore = MobMindData.threatScore(ally);
            if (!directAlarm && sourceScore < 20) continue;

            // Each relay loses confidence. Direct alarms start strong; remembered reports are roughly
            // halved on each hop, eventually falling below the 20-point threshold required to relay again.
            int effectiveSource = directAlarm ? Math.max(60, sourceScore) : sourceScore;
            int strength = Math.max(6, Math.min(45, effectiveSource / 2 + social / 20));
            MobMindData.rememberThreat(mob, threat, strength, level);
            MobMindData.addState(mob, StateType.FEAR, 1);
            MobMindData.setResting(mob, false);
            return;
        }
    }

    private static LivingEntity legalThreatFrom(Mob ally, ServerLevel level) {
        LivingEntity target = ally.getTarget();
        if (BehaviorUtil.isValidCombatTarget(target)) return target;
        Optional<LivingEntity> remembered = MobMindData.resolveThreat(ally, level);
        return remembered.filter(BehaviorUtil::isValidCombatTarget).orElse(null);
    }
}
