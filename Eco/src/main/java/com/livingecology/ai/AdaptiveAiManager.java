package com.livingecology.ai;

import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesType;
import com.livingecology.territory.TerritoryManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

import java.util.HashSet;
import java.util.Set;

public final class AdaptiveAiManager {
    private AdaptiveAiManager() {}

    public static void tickLevel(ServerLevel level) {
        if (level.players().isEmpty()) return;
        long now = level.getGameTime();
        if ((now & 1L) != 0L) return; // spatial scan at 10 Hz under the normal 20 TPS target
        long brainStep = now / 2L;
        Set<Integer> handled = new HashSet<>();

        level.players().forEach(player -> {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(72.0D), MobMindData::supports)) {
                if (!handled.add(mob.getId())) continue;
                MobMindData.initialize(mob, level);
                MobMindData.decayStates(mob, level);

                // A resting entity is held still after vanilla goals tick. Damage/pressure immediately clears this flag.
                if (MobMindData.isResting(mob)) mob.getNavigation().stop();

                int interval = Math.max(3, MobMindData.thinkInterval(mob) / 2);
                if (Math.floorMod(brainStep + mob.getId(), interval) != 0L) continue;

                SpeciesType species = SpeciesType.from(mob).orElse(null);
                if (species == null) continue;
                TerritoryManager.ensureTerritory(mob, level);
                if (MobMindData.isBoss(mob)) TerritoryManager.boostBossTerritory(mob, level);

                switch (species) {
                    case COW -> CowBehavior.tick(mob, level);
                    case WOLF -> WolfBehavior.tick(mob, level);
                    case SPIDER -> SpiderBehavior.tick(mob, level);
                    case ZOMBIE -> ZombieBehavior.tick(mob, level);
                }
            }
        });
    }
}
