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

import java.util.List;
import java.util.Optional;

/**
 * Local social warning for passive herd species.
 *
 * This is deliberately not a global group brain: the receiver must be close enough to perceive a
 * same-species groupmate, and the reported threat must still be within a plausible extended
 * perception radius. Information can therefore travel through a herd over time without becoming
 * instantaneous telepathy across a territory.
 */
public final class SocialAlarmBehavior {
    private SocialAlarmBehavior() {}

    public static void tick(Mob mob, ServerLevel level, SpeciesProfile profile) {
        if (profile.behaviorFamily() != BehaviorFamily.PASSIVE_HERD) return;
        if (MobMindData.resolveThreat(mob, level).isPresent()) return;

        int social = MobMindData.getAttribute(mob, AttributeType.SOCIABILITY);
        if (social < 45) return;

        double signalRange = 4.0D + social * 0.10D;
        double perceptionRange = BehaviorUtil.perceptionRange(mob,
                MobMindData.getAttribute(mob, AttributeType.PERCEPTION));
        List<Mob> herd = BehaviorUtil.nearbySameSpecies(mob, level, signalRange);

        for (Mob ally : herd) {
            if (!mob.hasLineOfSight(ally)) continue;

            LivingEntity threat = legalThreatFrom(ally, level);
            if (threat == null) continue;
            if (SpeciesType.from(threat).orElse(null) == profile.species()) continue;
            if (mob.distanceTo(threat) > perceptionRange * 1.70D) continue;

            // A current target is a strong alarm. Memory-only reports need a meaningful score so weak,
            // stale impressions are not amplified indefinitely through a herd.
            boolean directAlarm = ally.getTarget() == threat;
            if (!directAlarm && MobMindData.threatScore(ally) < 20) continue;

            int strength = 4 + social / 20;
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
