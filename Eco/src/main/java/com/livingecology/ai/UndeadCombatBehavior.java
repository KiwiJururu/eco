package com.livingecology.ai;

import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.data.StateType;
import com.livingecology.environment.EnvironmentManager;
import com.livingecology.environment.EnvironmentSnapshot;
import com.livingecology.territory.TerritoryContext;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import com.livingecology.territory.TerritoryZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Combat-safe memory/context overlay for Skeleton, Stray, Wither Skeleton and Zombified Piglin. */
public final class UndeadCombatBehavior {
    private static final int MAX_MEMORY_RECEIVERS = 8;
    private static final double MEMORY_RANGE = 12.0D;
    private static final double MELEE_RECOVERY_MIN_DISTANCE_SQR = 16.0D;

    private UndeadCombatBehavior() {}

    public static void tick(Mob mob, ServerLevel level) {
        SpeciesType species = SpeciesType.from(mob).orElse(null);
        if (species == null) return;
        UndeadCombatSpeciesPolicy policy = UndeadCombatSpeciesPolicy.of(species).orElse(null);
        if (policy == null) return;

        BehaviorUtil.sanitizeCombatTarget(mob, level);
        SpeciesProfile profile = SpeciesProfile.of(species);
        TerritoryRecord territory = TerritoryManager.ensureTerritory(mob, level);
        TerritoryContext context = TerritoryManager.contextForMob(mob, level);
        EnvironmentSnapshot environment = EnvironmentManager.snapshot(level, mob.blockPosition(), species);
        if (environment.habitability() < 20) MobMindData.addState(mob, StateType.STRESS, 1);
        if (context.contested()) MobMindData.addState(mob, StateType.RAGE, 1);
        MobMindData.setResting(mob, false);

        LivingEntity target = mob.getTarget();
        if (BehaviorUtil.isValidCombatTarget(target)) {
            MobMindData.rememberThreat(mob, target, 3, level);
            shareBoundedCombatMemory(mob, level, policy, target);
            recoverMeleeNavigation(mob, level, policy, target);
            return;
        }

        if (mob instanceof ZombifiedPiglin piglin
                && (piglin.getRemainingPersistentAngerTime() > 0
                || piglin.getPersistentAngerTarget() != null)) {
            MobMindData.addState(mob, StateType.RAGE, 1);
            return;
        }

        if (hasDaylightPriority(mob, level, policy)) return;
        returnHomeIfFarOutside(mob, level, profile, territory, context, 0.86D);
    }

    private static void shareBoundedCombatMemory(Mob source, ServerLevel level,
                                                 UndeadCombatSpeciesPolicy sourcePolicy,
                                                 LivingEntity target) {
        int informed = 0;
        for (Mob ally : level.getEntitiesOfClass(Mob.class,
                new AABB(source.blockPosition()).inflate(MEMORY_RANGE), other -> {
                    if (other == source || !other.isAlive()) return false;
                    SpeciesType otherSpecies = SpeciesType.from(other).orElse(null);
                    return otherSpecies != null && UndeadCombatSpeciesPolicy.of(otherSpecies)
                            .map(policy -> policy.knowledgeGroup() == sourcePolicy.knowledgeGroup())
                            .orElse(false);
                })) {
            if (informed >= MAX_MEMORY_RECEIVERS) break;
            if (!ally.hasLineOfSight(source) && ally.distanceToSqr(source) > 36.0D) continue;
            MobMindData.initialize(ally, level);
            MobMindData.rememberThreat(ally, target, 5, level);
            MobMindData.addState(ally, StateType.RAGE, 1);
            informed++;
        }
    }

    /** Returns true only when a melee recovery path was actually started. */
    public static boolean recoverMeleeNavigation(Mob mob, ServerLevel level,
                                                 UndeadCombatSpeciesPolicy policy,
                                                 LivingEntity target) {
        if (!BehaviorUtil.isValidCombatTarget(target)) return false;
        boolean ranged = usesRangedPositioning(mob, policy);
        boolean navigationDone = mob.getNavigation().isDone();
        boolean stalled = !navigationDone && MobMindData.movementStalled(mob, level, 35, 0.45D);
        if (!shouldAttemptMeleeRecovery(ranged, mob.onGround(), mob.isPassenger(), mob.isLeashed(),
                mob.distanceToSqr(target), navigationDone, stalled)) return false;

        if (stalled) mob.getNavigation().stop();
        boolean ordered = mob.getNavigation().moveTo(target, 1.0D);
        return ordered && !mob.getNavigation().isDone();
    }

    public static boolean usesRangedPositioning(Mob mob, UndeadCombatSpeciesPolicy policy) {
        return policy.combatMode() == UndeadCombatSpeciesPolicy.CombatMode.WEAPON_AWARE_SKELETON
                && mob instanceof AbstractSkeleton
                && mob.getMainHandItem().getItem() instanceof BowItem;
    }

    public static boolean shouldAttemptMeleeRecovery(boolean ranged, boolean grounded,
                                                     boolean passenger, boolean leashed,
                                                     double targetDistanceSqr,
                                                     boolean navigationDone, boolean stalled) {
        return !ranged && grounded && !passenger && !leashed
                && targetDistanceSqr > MELEE_RECOVERY_MIN_DISTANCE_SQR
                && (navigationDone || stalled);
    }

    public static boolean daylightPriority(SpeciesType species, boolean day, boolean skyVisible,
                                           boolean hasHeadCover) {
        return UndeadCombatSpeciesPolicy.of(species)
                .map(policy -> policy.daylightSensitive() && day && skyVisible && !hasHeadCover)
                .orElse(false);
    }

    private static boolean hasDaylightPriority(Mob mob, ServerLevel level,
                                               UndeadCombatSpeciesPolicy policy) {
        return policy.daylightSensitive() && level.isDay()
                && level.canSeeSky(mob.blockPosition())
                && mob.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty();
    }

    private static void returnHomeIfFarOutside(Mob mob, ServerLevel level, SpeciesProfile profile,
                                               TerritoryRecord territory, TerritoryContext context,
                                               double speed) {
        if (territory == null || context.ownZone() != TerritoryZone.OUTSIDE
                || !mob.getNavigation().isDone()) return;
        Vec3 core = Vec3.atCenterOf(territory.core());
        if (mob.distanceToSqr(core) <= 32.0D * 32.0D) return;
        BlockPos corePos = BlockPos.containing(core);
        if (level.hasChunkAt(corePos) && BehaviorUtil.isSafeLand(level, corePos)) {
            mob.getNavigation().moveTo(core.x, core.y, core.z, speed);
            return;
        }
        BehaviorUtil.safePatrolPoint(mob, level, profile, core, 6.0D)
                .ifPresent(target -> mob.getNavigation().moveTo(target.x, target.y, target.z, speed));
    }
}
