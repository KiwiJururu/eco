package com.livingecology.ai;

import com.livingecology.data.AttributeType;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesType;
import com.livingecology.data.StateType;
import com.livingecology.territory.TerritoryContext;
import com.livingecology.territory.TerritoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public final class ZombieBehavior {
    private ZombieBehavior() {}

    public static void tick(Mob raw, ServerLevel level) {
        if (!(raw instanceof Zombie zombie)) return;
        TerritoryManager.ensureTerritory(zombie, level);
        TerritoryContext context = TerritoryManager.contextForMob(zombie, level);
        int perception = MobMindData.getAttribute(zombie, AttributeType.PERCEPTION);
        int social = MobMindData.getAttribute(zombie, AttributeType.SOCIABILITY);
        double range = BehaviorUtil.perceptionRange(zombie, perception);
        int adaptation = MobMindData.calculateAdaptation(zombie, level);

        LivingEntity target = zombie.getTarget();
        if (target == null || !target.isAlive()) target = null;

        List<Mob> horde = BehaviorUtil.nearbySameSpecies(zombie, level,
                10.0D + social * 0.10D + (MobMindData.isBoss(zombie) ? 10.0D : 0.0D));

        if (target == null) {
            for (Mob ally : horde) {
                LivingEntity shared = ally.getTarget();
                if (shared != null && shared.isAlive()) {
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

        // Spider <-> Zombie symbiosis: respond to an ally's attacker.
        if (target == null) {
            for (Mob spider : BehaviorUtil.species(level, zombie.blockPosition(), SpeciesType.SPIDER, range)) {
                Optional<LivingEntity> allyThreat = MobMindData.resolveThreat(spider, level);
                if (allyThreat.isPresent()) {
                    LivingEntity threat = allyThreat.get();
                    if ((SpeciesType.from(threat).orElse(null) == SpeciesType.WOLF || threat instanceof Player)
                            && zombie.distanceTo(threat) <= range * 1.3D) {
                        target = threat;
                        TerritoryManager.recordCooperation(zombie, spider, level, 1);
                        break;
                    }
                }
            }
        }

        // Wolf <-> Zombie aggressive war: wolves become a priority in contact/contested zones.
        if (target == null) {
            Optional<Mob> wolf = BehaviorUtil.nearestSpecies(zombie, level, SpeciesType.WOLF, range);
            if (wolf.isPresent()) {
                boolean rememberedWolf = MobMindData.resolveThreat(zombie, level)
                        .filter(t -> t.getUUID().equals(wolf.get().getUUID())).isPresent();
                if (rememberedWolf || context.contested() || context.tension() >= 65 || horde.size() >= 2) {
                    target = wolf.get();
                }
            }
        }

        if (target != null) {
            zombie.setTarget(target);
            MobMindData.rememberThreat(zombie, target, 4, level);
            MobMindData.setResting(zombie, false);

            int relay = MobMindData.isBoss(zombie) ? 10 : Math.max(2, social / 15);
            int sent = 0;
            for (Mob ally : horde) {
                if (sent++ >= relay) break;
                MobMindData.rememberThreat(ally, target, MobMindData.isBoss(zombie) ? 15 : 7, level);
                if (ally.getTarget() == null && ally.hasLineOfSight(target)) ally.setTarget(target);
            }
            avoidKnownCobwebIfUseful(zombie, level, adaptation);
            return;
        }

        Optional<BlockPos> lastPos = MobMindData.lastThreatPos(zombie);
        long memoryAge = level.getGameTime() - MobMindData.lastThreatTime(zombie);
        long memoryWindow = 300L + MobMindData.getAttribute(zombie, AttributeType.MEMORY) * 16L;
        if (lastPos.isPresent() && memoryAge >= 0L && memoryAge <= memoryWindow) {
            BlockPos p = lastPos.get();
            zombie.getNavigation().moveTo(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D, 1.0D);
            avoidKnownCobwebIfUseful(zombie, level, adaptation);
        }
    }

    private static void avoidKnownCobwebIfUseful(Zombie zombie, ServerLevel level, int adaptation) {
        if (adaptation < 45) return;
        Vec3 look = zombie.getLookAngle();
        BlockPos ahead = BlockPos.containing(zombie.position().add(look.x * 1.2D, 0.0D, look.z * 1.2D));
        boolean web = level.getBlockState(zombie.blockPosition()).is(Blocks.COBWEB)
                || level.getBlockState(ahead).is(Blocks.COBWEB);
        if (!web) return;

        Vec3 sideA = new Vec3(-look.z, 0, look.x).normalize().scale(2.5D);
        Vec3 sideB = sideA.scale(-1.0D);
        BlockPos a = BlockPos.containing(zombie.position().add(sideA));
        BlockPos b = BlockPos.containing(zombie.position().add(sideB));
        if (!level.getBlockState(a).is(Blocks.COBWEB) && level.getBlockState(a).isAir()) {
            zombie.getNavigation().moveTo(a.getX() + 0.5D, a.getY(), a.getZ() + 0.5D, 1.05D);
        } else if (!level.getBlockState(b).is(Blocks.COBWEB) && level.getBlockState(b).isAir()) {
            zombie.getNavigation().moveTo(b.getX() + 0.5D, b.getY(), b.getZ() + 0.5D, 1.05D);
        }
    }
}
