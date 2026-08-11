package com.livingecology.ai;

import com.livingecology.data.AttributeType;
import com.livingecology.data.MobMindData;
import com.livingecology.data.MovementDomain;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.data.StateType;
import com.livingecology.environment.EnvironmentManager;
import com.livingecology.environment.EnvironmentSnapshot;
import com.livingecology.territory.TerritoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Conservative controller for issue #7's aquatic and amphibious species.
 *
 * <p>Vanilla remains authoritative for schooling-fish leadership, puffer inflation, dolphin air,
 * Axolotl/Frog/Tadpole brains, Turtle home/egg travel and Squid movement goals. Living Ecology
 * adds bounded local threat response and idle cohesion only where the ownership audit says that
 * an overlay is safe.</p>
 */
public final class AquaticBehavior {
    private static final int MAX_GROUP_SAMPLE = 12;

    private AquaticBehavior() {}

    public static void tick(Mob mob, ServerLevel level) {
        SpeciesType species = SpeciesType.from(mob).orElse(null);
        if (species == null) return;
        AquaticSpeciesPolicy policy = AquaticSpeciesPolicy.of(species).orElse(null);
        if (policy == null) return;

        SpeciesProfile profile = SpeciesProfile.of(species);
        TerritoryManager.ensureTerritory(mob, level);
        EnvironmentSnapshot environment = EnvironmentManager.snapshot(level, mob.blockPosition(), species);
        updateHabitatState(mob, profile, environment);

        if (mob.isPassenger() || mob.isLeashed()) return;
        LivingEntity threat = perceivedThreat(mob, level);

        switch (policy.overlayMode()) {
            case VANILLA_SCHOOL_THREAT_ONLY -> schoolFish(mob, level, profile, threat);
            case PUFFER_DEFENSE_ONLY -> preservePufferDefense(mob);
            case SQUID_VECTOR_COHESION -> squid((Squid) mob, level, profile, threat);
            case IDLE_NAVIGATION_COHESION -> dolphin((Dolphin) mob, level, profile, threat);
            case BRAIN_IDLE_NAVIGATION_COHESION -> brainOwnedSwimmer(mob, level, profile, threat);
            case VANILLA_OBSERVATION_ONLY -> {
                // Turtle home/egg travel and Frog jump/tongue/spawn activities own movement.
            }
        }
    }

    private static void schoolFish(Mob mob, ServerLevel level, SpeciesProfile profile, LivingEntity threat) {
        if (threat == null || MobMindData.getState(mob, StateType.FEAR) < 2) return;
        orderEscape(mob, level, profile, threat, true);
    }

    private static void preservePufferDefense(Mob mob) {
        if (mob instanceof Pufferfish puffer && puffer.getPuffState() > 0) {
            // Puff/deflate timing and scary-mob detection are vanilla gameplay-critical.
            MobMindData.setResting(puffer, false);
        }
    }

    private static void squid(Squid squid, ServerLevel level, SpeciesProfile profile, LivingEntity threat) {
        if (!squid.isInWaterOrBubble()) return;
        if (threat != null && MobMindData.getState(squid, StateType.FEAR) >= 1) {
            // A hurt Squid already owns an ink-and-flee vector. Do not replace that response.
            if (MobMindData.getState(squid, StateType.PAIN) > 0 && squid.hasMovementVector()) return;
            setSquidVector(squid, squid.position().subtract(threat.position()), 0.22F);
            return;
        }
        if (squid.hasMovementVector()) return;
        groupCenter(squid, level, profile).filter(center -> squid.distanceToSqr(center) > 9.0D)
                .ifPresent(center -> setSquidVector(squid, center.subtract(squid.position()), 0.11F));
    }

    private static void dolphin(Dolphin dolphin, ServerLevel level, SpeciesProfile profile, LivingEntity threat) {
        if (!dolphin.isInWaterOrBubble() || dolphin.gotFish() || !dolphin.getMainHandItem().isEmpty()) return;
        if (dolphin.getAirSupply() <= dolphin.getMaxAirSupply() / 3
                || dolphin.getMoistnessLevel() <= 400) return;

        if (threat != null && MobMindData.getState(dolphin, StateType.FEAR) >= 2) {
            boolean directStrongAlarm = dolphin.hasLineOfSight(threat) && MobMindData.threatScore(dolphin) >= 45;
            if (dolphin.getNavigation().isDone() || directStrongAlarm) {
                orderEscape(dolphin, level, profile, threat, true);
            }
            return;
        }
        if (dolphin.getNavigation().isDone()) orderIdleCohesion(dolphin, level, profile, 0.92D);
    }

    private static void brainOwnedSwimmer(Mob mob, ServerLevel level, SpeciesProfile profile,
                                          LivingEntity threat) {
        if (mob instanceof Axolotl axolotl && axolotl.isPlayingDead()) return;
        if ((mob instanceof Animal animal && animal.isInLove())
                || hasCriticalBrainTask(mob) || !mob.getNavigation().isDone()) return;

        if (threat != null && MobMindData.getState(mob, StateType.FEAR) >= 2) {
            orderEscape(mob, level, profile, threat, false);
            return;
        }
        orderIdleCohesion(mob, level, profile, 0.84D);
    }

    private static boolean hasCriticalBrainTask(Mob mob) {
        return mob.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)
                || mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                || mob.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET)
                || mob.getBrain().hasMemoryValue(MemoryModuleType.TEMPTING_PLAYER);
    }

    private static void orderIdleCohesion(Mob mob, ServerLevel level, SpeciesProfile profile, double speed) {
        Optional<Vec3> center = groupCenter(mob, level, profile);
        if (center.isEmpty() || mob.distanceToSqr(center.get()) <= 16.0D) return;
        BlockPos targetPos = BlockPos.containing(center.get());
        if (!level.hasChunkAt(targetPos)
                || !BehaviorUtil.isValidForDomain(level, targetPos, profile.movementDomain())) return;
        mob.getNavigation().moveTo(center.get().x, center.get().y, center.get().z, speed);
        MobMindData.setResting(mob, false);
    }

    private static void orderEscape(Mob mob, ServerLevel level, SpeciesProfile profile,
                                    LivingEntity threat, boolean mayInterruptVanillaPath) {
        if (!mayInterruptVanillaPath && !mob.getNavigation().isDone()) return;
        Vec3 socialBias = groupCenter(mob, level, profile).orElse(null);
        Vec3 target = BehaviorUtil.targetAwayForDomain(mob, level, profile, threat.position(),
                7.0D + MobMindData.getState(mob, StateType.FEAR), socialBias);
        if (target.distanceToSqr(mob.position()) < 1.0D) return;
        mob.getNavigation().moveTo(target.x, target.y, target.z, 1.08D);
        MobMindData.setResting(mob, false);
    }

    private static Optional<Vec3> groupCenter(Mob mob, ServerLevel level, SpeciesProfile profile) {
        int social = MobMindData.getAttribute(mob, AttributeType.SOCIABILITY);
        if (social < 40) return Optional.empty();
        double radius = 5.0D + social * 0.08D;
        List<Mob> nearby = BehaviorUtil.nearbySameSpecies(mob, level, radius);
        ArrayList<LivingEntity> sample = new ArrayList<>();
        sample.add(mob);
        boolean mobInWater = mob.isInWaterOrBubble();
        for (Mob other : nearby) {
            if (sample.size() >= MAX_GROUP_SAMPLE) break;
            if (profile.movementDomain() == MovementDomain.WATER && !other.isInWaterOrBubble()) continue;
            if (profile.movementDomain() == MovementDomain.AMPHIBIOUS
                    && other.isInWaterOrBubble() != mobInWater) continue;
            sample.add(other);
        }
        if (sample.size() < 2) return Optional.empty();
        return Optional.of(BehaviorUtil.centroid(sample, mob.position()));
    }

    private static LivingEntity perceivedThreat(Mob mob, ServerLevel level) {
        Optional<LivingEntity> remembered = MobMindData.resolveThreat(mob, level);
        if (remembered.isEmpty()) return null;
        LivingEntity threat = remembered.get();
        double range = BehaviorUtil.perceptionRange(mob,
                MobMindData.getAttribute(mob, AttributeType.PERCEPTION));
        if (mob.distanceTo(threat) > range * 1.5D) return null;
        return mob.hasLineOfSight(threat) || MobMindData.threatScore(mob) >= 45 ? threat : null;
    }

    private static void setSquidVector(Squid squid, Vec3 direction, float speed) {
        if (direction.lengthSqr() < 0.01D) return;
        Vec3 normalized = direction.normalize();
        squid.setMovementVector((float) normalized.x * speed,
                (float) normalized.y * speed * 0.55F, (float) normalized.z * speed);
        MobMindData.setResting(squid, false);
    }

    private static void updateHabitatState(Mob mob, SpeciesProfile profile,
                                           EnvironmentSnapshot environment) {
        if (environment.habitability() < 25) MobMindData.addState(mob, StateType.STRESS, 1);
        if (profile.movementDomain() == MovementDomain.WATER && !mob.isInWaterOrBubble()) {
            MobMindData.addState(mob, StateType.STRESS, 1);
        }
    }
}
