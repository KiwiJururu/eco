package com.livingecology.ai;

import com.livingecology.data.AttributeType;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.territory.TerritoryContext;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Dedicated ecological controller shared by vanilla Zombie subclasses. */
public final class ZombieBehavior {
    private ZombieBehavior() {}

    public static void tick(Mob raw, ServerLevel level) {
        if (!(raw instanceof Zombie zombie)) return;
        SpeciesType species = SpeciesType.from(zombie).orElse(SpeciesType.ZOMBIE);
        SpeciesProfile profile = SpeciesProfile.of(species);
        TerritoryRecord territory = TerritoryManager.ensureTerritory(zombie, level);
        TerritoryContext context = TerritoryManager.contextForMob(zombie, level);
        int perception = MobMindData.getAttribute(zombie, AttributeType.PERCEPTION);
        int social = MobMindData.getAttribute(zombie, AttributeType.SOCIABILITY);
        double range = BehaviorUtil.perceptionRange(zombie, perception);
        int adaptation = MobMindData.calculateAdaptation(zombie, level);

        LivingEntity vanillaTarget = zombie.getTarget();
        if (!BehaviorUtil.isValidCombatTarget(vanillaTarget)) vanillaTarget = null;

        // Zombie variants are one social/ecological family. A Husk, Drowned and Zombie Villager may
        // exchange target knowledge with normal Zombies instead of forming isolated micro-hordes.
        List<Mob> horde = nearbyZombieFamily(zombie, level,
                10.0D + social * 0.10D + (MobMindData.isBoss(zombie) ? 10.0D : 0.0D));

        LivingEntity target = null;

        // 1) Wolf <-> Zombie-family war has ecological priority over a vanilla player fixation when
        // the horde has enough confidence or local history to engage.
        Optional<Mob> wolf = BehaviorUtil.nearestSpecies(zombie, level, SpeciesType.WOLF, range);
        if (wolf.isPresent()) {
            Mob candidate = wolf.get();
            boolean rememberedWolf = MobMindData.resolveThreat(zombie, level)
                    .filter(t -> t.getUUID().equals(candidate.getUUID())).isPresent();
            if (rememberedWolf || context.contested() || context.tension() >= 55 || horde.size() >= 2) {
                target = candidate;
            }
        }

        // 2) Spider <-> Zombie-family symbiosis: respond to an ally's real attacker.
        if (target == null) {
            for (Mob spider : level.getEntitiesOfClass(Mob.class, zombie.getBoundingBox().inflate(range), m -> {
                SpeciesType type = SpeciesType.from(m).orElse(null);
                return type == SpeciesType.SPIDER || type == SpeciesType.CAVE_SPIDER;
            })) {
                Optional<LivingEntity> allyThreat = MobMindData.resolveThreat(spider, level);
                if (allyThreat.isPresent()) {
                    LivingEntity threat = allyThreat.get();
                    if (BehaviorUtil.isValidCombatTarget(threat)
                            && (SpeciesType.from(threat).orElse(null) == SpeciesType.WOLF
                            || threat instanceof net.minecraft.world.entity.player.Player)
                            && zombie.distanceTo(threat) <= range * 1.3D) {
                        target = threat;
                        TerritoryManager.recordCooperation(zombie, spider, level, 1);
                        break;
                    }
                }
            }
        }

        // 3) Horde knowledge. Mixed zombie variants share legal targets and remembered threats.
        if (target == null) {
            for (Mob ally : horde) {
                LivingEntity shared = ally.getTarget();
                if (BehaviorUtil.isValidCombatTarget(shared)) {
                    target = shared;
                    break;
                }
                Optional<LivingEntity> remembered = MobMindData.resolveThreat(ally, level);
                if (remembered.isPresent() && zombie.distanceTo(remembered.get()) <= range * 1.4D) {
                    target = remembered.get();
                    break;
                }
            }
        }

        // 4) Finally preserve a valid vanilla target (normally a Survival/Adventure player or vanilla prey).
        if (target == null) target = vanillaTarget;

        if (target != null) {
            zombie.setTarget(target);
            MobMindData.rememberThreat(zombie, target, 4, level);

            int relay = MobMindData.isBoss(zombie) ? 10 : Math.max(2, social / 15);
            int sent = 0;
            for (Mob ally : horde) {
                if (sent++ >= relay) break;
                MobMindData.initialize(ally, level);
                MobMindData.rememberThreat(ally, target, MobMindData.isBoss(zombie) ? 15 : 7, level);
                LivingEntity allyTarget = ally.getTarget();
                if (!BehaviorUtil.isValidCombatTarget(allyTarget) && ally.hasLineOfSight(target)) {
                    ally.setTarget(target);
                }
            }

            boolean webDetour = avoidKnownCobwebIfUseful(zombie, level, adaptation);
            boolean stalled = MobMindData.movementStalled(zombie, level, 30, 0.55D);
            if (!webDetour && zombie.distanceToSqr(target) > 6.25D
                    && (zombie.getNavigation().isDone() || stalled)) {
                // Recalculate with a small individual offset so a dense horde does not all request the same node.
                double angle = (zombie.getId() * 2.399963229728653D) % (Math.PI * 2.0D);
                double spread = Math.min(3.5D, Math.max(0.0D, horde.size() - 1) * 0.35D);
                Vec3 approach = target.position().add(Math.cos(angle) * spread, 0.0D, Math.sin(angle) * spread);
                if (stalled) zombie.getNavigation().stop();
                zombie.getNavigation().moveTo(approach.x, approach.y, approach.z, 1.05D);
            }
            return;
        }

        Optional<BlockPos> lastPos = MobMindData.lastThreatPos(zombie);
        long memoryAge = level.getGameTime() - MobMindData.lastThreatTime(zombie);
        long memoryWindow = 300L + MobMindData.getAttribute(zombie, AttributeType.MEMORY) * 16L;
        if (lastPos.isPresent() && MobMindData.threatScore(zombie) > 0
                && memoryAge >= 0L && memoryAge <= memoryWindow) {
            BlockPos p = lastPos.get();
            boolean stalled = MobMindData.movementStalled(zombie, level, 30, 0.55D);
            if (zombie.getNavigation().isDone() || stalled) {
                if (stalled) zombie.getNavigation().stop();
                zombie.getNavigation().moveTo(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D, 1.0D);
            }
            avoidKnownCobwebIfUseful(zombie, level, adaptation);
            return;
        }

        // Horde drift keeps an idle group alive and mobile even when vanilla random-stroll timing lines up poorly.
        // Use the species movement domain so Drowned patrol can remain amphibious instead of demanding dry land.
        if (horde.size() >= 2 && zombie.getNavigation().isDone() && MobMindData.patrolDue(zombie, level)) {
            Vec3 center = territory == null ? zombie.position() : Vec3.atCenterOf(territory.core());
            Optional<Vec3> drift = BehaviorUtil.safePatrolPoint(zombie, level, profile, center, 12.0D);
            if (drift.isPresent()) {
                Vec3 p = drift.get();
                zombie.getNavigation().moveTo(p.x, p.y, p.z, 0.85D);
                MobMindData.scheduleNextPatrol(zombie, level, 80, 180);
            } else {
                MobMindData.scheduleNextPatrol(zombie, level, 40, 90);
            }
        }
    }

    private static List<Mob> nearbyZombieFamily(Zombie zombie, ServerLevel level, double radius) {
        return level.getEntitiesOfClass(Mob.class, zombie.getBoundingBox().inflate(radius), other -> {
            if (other == zombie || !other.isAlive()) return false;
            SpeciesType type = SpeciesType.from(other).orElse(null);
            return isZombieFamily(type);
        });
    }

    private static boolean isZombieFamily(SpeciesType type) {
        return type == SpeciesType.ZOMBIE || type == SpeciesType.ZOMBIE_VILLAGER
                || type == SpeciesType.HUSK || type == SpeciesType.DROWNED || type == SpeciesType.GIANT;
    }

    private static boolean avoidKnownCobwebIfUseful(Zombie zombie, ServerLevel level, int adaptation) {
        if (adaptation < 45) return false;
        Vec3 look = zombie.getLookAngle();
        BlockPos ahead = BlockPos.containing(zombie.position().add(look.x * 1.2D, 0.0D, look.z * 1.2D));
        boolean web = level.getBlockState(zombie.blockPosition()).is(Blocks.COBWEB)
                || level.getBlockState(ahead).is(Blocks.COBWEB);
        if (!web) return false;

        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 0.001D) return false;
        horizontalLook = horizontalLook.normalize();
        Vec3 sideA = new Vec3(-horizontalLook.z, 0, horizontalLook.x).scale(2.5D);
        Vec3 sideB = sideA.scale(-1.0D);
        BlockPos a = BlockPos.containing(zombie.position().add(sideA));
        BlockPos b = BlockPos.containing(zombie.position().add(sideB));
        if (level.hasChunkAt(a) && BehaviorUtil.isSafeLand(level, a) && !level.getBlockState(a).is(Blocks.COBWEB)) {
            zombie.getNavigation().moveTo(a.getX() + 0.5D, a.getY(), a.getZ() + 0.5D, 1.05D);
            return true;
        } else if (level.hasChunkAt(b) && BehaviorUtil.isSafeLand(level, b) && !level.getBlockState(b).is(Blocks.COBWEB)) {
            zombie.getNavigation().moveTo(b.getX() + 0.5D, b.getY(), b.getZ() + 0.5D, 1.05D);
            return true;
        }
        return false;
    }
}
