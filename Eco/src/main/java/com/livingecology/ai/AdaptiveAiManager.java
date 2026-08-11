package com.livingecology.ai;

import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.territory.TerritoryManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

import java.util.HashSet;
import java.util.Set;

/** Active-world dispatcher. Unloaded ecology is handled abstractly by territory/environment timestamps. */
public final class AdaptiveAiManager {
    private AdaptiveAiManager() {}

    public static void tickLevel(ServerLevel level) {
        if (level.players().isEmpty()) return;
        long now = level.getGameTime();
        if ((now & 3L) != 0L) return; // broad scan at 5 Hz; individual brains are additionally staggered
        long brainStep = now / 4L;
        Set<Integer> handled = new HashSet<>();

        level.players().forEach(player -> {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(68.0D), MobMindData::supports)) {
                if (!handled.add(mob.getId())) continue;
                MobMindData.initialize(mob, level);
                MobMindData.decayStates(mob, level);
                BehaviorUtil.sanitizeCombatTarget(mob, level);

                if (mob.getTarget() != null) MobMindData.setResting(mob, false);
                if (MobMindData.isResting(mob)) mob.getNavigation().stop();

                int interval = Math.max(2, MobMindData.thinkInterval(mob) / 3);
                if (Math.floorMod(brainStep + mob.getId(), interval) != 0L) continue;

                SpeciesType species = SpeciesType.from(mob).orElse(null);
                if (species == null) continue;
                SpeciesProfile profile = SpeciesProfile.of(species);

                // Short local communication pass before the species controller. This lets passive herd
                // animals react to information from visible nearby groupmates without a global shared brain.
                SocialAlarmBehavior.tick(mob, level, profile);

                if (profile.formsPersistentTerritory(MobMindData.isBoss(mob))) {
                    TerritoryManager.ensureTerritory(mob, level);
                    if (MobMindData.isBoss(mob)) TerritoryManager.boostBossTerritory(mob, level);
                }

                switch (species) {
                    case COW, MOOSHROOM -> CowBehavior.tick(mob, level);
                    case WOLF -> WolfBehavior.tick(mob, level);
                    case SPIDER, CAVE_SPIDER -> SpiderBehavior.tick(mob, level);
                    case ZOMBIE, ZOMBIE_VILLAGER, HUSK, DROWNED -> ZombieBehavior.tick(mob, level);
                    case COD, SALMON, TROPICAL_FISH, PUFFERFISH, SQUID, GLOW_SQUID, DOLPHIN,
                            AXOLOTL, TURTLE, TADPOLE, FROG -> AquaticBehavior.tick(mob, level);
                    case BAT, PARROT, ALLAY, BEE -> FlyingColonyBehavior.tick(mob, level);
                    default -> GenericBehavior.tick(mob, level, profile);
                }
            }
        });
    }
}
