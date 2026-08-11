package com.livingecology.territory;

import com.livingecology.ai.BehaviorUtil;
import com.livingecology.data.AttributeType;
import com.livingecology.data.FootprintType;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.environment.EcologyMath;
import com.livingecology.environment.EnvironmentManager;
import com.livingecology.environment.EnvironmentSavedData;
import com.livingecology.environment.EnvironmentSnapshot;
import com.livingecology.util.HashNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.*;

public final class TerritoryManager {
    private TerritoryManager() {}

    public static void tickLevel(ServerLevel level) {
        long now = level.getGameTime();
        if (now % 40L == 0L) EnvironmentManager.sampleFireNearPlayers(level);
        if (now % 100L == 0L) updateActiveTerritories(level);
        if (now % 200L == 0L) resolveActiveRelations(level);
        materializeFootprints(level, 2, 14);
    }

    public static TerritoryRecord ensureTerritory(Mob mob, ServerLevel level) {
        SpeciesType species = SpeciesType.from(mob).orElse(null);
        if (species == null) return null;
        SpeciesProfile profile = SpeciesProfile.of(species);
        if (isPlayerBound(mob) || !profile.formsPersistentTerritory(MobMindData.isBoss(mob))) {
            if (MobMindData.territoryId(mob) != 0L) MobMindData.setTerritoryId(mob, 0L);
            return null;
        }

        TerritorySavedData data = TerritorySavedData.get(level);
        long currentId = MobMindData.territoryId(mob);
        TerritoryRecord current = data.get(currentId);
        if (current != null && current.species() == species) return current;
        if (currentId != 0L) MobMindData.setTerritoryId(mob, 0L);

        TerritoryRecord nearby = data.near(mob.blockPosition()).stream()
                .filter(r -> r.species() == species && r.state() != TerritoryState.ABANDONED)
                .filter(r -> r.center().distSqr(mob.blockPosition()) <= sqr((r.radiusChunks() + 3) * 16.0D))
                .min(Comparator.comparingDouble(r -> r.center().distSqr(mob.blockPosition())))
                .orElse(null);
        if (nearby != null) {
            MobMindData.setTerritoryId(mob, nearby.id());
            if (MobMindData.isBoss(mob)) nearby.setLeader(mob.getUUID());
            data.setDirty();
            return nearby;
        }

        long now = level.getGameTime();
        if (now < MobMindData.nextTerritoryCheck(mob)) return null;
        MobMindData.setNextTerritoryCheck(mob, now + 200L);

        List<Mob> same = level.getEntitiesOfClass(Mob.class,
                mob.getBoundingBox().inflate(profile.territorySearchRadius()),
                e -> e.isAlive() && species.matches(e) && !isPlayerBound(e));
        int minimum = MobMindData.isBoss(mob) ? 1 : profile.territoryFormationMinimum();
        if (same.size() < minimum) return null;

        long sx = 0L, sy = 0L, sz = 0L;
        for (Mob member : same) {
            sx += member.blockPosition().getX();
            sy += member.blockPosition().getY();
            sz += member.blockPosition().getZ();
        }
        BlockPos center = new BlockPos((int) (sx / same.size()), (int) (sy / same.size()), (int) (sz / same.size()));
        long seed = HashNoise.combine(level.getSeed(), center.asLong(), species.ordinal() * 341873128712L);
        int maturity = 20 + HashNoise.bounded(seed ^ 0x55aa55aa55aa55aaL, 61); // procedural "history before discovery"
        int territoriality = MobMindData.getAttribute(mob, AttributeType.TERRITORY);
        int maxRadius = Math.max(2, profile.maxTerritoryRadius());
        int radius = Mth.clamp(2 + territoriality / 25 + Math.min(2, same.size() / 4), 2, maxRadius);
        int pressure = Mth.clamp(25 + same.size() * 5 + maturity / 3, 15, 90);

        TerritoryRecord created = data.create(species, center, center, radius, pressure, maturity,
                same.size(), seed, now);
        if (MobMindData.isBoss(mob)) created.setLeader(mob.getUUID());
        for (Mob member : same) {
            MobMindData.initialize(member, level);
            MobMindData.setTerritoryId(member, created.id());
        }
        data.setDirty();
        return created;
    }

    private static void updateActiveTerritories(ServerLevel level) {
        TerritorySavedData data = TerritorySavedData.get(level);
        long now = level.getGameTime();
        boolean changed = false;
        for (TerritoryRecord record : activeTerritories(level)) {
            changed |= catchUp(record, level, data.simulationScale(), now);
            changed |= updateLoadedPopulation(record, level);
            if (record.pressure() >= 88 && record.population() >= Math.max(5, record.radiusChunks())) {
                if (record.state() != TerritoryState.DOMINANT) {
                    record.setState(TerritoryState.DOMINANT);
                    changed = true;
                }
            } else if (record.state() == TerritoryState.DOMINANT && record.pressure() < 70) {
                record.setState(TerritoryState.ACTIVE);
                changed = true;
            }
        }
        if (changed) data.setDirty();
    }

    private static boolean catchUp(TerritoryRecord record, ServerLevel level, double scale, long now) {
        long rawElapsed = Math.max(0L, now - record.lastRawUpdate());
        if (rawElapsed <= 0L) return false;
        double buffer = record.simulatedTickBuffer() + rawElapsed * scale;
        long steps = (long) Math.floor(buffer / 2400.0D);
        record.setLastRawUpdate(now);
        record.setSimulatedTickBuffer(buffer - steps * 2400.0D);
        if (steps <= 0L) return true;

        EnvironmentSnapshot env = EnvironmentManager.snapshot(level, record.center(), record.species());
        int oldMaturity = record.maturity();
        int oldPressure = record.pressure();

        long maturitySteps = Math.min(40L, steps / 2L + 1L);
        if (record.population() > 0 && env.habitability() >= 42) {
            record.setMaturity(record.maturity() + (int) maturitySteps);
        } else if (env.habitability() < 30 || record.population() <= 0) {
            record.setMaturity(record.maturity() - (int) maturitySteps);
        }

        int target = EcologyMath.pressureTarget(record.population(), record.maturity(), env.habitability(),
                record.state() == TerritoryState.ABANDONED);
        int maxChange = (int) Math.min(30L, steps / 2L + 1L);
        record.setPressure(approach(record.pressure(), target, maxChange));

        SpeciesProfile profile = SpeciesProfile.of(record.species());
        if (profile.maxTerritoryRadius() > 0 && record.state() != TerritoryState.ABANDONED) {
            int desiredRadius = EcologyMath.desiredRadius(profile, record.radiusChunks(), record.population(), env.habitability());
            if (desiredRadius != record.radiusChunks()) record.setRadiusChunks(desiredRadius);
        }

        if (record.population() <= 0 && record.pressure() <= 8) record.setState(TerritoryState.ABANDONED);
        else if (record.state() == TerritoryState.ABANDONED && record.population() > 0) record.setState(TerritoryState.RECOVERING);
        else if (record.state() == TerritoryState.RECOVERING && record.pressure() >= 35) record.setState(TerritoryState.ACTIVE);

        return oldMaturity != record.maturity() || oldPressure != record.pressure() || rawElapsed > 0L;
    }

    private static int approach(int current, int target, int maxChange) {
        if (current < target) return Math.min(target, current + maxChange);
        if (current > target) return Math.max(target, current - maxChange);
        return current;
    }

    private static boolean updateLoadedPopulation(TerritoryRecord record, ServerLevel level) {
        double radius = record.radiusChunks() * 16.0D * 1.15D;
        List<Mob> loaded = level.getEntitiesOfClass(Mob.class, new AABB(record.center()).inflate(radius),
                e -> e.isAlive() && record.species().matches(e) && !isPlayerBound(e));
        int actual = loaded.size();
        int old = record.population();
        if (actual > 0) {
            int smoothed = Math.max(1, (int) Math.round(old * 0.65D + actual * 0.35D));
            record.setPopulation(smoothed);
            for (Mob member : loaded) {
                MobMindData.initialize(member, level);
                if (MobMindData.territoryId(member) == 0L || MobMindData.territoryId(member) == record.id()) {
                    MobMindData.setTerritoryId(member, record.id());
                }
                if (MobMindData.isBoss(member)) record.setLeader(member.getUUID());
            }
        } else if (level.hasChunkAt(record.core())) {
            record.setPopulation(Math.max(0, old - 1));
        }
        return old != record.population();
    }

    private static List<TerritoryRecord> activeTerritories(ServerLevel level) {
        TerritorySavedData data = TerritorySavedData.get(level);
        LinkedHashMap<Long, TerritoryRecord> active = new LinkedHashMap<>();
        // Query only the spatial buckets around players. Cost does not grow linearly with the lifetime total of saved territories.
        level.players().forEach(player -> {
            for (TerritoryRecord record : data.near(player.blockPosition())) {
                double range = record.radiusChunks() * 16.0D + 96.0D;
                if (player.distanceToSqr(record.center().getX() + 0.5D, record.center().getY() + 0.5D,
                        record.center().getZ() + 0.5D) <= range * range) {
                    active.put(record.id(), record);
                }
            }
        });
        return new ArrayList<>(active.values());
    }

    private static void resolveActiveRelations(ServerLevel level) {
        TerritorySavedData data = TerritorySavedData.get(level);
        List<TerritoryRecord> active = activeTerritories(level);
        long now = level.getGameTime();
        boolean dirty = false;
        for (int i = 0; i < active.size(); i++) {
            for (int j = i + 1; j < active.size(); j++) {
                TerritoryRecord a = active.get(i);
                TerritoryRecord b = active.get(j);
                double contactRange = (a.radiusChunks() + b.radiusChunks()) * 16.0D + 40.0D;
                if (a.center().distSqr(b.center()) > contactRange * contactRange) continue;

                boolean overlap = a.center().distSqr(b.center()) < sqr((a.radiusChunks() + b.radiusChunks()) * 16.0D);
                RelationshipProfile natural = RelationService.natural(a.species(), b.species());
                RelationRecord relation = data.getRelation(a.id(), b.id());
                if (relation == null && natural.kind() != RelationKind.NEUTRAL) relation = data.relation(a.id(), b.id());
                if (relation == null) continue;

                if (relation.lastResolution() <= 0L) {
                    relation.setLastResolution(now);
                    dirty = true;
                    continue;
                }

                long rawElapsed = Math.max(0L, now - relation.lastResolution());
                long ecologicalElapsed = (long) Math.floor(rawElapsed * data.simulationScale());
                int epochs = (int) Math.min(8L, ecologicalElapsed / 1200L);
                if (epochs <= 0) continue;
                relation.setLastResolution(now);

                int tension = RelationService.tension(data, a, b, overlap);
                int cooperation = RelationService.cooperation(data, a, b);

                if (natural.kind() == RelationKind.SYMBIOTIC && overlap && cooperation >= 55) {
                    int gain = Math.min(4, epochs);
                    a.addPressure(gain);
                    b.addPressure(gain);
                    relation.addAffinity(Math.max(1, (epochs + 1) / 2), now);
                    dirty = true;
                }

                if (tension >= 75) {
                    int noiseEpoch = (int) Math.max(1L, now / 1200L);
                    int powerA = territoryPower(a) + HashNoise.signed(HashNoise.combine(a.seed(), b.seed(), noiseEpoch), 8);
                    int powerB = territoryPower(b) + HashNoise.signed(HashNoise.combine(b.seed(), a.seed(), noiseEpoch), 8);
                    if (Math.abs(powerA - powerB) >= 4) {
                        TerritoryRecord winner = powerA > powerB ? a : b;
                        TerritoryRecord loser = powerA > powerB ? b : a;
                        int push = Math.min(4, Math.max(1, epochs / 2));
                        winner.addPressure(push);
                        loser.addPressure(-push * 2);
                        relation.addMomentum((winner.id() == relation.a() ? 1 : -1) * push);
                        relation.addRivalry(Math.min(3, epochs), now);
                        a.setState(TerritoryState.CONTESTED);
                        b.setState(TerritoryState.CONTESTED);
                        if (Math.abs(relation.warMomentum()) >= 5) conquestStep(level, data, winner, loser, relation);
                        dirty = true;
                    }
                } else if (natural.kind() == RelationKind.BORDERED && tension < 60 && overlap
                        && now - relation.lastAggression() > 12000L) {
                    // Quiet borders can stabilize into a no-man's-land without requiring a scripted peace event.
                    relation.adjustRivalry(-Math.min(2, epochs));
                    relation.adjustAffinity(1);
                    dirty = true;
                }
            }
        }
        if (dirty) data.setDirty();
    }

    private static int territoryPower(TerritoryRecord record) {
        SpeciesProfile profile = SpeciesProfile.of(record.species());
        int constitutionMid = profile.midpoint(AttributeType.CONSTITUTION);
        int socialMid = profile.midpoint(AttributeType.SOCIABILITY);
        return record.pressure() + record.population() * 3 + constitutionMid / 2 + socialMid / 5
                + (record.leader() != null ? 12 : 0);
    }

    private static void conquestStep(ServerLevel level, TerritorySavedData data, TerritoryRecord winner,
                                     TerritoryRecord loser, RelationRecord relation) {
        int winnerMax = Math.max(2, SpeciesProfile.of(winner.species()).maxTerritoryRadius());
        winner.setRadiusChunks(Math.min(winnerMax, winner.radiusChunks() + 1));
        loser.setRadiusChunks(Math.max(2, loser.radius×^ý¶‰žËkºwµçP€´‘¥ÍÑ…¹”€¼5…Ñ ¹µ…à Ä¸Á°É…‘¥ÕÌ¤ì(€€€€€€€É•ÑÕÉ¸É•½É¹ÁÉ•ÍÍÕÉ” ¤€¨™…Ñ½Èì(€€€ô((€€€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ‘½Õ‰±”•™™•Ñ¥Ù•I…‘¥ÕÍ	±½­Ì¡Q•ÉÉ¥Ñ½ÉåI•½ÉÉ•½É°	±½­A½ÌÁ½Ì¤ì(€€€€€€€¥¹Ð¡Õ¹­`€ôÁ½Ì¹•Ñ` ¤€øø€Ðì(€€€€€€€¥¹Ð¡Õ¹­h€ôÁ½Ì¹•Ñh ¤€øø€Ðì(€€€€€€€‘½Õ‰±”•‘•9½¥Í”€ô€À¸àá€¬!…Í¡9½¥Í”¹Õ¹¥Ð¡!…Í¡9½¥Í”¹½µ‰¥¹”¡É•½É¹Í•• ¤°¡Õ¹­`°¡Õ¹­h¤¤€¨€À¸ÈÑì(€€€€€€€É•ÑÕÉ¸É•½É¹É…‘¥ÕÍ¡Õ¹­Ì ¤€¨€ÄØ¸Á€¨•‘•9½¥Í”ì(€€€ô((€€€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ‘½Õ‰±”¡½É¥é½¹Ñ…±¥ÍÑ…¹”¡	±½­A½Ì„°	±½­A½Ìˆ¤ì(€€€€€€€‘½Õ‰±”‘à€ô„¹•Ñ` ¤€´ˆ¹•Ñ` ¤ì(€€€€€€€‘½Õ‰±”‘è€ô„¹•Ñh ¤€´ˆ¹•Ñh ¤ì(€€€€€€€É•ÑÕÉ¸5…Ñ ¹ÍÅÉÐ¡‘à€¨‘à€¬‘è€¨‘è¤ì(€€€ô((€€€ÁÕ‰±¥ŒÍÑ…Ñ¥ŒÙ½¥É•½É‘É•ÍÍ¥½¸¡5½ˆÙ¥Ñ¥´°1¥Ù¥¹¹Ñ¥Ñä…ÑÑ…­•È°M•ÉÙ•É1•Ù•°±•Ù•°°¥¹ÐÍ•Ù•É¥Ñä¤ì(€€€€€€€¥˜€ „¡…ÑÑ…­•È¥¹ÍÑ…¹•½˜5½ˆ…ÑÑ…­•É5½ˆ¤¤É•ÑÕÉ¸ì(€€€€€€€¥˜€ …5½‰5¥¹‘…Ñ„¹ÍÕÁÁ½ÉÑÌ¡…ÑÑ…­•É5½ˆ¤¤É•ÑÕÉ¸ì(€€€€€€€•¹ÍÕÉ•Q•ÉÉ¥Ñ½Éä¡Ù¥Ñ¥´°±•Ù•°¤ì(€€€€€€€•¹ÍÕÉ•Q•ÉÉ¥Ñ½Éä¡…ÑÑ…­•É5½ˆ°±•Ù•°¤ì(€€€€€€€±½¹œ„€ô5½‰5¥¹‘…Ñ„¹Ñ•ÉÉ¥Ñ½Éå%¡Ù¥Ñ¥´¤ì(€€€€€€€±½¹œˆ€ô5½‰5¥¹‘…Ñ„¹Ñ•ÉÉ¥Ñ½Éå%¡…ÑÑ…­•É5½ˆ¤ì(€€€€€€€¥˜€¡„€ðô€Àñðˆ€ðô€Àñð„€ôôˆ¤É•ÑÕÉ¸ì(€€€€€€€Q•ÉÉ¥Ñ½ÉåM…Ù•‘…Ñ„‘…Ñ„€ôQ•ÉÉ¥Ñ½ÉåM…Ù•‘…Ñ„¹•Ð¡±•Ù•°¤ì(€€€€€€€I•±…Ñ¥½¹I•½ÉÉ•±…Ñ¥½¸€ô‘…Ñ„¹É•±…Ñ¥½¸¡„°ˆ¤ì(€€€€€€€¥˜€¡É•±…Ñ¥½¸€„ô¹Õ±°¤ì(€€€€€€€€€€€É•±…Ñ¥½¸¹…‘‘I¥Ù…±Éä¡5…Ñ ¹µ…à Ä°Í•Ù•É¥Ñä¤°±•Ù•°¹•Ñ…µ•Q¥µ” ¤¤ì(€€€€€€€€€€€É•±…Ñ¥½¸¹…‘©ÕÍÑ™™¥¹¥Ñä µ5…Ñ ¹µ…à Ä°Í•Ù•É¥Ñä€¼€È¤¤ì(€€€€€€€€€€€‘…Ñ„¹Í•Ñ¥ÉÑä ¤ì(€€€€€€€ô(€€€ô((€€€ÁÕ‰±¥ŒÍÑ…Ñ¥ŒÙ½¥É•½É‘½½Á•É…Ñ¥½¸¡5½ˆ¡•±Á•È°5½ˆ…±±ä°M•ÉÙ•É1•Ù•°±•Ù•°°¥¹Ð…µ½Õ¹Ð¤ì(€€€€€€€•¹ÍÕÉ•Q•ÉÉ¥Ñ½Éä¡¡•±Á•È°±•Ù•°¤ì(€€€€€€€•¹ÍÕÉ•Q•ÉÉ¥Ñ½Éä¡…±±ä°±•Ù•°¤ì(€€€€€€€±½¹œ„€ô5½‰5¥¹‘…Ñ„¹Ñ•ÉÉ¥Ñ½Éå%¡¡•±Á•È¤ì(€€€€€€€±½¹œˆ€ô5½‰5¥¹‘…Ñ„¹Ñ•ÉÉ¥Ñ½Éå%¡…±±ä¤ì(€€€€€€€¥˜€¡„€ðô€Àñðˆ€ðô€Àñð„€ôôˆ¤É•ÑÕÉ¸ì(€€€€€€€Q•ÉÉ¥Ñ½ÉåM…Ù•‘…Ñ„‘…Ñ„€ôQ•ÉÉ¥Ñ½ÉåM…Ù•‘…Ñ„¹•Ð¡±•Ù•°¤ì(€€€€€€€I•±…Ñ¥½¹I•½ÉÉ•±…Ñ¥½¸€ô‘…Ñ„¹É•±…Ñ¥½¸¡„°ˆ¤ì(€€€€€€€¥˜€¡É•±…Ñ¥½¸€„ô¹Õ±°¤ì(€€€€€€€€€€€É•±…Ñ¥½¸¹…‘‘™™¥¹¥Ñä¡5…Ñ ¹µ…à Ä°…µ½Õ¹Ð¤°±•Ù•°¹•Ñ…µ•Q¥µ” ¤¤ì(€€€€€€€€€€€É•±…Ñ¥½¸¹…‘‘I¥Ù…±Éä ´Ä°±•Ù•°¹•Ñ…µ•Q¥µ” ¤¤ì(€€€€€€€€€€€‘…Ñ„¹Í•Ñ¥ÉÑä ¤ì(€€€€€€€ô(€€€ô((€€€€¼¨¨(€€€€€¨5½Ù•Ì„Ñ•ÉÉ¥Ñ½ÉäÌÁ•ÉÍ¥ÍÑ••¹Ñ•È½½É”Ñ¼„±½…‘•°¹•…É‰ä•½±½¥…°…¹¡½È¸(€€€€€¨…±±•ÉÌÙ…±¥‘…Ñ”Ñ¡”…¹¡½ÈÑåÁ”€¡™½È•á…µÁ±”„	•”¡¥Ù”¤ìÑ¡¥Ìµ•Ñ¡½ÍÕÁÁ±¥•ÌÑ¡”Í¡…É•(€€€€€¨¹¼µ™½É•µ¡Õ¹¬°‰½Õ¹‘•µ‘¥ÍÑ…¹”…¹ÍÁ…Ñ¥…°µ¥¹‘•à¥¹Ù…É¥…¹ÑÌ¸(€€€€€¨¼(€€€ÁÕ‰±¥ŒÍÑ…Ñ¥Œ‰½½±•…¸É••¹Ñ•É=¹1½…‘•‘½É”¡Q•ÉÉ¥Ñ½ÉåI•½ÉÑ•ÉÉ¥Ñ½Éä°M•ÉÙ•É1•Ù•°±•Ù•°°(€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€	±½­A½Ì…¹¡½È°‘½Õ‰±”µ…á¥ÍÑ…¹”¤ì(€€€€€€€¥˜€¡Ñ•ÉÉ¥Ñ½Éä€ôô¹Õ±°ñð…¹¡½È€ôô¹Õ±°ñð€…±•Ù•°¹¡…Í¡Õ¹­Ð¡…¹¡½È¤¤É•ÑÕÉ¸™…±Í”ì(€€€€€€€‘½Õ‰±”‰½Õ¹‘•‘¥ÍÑ…¹”€ô5Ñ ¹±…µÀ¡µ…á¥ÍÑ…¹”°€Ä¸Á°€ØÐ¸Á¤ì(€€€€€€€¥˜€¡Ñ•ÉÉ¥Ñ½Éä¹•¹Ñ•È ¤¹‘¥ÍÑMÅÈ¡…¹¡½È¤€ø‰½Õ¹‘•‘¥ÍÑ…¹”€¨‰½Õ¹‘•‘¥ÍÑ…¹”¤É•ÑÕÉ¸™…±Í”ì(€€€€€€€¥˜€¡Ñ•ÉÉ¥Ñ½Éä¹•¹Ñ•È ¤¹•ÅÕ…±Ì¡…¹¡½È¤€˜˜Ñ•ÉÉ¥Ñ½Éä¹½É” ¤¹•ÅÕ…±Ì¡…¹¡½È¤¤É•ÑÕÉ¸ÑÉÕ”ì((€€€€€€€	±½­A½Ì½±‘•¹Ñ•È€ôÑ•ÉÉ¥Ñ½Éä¹•¹Ñ•È ¤ì(€€€€€€€Ñ•ÉÉ¥Ñ½Éä¹Í•Ñ•¹Ñ•È¡…¹¡½È¤ì(€€€€€€€Ñ•ÉÉ¥Ñ½Éä¹Í•Ñ½É”¡…¹¡½È¤ì(€€€€€€€Q•ÉÉ¥Ñ½ÉåM…Ù•‘…Ñ„¹•Ð¡±•Ù•°¤¹É•¥¹‘•à¡Ñ•ÉÉ¥Ñ½Éä°½±‘•¹Ñ•È¤ì(€€€€€€€É•ÑÕÉ¸ÑÉÕ”ì(€€€ô((€€€ÁÕ‰±¥ŒÍÑ…Ñ¥ŒÙ½¥½¹½‰Ý•‰	É½­•¸¡M•ÉÙ•É1•Ù•°±•Ù•°°	±½­A½ÌÁ½Ì¤ì(€€€€€€€Q•ÉÉ¥Ñ½ÉåM…Ù•‘…Ñ„‘…Ñ„€ôQ•ÉÉ¥Ñ½ÉåM…Ù•‘…Ñ„¹•Ð¡±•Ù•°¤ì(€€€€€€€Q•ÉÉ¥Ñ½ÉåI•½ÉÍÁ¥‘•È€ô‘…Ñ„¹¹•…È¡Á½Ì¤¹ÍÑÉ•…´ ¤(€€€€€€€€€€€€€€€€¹™¥±Ñ•È¡È€´øMÁ•¥•ÍAÉ½™¥±”¹½˜¡È¹ÍÁ•¥•Ì ¤¤¹™½½ÑÁÉ¥¹ÑQåÁ” ¤€ôô½½ÑÁÉ¥¹ÑQåÁ”¹=	](€€€€€€€€€€€€€€€€€€€€€€€€˜˜é½¹•½È¡È°Á½Ì¤€„ôQ•ÉÉ¥Ñ½Éåi½¹”¹=UQM%¤(€€€€€€€€€€€€€€€€¹µ…à¡½µÁ…É…Ñ½È¹½µÁ…É¥¹½Õ‰±”¡È€´ø¥¹™±Õ•¹•Ð¡È°Á½Ì¤¤¤¹½É±Í”¡¹Õ±°¤ì(€€€€€€€¥˜€¡ÍÁ¥‘•È€ôô¹Õ±°¤É•ÑÕÉ¸ì(€€€€€€€ÍÁ¥‘•È¹…‘‘½½ÑÁÉ¥¹ÑAÉ½É•ÍÌ ´Ä¤ì(€€€€€€€¥˜€¡é½¹•½È¡ÍÁ¥‘•È°Á½Ì¤€ôôQ•ÉÉ¥Ñ½Éåi½¹”¹=I¤ÍÁ¥‘•È¹…‘‘AÉ•ÍÍÕÉ” ´Ä¤ì(€€€€€€€‘…Ñ„¹Í•Ñ¥ÉÑä ¤ì(€€€ô((€€€€¼¨¨…±±•‰äÑ¡”Ù…¹¥±±„‰É••‘¥¹œ•Ù•¹Ð¸	¥ÉÑ¡ÌÉ…¥Í”±½…°Á½ÁÕ±…Ñ¥½¸½ÁÉ•ÍÍÕÉ”…¹…¸•áÁ…¹„¡•…±Ñ¡äÑ•ÉÉ¥Ñ½Éä¸€¨¼(€€€ÁÕ‰±¥ŒÍÑ…Ñ¥ŒÙ½¥É•½É‘	¥ÉÑ ¡5½ˆÁ…É•¹Ð°5½ˆ¡¥±°M•ÉÙ•É1•Ù•°±•Ù•°¤ì(€€€€€€€Q•ÉÉ¥Ñ½ÉåI•½ÉÑ•ÉÉ¥Ñ½Éä€ô•¹ÍÕÉ•Q•ÉÉ¥Ñ½Éä¡Á…É•¹Ð°±•Ù•°¤ì(€€€€€€€¥˜€¡Ñ•ÉÉ¥Ñ½Éä€ôô¹Õ±°¤É•ÑÕÉ¸ì(€€€€€€€5½‰5¥¹‘…Ñ„¹¥¹¥Ñ¥…±¥é”¡¡¥±°±•Ù•°¤ì(€€€€€€€5½‰5¥¹‘…Ñ„¹Í•ÑQ•ÉÉ¥Ñ½Éå%¡¡¥±°Ñ•ÉÉ¥Ñ½Éä¹¥ ¤¤ì(€€€€€€€Ñ•ÉÉ¥Ñ½Éä¹Í•ÑA½ÁÕ±…Ñ¥½¸¡Ñ•ÉÉ¥Ñ½Éä¹Á½ÁÕ±…Ñ¥½¸ ¤€¬€Ä¤ì(€€€€€€€Ñ•ÉÉ¥Ñ½Éä¹…‘‘AÉ•ÍÍÕÉ” È¤ì(€€€€€€€¹Ù¥É½¹µ•¹ÑM¹…ÁÍ¡½Ð•¹Ø€ô¹Ù¥É½¹µ•¹Ñ5…¹…•È¹Í¹…ÁÍ¡½Ð¡±•Ù•°°Ñ•ÉÉ¥Ñ½Éä¹•¹Ñ•È ¤°Ñ•ÉÉ¥Ñ½Éä¹ÍÁ•¥•Ì ¤¤ì(€€€€€€€MÁ•¥•ÍAÉ½™¥±”ÁÉ½™¥±”€ôMÁ•¥•ÍAÉ½™¥±”¹½˜¡Ñ•ÉÉ¥Ñ½Éä¹ÍÁ•¥•Ì ¤¤ì(€€€€€€€¥¹Ð‘•Í¥É•€ô½±½å5…Ñ ¹‘•Í¥É•‘I…‘¥ÕÌ¡ÁÉ½™¥±”°Ñ•ÉÉ¥Ñ½Éä¹É…‘¥ÕÍ¡Õ¹­Ì ¤°Ñ•ÉÉ¥Ñ½Éä¹Á½ÁÕ±…Ñ¥½¸ ¤°•¹Ø¹¡…‰¥Ñ…‰¥±¥Ñä ¤¤ì(€€€€€€€¥˜€¡‘•Í¥É•€øÑ•ÉÉ¥Ñ½Éä¹É…‘¥ÕÍ¡Õ¹­Ì ¤¤Ñ•ÉÉ¥Ñ½Éä¹Í•ÑI…‘¥ÕÍ¡Õ¹­Ì¡‘•Í¥É•¤ì(€€€€€€€Q•ÉÉ¥Ñ½ÉåM…Ù•‘…Ñ„¹•Ð¡±•Ù•°¤¹Í•Ñ¥ÉÑä ¤ì(€€€ô((€€€ÁÕ‰±¥ŒÍÑ…Ñ¥ŒÙ½¥‰½½ÍÑ	½ÍÍQ•ÉÉ¥Ñ½Éä¡5½ˆ‰½ÍÌ°M•ÉÙ•É1•Ù•°±•Ù•°¤ì(€€€€€€€¥˜€ …5½‰5¥¹‘…Ñ„¹¥Í	½ÍÌ¡‰½ÍÌ¤¤É•ÑÕÉ¸ì(€€€€€€€Q•ÉÉ¥Ñ½ÉåI•½ÉÑ•ÉÉ¥Ñ½Éä€ô•¹ÍÕÉ•Q•ÉÉ¥Ñ½Éä¡‰½ÍÌ°±•Ù•°¤ì(€€€€€€€¥˜€¡Ñ•ÉÉ¥Ñ½Éä€ôô¹Õ±°¤É•ÑÕÉ¸ì(€€€€€€€Ñ•ÉÉ¥Ñ½Éä¹Í•Ñ1•…‘•È¡‰½ÍÌ¹•ÑUU% ¤¤ì(€€€€€€€¥˜€¡±•Ù•°¹•Ñ…µ•Q¥µ” ¤€”€ÈÀÁ0€ôô€Á0¤Ñ•ÉÉ¥Ñ½Éä¹…‘‘AÉ•ÍÍÕÉ” Ä¤ì(€€€€€€€Q•ÉÉ¥Ñ½ÉåM…Ù•‘…Ñ„¹•Ð¡±•Ù•°¤¹Í•Ñ¥ÉÑä ¤ì(€€€ô((€€€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥ŒÙ½¥µ…Ñ•É¥…±¥é•½½ÑÁÉ¥¹ÑÌ¡M•ÉÙ•É1•Ù•°±•Ù•°°¥¹Ð‰±½­	Õ‘•Ð°¥¹Ðµ…á¡•­Ì¤ì(€€€€€€€¥˜€¡‰±½­	Õ‘•Ð€ðô€Àñðµ…á¡•­Ì€ðô€À¤É•ÑÕÉ¸ì(€€€€€€€Q•ÉÉ¥Ñ½ÉåM…Ù•‘…Ñ„‘…Ñ„€ôQ•ÉÉ¥Ñ½ÉåM…Ù•‘…Ñ„¹•Ð¡±•Ù•°¤ì(€€€€€€€1¥ÍÐñQ•ÉÉ¥Ñ½ÉåI•½ÉøÉ•½É‘Ì€ô…Ñ¥Ù•Q•ÉÉ¥Ñ½É¥•Ì¡±•Ù•°¤¹ÍÑÉ•…´ ¤(€€€€€€€€€€€€€€€€¹™¥±Ñ•È¡È€´øMÁ•¥•ÍAÉ½™¥±”¹½˜¡È¹ÍÁ•¥•Ì ¤¤¹™½½ÑÁÉ¥¹ÑQåÁ” ¤€„ô½½ÑÁÉ¥¹ÑQåÁ”¹9=9¤(€€€€€€€€€€€€€€€€¹Í½ÉÑ•¡½µÁ…É…Ñ½È¹½µÁ…É¥¹1½¹œ¡Q•ÉÉ¥Ñ½ÉåI•½Éèé¥¤¤¹Ñ½1¥ÍÐ ¤ì(€€€€€€€¥˜€¡É•½É‘Ì¹¥ÍµÁÑä ¤¤É•ÑÕÉ¸ì((€€€€€€€¥¹ÐÍÑ…ÉÐ€ô€¡¥¹Ð¤5…Ñ ¹™±½½É5½¡±•Ù•°¹•Ñ…µ•Q¥µ” ¤°É•½É‘Ì¹Í¥é” ¤¤ì(€€€€€€€¥¹Ð¡…¹•€ô€Àì(€€€€€€€¥¹Ð¡•­Ì€ô€Àì(€€€€€€€™½È€¡¥¹Ð½™™Í•Ð€ô€Àì½™™Í•Ð€ðÉ•½É‘Ì¹Í¥é” ¤€˜˜¡…¹•€ð‰±½­	Õ‘•Ð€˜˜¡•­Ì€ðµ…á¡•­Ìì½™™Í•Ð¬¬¤ì(€€€€€€€€€€€Q•ÉÉ¥Ñ½ÉåI•½ÉÉ•½É€ôÉ•½É‘Ì¹•Ð ¡ÍÑ…ÉÐ€¬½™™Í•Ð¤€”É•½É‘Ì¹Í¥é” ¤¤ì(€€€€€€€€€€€½½ÑÁÉ¥¹ÑQåÁ”ÑåÁ”€ôMÁ•¥•ÍAÉ½™¥±”¹½˜¡É•½É¹ÍÁ•¥•Ì ¤¤¹™½½ÑÁÉ¥¹ÑQåÁ” ¤ì(€€€€€€€€€€€¥¹Ð‘•Í¥É•€ôÉ•½É¹‘•Í¥É•‘½½ÑÁÉ¥¹Ð ¤ì(€€€€€€€€€€€‰½½±•…¸É½Ü€ôÉ•½É¹™½½ÑÁÉ¥¹ÑAÉ½É•ÍÌ ¤€ð‘•Í¥É•ì(€€€€€€€€€€€‰½½±•…¸‘•…ä€ôÑåÁ”€ôô½½ÑÁÉ¥¹ÑQåÁ”¹=	]€˜˜É•½É¹™½½ÑÁÉ¥¹ÑAÉ½É•ÍÌ ¤€ø‘•Í¥É•ì(€€€€€€€€€€€¥˜€ …É½Ü€˜˜€…‘•…ä¤½¹Ñ¥¹Õ”ì((€€€€€€€€€€€Ý¡¥±”€¡¡…¹•€ð‰±½­	Õ‘•Ð€˜˜¡•­Ì€ðµ…á¡•­Ì¤ì(€€€€€€€€€€€€€€€¡•­Ì¬¬ì(€€€€€€€€€€€€€€€¥¹ÐÕÉÍ½È€ôÉ•½É¹¹•áÑ½½ÑÁÉ¥¹ÑÕÉÍ½È ¤ì(€€€€€€€€€€€€€€€	±½­A½Ì…¹‘¥‘…Ñ”€ô™½½ÑÁÉ¥¹Ñ…¹‘¥‘…Ñ”¡É•½É°ÕÉÍ½È¤ì(€€€€€€€€€€€€€€€¥˜€ …±•Ù•°¹¡…Í¡Õ¹­Ð¡…¹‘¥‘…Ñ”¤¤½¹Ñ¥¹Õ”ì(€€€€€€€€€€€€€€€Q•ÉÉ¥Ñ½Éåi½¹”é½¹”€ôé½¹•½È¡É•½É°…¹‘¥‘…Ñ”¤ì(€€€€€€€€€€€€€€€¥˜€¡é½¹”€ôôQ•ÉÉ¥Ñ½Éåi½¹”¹=UQM%¤½¹Ñ¥¹Õ”ì((€€€€€€€€€€€€€€€¥˜€¡É½Ü¤ì(€€€€€€€€€€€€€€€€€€€‘½Õ‰±”¡…¹”€ôÍÝ¥Ñ €¡é½¹”¤ì(€€€€€€€€€€€€€€€€€€€€€€€…Í”=I€´ø€À¸äÁì(€€€€€€€€€€€€€€€€€€€€€€€…Í”%99H€´ø€À¸Ôáì(€€€€€€€€€€€€€€€€€€€€€€€…Í”=UQH€´ø€À¸ÈÑì(€€€€€€€€€€€€€€€€€€€€€€€…Í”=UQM%€´ø€À¸Áì(€€€€€€€€€€€€€€€€€€€ôì(€€€€€€€€€€€€€€€€€€€¥˜€¡!…Í¡9½¥Í”¹Õ¹¥Ð¡!…Í¡9½¥Í”¹½µ‰¥¹”¡É•½É¹Í•• ¤°ÕÉÍ½È°€ääÅ0¤¤€ø¡…¹”¤½¹Ñ¥¹Õ”ì(€€€€€€€€€€€€€€€€€€€¥˜€¡ÑÉåA±…•½½ÑÁÉ¥¹Ð¡±•Ù•°°É•½É°ÑåÁ”°…¹‘¥‘…Ñ”°ÕÉÍ½È¤¤ì(€€€€€€€€€€€€€€€€€€€€€€€É•½É¹…‘‘½½ÑÁÉ¥¹ÑAÉ½É•ÍÌ Ä¤ì(€€€€€€€€€€€€€€€€€€€€€€€¡…¹•¬¬ì(€€€€€€€€€€€€€€€€€€€€€€€‰É•…¬ì(€€€€€€€€€€€€€€€€€€€ô(€€€€€€€€€€€€€€€ô•±Í”¥˜€¡ÑåÁ”€ôô½½ÑÁÉ¥¹ÑQåÁ”¹=	]€˜˜±•Ù•°¹•Ñ	±½­MÑ…Ñ”¡…¹‘¥‘…Ñ”¤¹¥Ì¡	±½­Ì¹=	]¤(€€€€€€€€€€€€€€€€€€€€€€€€˜˜¹½9•…É‰åA±…å•È¡±•Ù•°°…¹‘¥‘…Ñ”°€Ü¸Á¤¤ì(€€€€€€€€€€€€€€€€€€€±•Ù•°¹Í•Ñ	±½¬¡…¹‘¥‘…Ñ”°	±½­Ì¹%H¹‘•™…Õ±Ñ	±½­MÑ…Ñ” ¤°€Ì¤ì(€€€€€€€€€€€€€€€€€€€É•½É¹…‘‘½½ÑÁÉ¥¹ÑAÉ½É•ÍÌ ´Ä¤ì(€€€€€€€€€€€€€€€€€€€¡…¹•¬¬ì(€€€€€€€€€€€€€€€€€€€‰É•…¬ì(€€€€€€€€€€€€€€€ô(€€€€€€€€€€€ô(€€€€€€€ô(€€€€€€€¥˜€¡¡…¹•€ø€Àñð¡•­Ì€ø€À¤‘…Ñ„¹Í•Ñ¥ÉÑä ¤ì(€€€ô((€€€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ‰½½±•…¸ÑÉåA±…•½½ÑÁÉ¥¹Ð¡M•ÉÙ•É1•Ù•°±•Ù•°°Q•ÉÉ¥Ñ½ÉåI•½ÉÉ•½É°½½ÑÁÉ¥¹ÑQåÁ”ÑåÁ”°(€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€	±½­A½ÌÉ…Ü°¥¹ÐÕÉÍ½È¤ì(€€€€€€€É•ÑÕÉ¸ÍÝ¥Ñ €¡ÑåÁ”¤ì(€€€€€€€€€€€…Í”=	]€´øÑÉåA±…•½‰Ý•ˆ¡±•Ù•°°É…Ü¤ì(€€€€€€€€€€€…Í”1=]IL€´øÑÉåA±…•±½Ý•È¡±•Ù•°°É…Ü°É•½É¹Í•• ¤°ÕÉÍ½È¤ì(€€€€€€€€€€€…Í”QI%0€´øÑÉåA±…•QÉ…¥°¡±•Ù•°°É…Ü¤ì(€€€€€€€€€€€…Í”	UII=\€´øÑÉåA±…•	ÕÉÉ½Ý5…É¬¡±•Ù•°°É…Ü¤ì(€€€€€€€€€€€…Í”5UM!I==5L€´øÑÉåA±…•5ÕÍ¡É½½´¡±•Ù•°°É…Ü°É•½É¹Í•• ¤°ÕÉÍ½È¤ì(€€€€€€€€€€€…Í”9=9€´ø™…±Í”ì(€€€€€€€ôì(€€€ô((€€€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ	±½­A½Ì™½½ÑÁÉ¥¹Ñ…¹‘¥‘…Ñ”¡Q•ÉÉ¥Ñ½ÉåI•½ÉÉ•½É°¥¹ÐÕÉÍ½È¤ì(€€€€€€€±½¹œ Ä€ô!…Í¡9½¥Í”¹½µ‰¥¹”¡É•½É¹Í•• ¤°ÕÉÍ½È°€ÁàÅ˜ÄÈÍ‰ˆÕ0¤ì(€€€€€€€±½¹œ È€ô!…Í¡9½¥Í”¹µ¥àØÐ¡ Äx€ÁàÕ‘••”ØÙ‘0¤ì(€€€€€€€‘½Õ‰±”…¹±”€ô!…Í¡9½¥Í”¹Õ¹¥Ð¡ Ä¤€¨5…Ñ ¹A$€¨€È¸Áì(€€€€€€€‘½Õ‰±”µ…áI…‘¥ÕÌ€ôÉ•½É¹É…‘¥ÕÍ¡Õ¹­Ì ¤€¨€ÄØ¸Á€¨€À¸àÙì(€€€€€€€‘½Õ‰±”‘¥ÍÑ…¹”€ô5…Ñ ¹ÍÅÉÐ¡!…Í¡9½¥Í”¹Õ¹¥Ð¡ È¤¤€¨µ…áI…‘¥ÕÌì(€€€€€€€¥¹Ð‘à€ô€¡¥¹Ð¤5…Ñ ¹É½Õ¹¡5…Ñ ¹½Ì¡…¹±”¤€¨‘¥ÍÑ…¹”¤ì(€€€€€€€¥¹Ð‘è€ô€¡¥¹Ð¤5…Ñ ¹É½Õ¹¡5…Ñ ¹Í¥¸¡…¹±”¤€¨‘¥ÍÑ…¹”¤ì(€€€€€€€¥¹Ð‘ä€ô!…Í¡9½¥Í”¹Í¥¹•¡ Èx€ÁàÄÈÌÐÔØÜàå…‰‘•™0°€Ü¤ì(€€€€€€€É•ÑÕÉ¸É•½É¹½É” ¤¹½™™Í•Ð¡‘à°‘ä°‘è¤ì(€€€ô((€€€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ	±½­A½Ì™¥¹‘MÕÉ™…”¡M•ÉÙ•É1•Ù•°±•Ù•°°	±½­A½Ì…É½Õ¹¤ì(€€€€€€€™½È€¡¥¹Ð€ô€Àì€ðô€àì¬¬¤ì(€€€€€€€€€€€¥¹ÑmtåÌ€ô€ôô€À€ü¹•Ü¥¹Ñmuí…É½Õ¹¹•Ñd ¥ô€è¹•Ü¥¹Ñmuí…É½Õ¹¹•Ñd ¤€¬°…É½Õ¹¹•Ñd ¤€´‘ôì(€€€€€€€€€€€™½È€¡¥¹Ðä€èåÌ¤ì(€€€€€€€€€€€€€€€	±½­A½ÌÉ½Õ¹€ô¹•Ü	±½­A½Ì¡…É½Õ¹¹•Ñ` ¤°ä°…É½Õ¹¹•Ñh ¤¤ì(€€€€€€€€€€€€€€€¥˜€ …±•Ù•°¹¡…Í¡Õ¹­Ð¡É½Õ¹¤¤½¹Ñ¥¹Õ”ì(€€€€€€€€€€€€€€€	±½­MÑ…Ñ”ÍÑ…Ñ”€ô±•Ù•°¹•Ñ	±½­MÑ…Ñ”¡É½Õ¹¤ì(€€€€€€€€€€€€€€€¥˜€ …ÍÑ…Ñ”¹¥Í¥È ¤€˜˜±•Ù•°¹•Ñ	±½­MÑ…Ñ”¡É½Õ¹¹…‰½Ù” ¤¤¹¥Í¥È ¤¤É•ÑÕÉ¸É½Õ¹ì(€€€€€€€€€€€ô(€€€€€€€ô(€€€€€€€É•ÑÕÉ¸¹Õ±°ì(€€€ô((€€€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ‰½½±•…¸ÑÉåA±…•±½Ý•È¡M•ÉÙ•É1•Ù•°±•Ù•°°	±½­A½ÌÉ…Ü°±½¹œÍ••°¥¹ÐÕÉÍ½È¤ì(€€€€€€€	±½­A½ÌÉ½Õ¹€ô™¥¹‘MÕÉ™…”¡±•Ù•°°É…Ü¤ì(€€€€€€€¥˜€¡É½Õ¹€ôô¹Õ±°ñð€…¹½9•…É‰åA±…å•È¡±•Ù•°°É½Õ¹°€ä¸Á¤ñð±•Ù•°¹•Ñ	±½­¹Ñ¥Ñä¡É½Õ¹¤€„ô¹Õ±°¤É•ÑÕÉ¸™…±Í”ì(€€€€€€€	±½­MÑ…Ñ”Í½¥°€ô±•Ù•°¹•Ñ	±½­MÑ…Ñ”¡É½Õ¹¤ì(€€€€€€€¥˜€ „¡Í½¥°¹¥Ì¡	±½­Ì¹IMM}	1=,¤ñðÍ½¥°¹¥Ì¡	±½­Ì¹%IP¤ñðÍ½¥°¹¥Ì¡	±½­Ì¹A=i=0¤¤¤É•ÑÕÉ¸™…±Í”ì(€€€€€€€	±½­A½ÌÁ½Ì€ôÉ½Õ¹¹…‰½Ù” ¤ì(€€€€€€€	±½­MÑ…Ñ”™±½Ý•È€ôÍÝ¥Ñ €¡!…Í¡9½¥Í”¹‰½Õ¹‘•¡!…Í¡9½¥Í”¹½µ‰¥¹”¡Í••°ÕÉÍ½È°€Áá…‰ŒÄÅ0¤°€Ð¤¤ì(€€€€€€€€€€€…Í”€À€´ø	±½­Ì¹91%=8¹‘•™…Õ±Ñ	±½­MÑ…Ñ” ¤ì(€€€€€€€€€€€…Í”€Ä€´ø	±½­Ì¹A=AAd¹‘•™…Õ±Ñ	±½­MÑ…Ñ” ¤ì(€€€€€€€€€€€…Í”€È€´ø	±½­Ì¹iUI}	1UP¹‘•™…Õ±Ñ	±½­MÑ…Ñ” ¤ì(€€€€€€€€€€€‘•™…Õ±Ð€´ø	±½­Ì¹=ae}%Md¹‘•™…Õ±Ñ	±½­MÑ…Ñ” ¤ì(€€€€€€€ôì(€€€€€€€É•ÑÕÉ¸±•Ù•°¹•Ñ	±½­MÑ…Ñ”¡Á½Ì¤¹¥Í¥È ¤€˜˜™±½Ý•È¹…¹MÕÉÙ¥Ù”¡±•Ù•°°Á½Ì¤€˜˜±•Ù•°¹Í•Ñ	±½¬¡Á½Ì°™±½Ý•È°€Ì¤ì(€€€ô((€€€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ‰½½±•…¸ÑÉåA±…•QÉ…¥°¡M•ÉÙ•É1•Ù•°±•Ù•°°	±½­A½ÌÉ…Ü¤ì(€€€€€€€	±½­A½ÌÉ½Õ¹€ô™¥¹‘MÕÉ™…”¡±•Ù•°°É…Ü¤ì(€€€€€€€¥˜€¡É½Õ¹€ôô¹Õ±°ñð€…¹½9•…É‰åA±…å•È¡±•Ù•°°É½Õ¹°€ÄÀ¸Á¤ñð±•Ù•°¹•Ñ	±½­¹Ñ¥Ñä¡É½Õ¹¤€„ô¹Õ±°¤É•ÑÕÉ¸™…±Í”ì(€€€€€€€	±½­MÑ…Ñ”ÍÑ…Ñ”€ô±•Ù•°¹•Ñ	±½­MÑ…Ñ”¡É½Õ¹¤ì(€€€€€€€¥˜€ …ÍÑ…Ñ”¹¥Ì¡	±½­Ì¹IMM}	1=,¤¤É•ÑÕÉ¸™…±Í”ì(€€€€€€€É•ÑÕÉ¸±•Ù•°¹Í•Ñ	±½¬¡É½Õ¹°	±½­Ì¹=IM}%IP¹‘•™…Õ±Ñ	±½­MÑ…Ñ” ¤°€Ì¤ì(€€€ô((€€€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ‰½½±•…¸ÑÉåA±…•	ÕÉÉ½Ý5…É¬¡M•ÉÙ•É1•Ù•°±•Ù•°°	±½­A½ÌÉ…Ü¤ì(€€€€€€€	±½­A½ÌÉ½Õ¹€ô™¥¹‘MÕÉ™…”¡±•Ù•°°É…Ü¤ì(€€€€€€€¥˜€¡É½Õ¹€ôô¹Õ±°ñð€…¹½9•…É‰åA±…å•È¡±•Ù•°°É½Õ¹°€ÄÀ¸Á¤ñð±•Ù•°¹•Ñ	±½­¹Ñ¥Ñä¡É½Õ¹¤€„ô¹Õ±°¤É•ÑÕÉ¸™…±Í”ì(€€€€€€€	±½­MÑ…Ñ”ÍÑ…Ñ”€ô±•Ù•°¹•Ñ	±½­MÑ…Ñ”¡É½Õ¹¤ì(€€€€€€€¥˜€ „¡ÍÑ…Ñ”¹¥Ì¡	±½­Ì¹IMM}	1=,¤ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹%IP¤¤¤É•ÑÕÉ¸™…±Í”ì(€€€€€€€€¼¼Y¥ÍÕ…°Í¥¸½¹±äè¹•Ù•È½Á•¹Ì„¡½±”°Í¼Ñ•ÉÉ…¥¸½Á…Ñ¡™¥¹‘¥¹œ…¹¹½Ð‰”‰É½­•¸‰äÑ¡”™½½ÑÁÉ¥¹ÐÍåÍÑ•´¸(€€€€€€€É•ÑÕÉ¸±•Ù•°¹Í•Ñ	±½¬¡É½Õ¹°	±½­Ì¹=IM}%IP¹‘•™…Õ±Ñ	±½­MÑ…Ñ” ¤°€Ì¤ì(€€€ô((€€€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ‰½½±•…¸ÑÉåA±…•5ÕÍ¡É½½´¡M•ÉÙ•É1•Ù•°±•Ù•°°	±½­A½ÌÉ…Ü°±½¹œÍ••°¥¹ÐÕÉÍ½È¤ì(€€€€€€€	±½­A½ÌÉ½Õ¹€ô™¥¹‘MÕÉ™…”¡±•Ù•°°É…Ü¤ì(€€€€€€€¥˜€¡É½Õ¹€ôô¹Õ±°ñð€…¹½9•…É‰åA±…å•È¡±•Ù•°°É½Õ¹°€ä¸Á¤ñð±•Ù•°¹•Ñ	±½­¹Ñ¥Ñä¡É½Õ¹¤€„ô¹Õ±°¤É•ÑÕÉ¸™…±Í”ì(€€€€€€€	±½­MÑ…Ñ”Í½¥°€ô±•Ù•°¹•Ñ	±½­MÑ…Ñ”¡É½Õ¹¤ì(€€€€€€€¥˜€ „¡Í½¥°¹¥Ì¡	±½­Ì¹5e1%U4¤ñðÍ½¥°¹¥Ì¡	±½­Ì¹IMM}	1=,¤ñðÍ½¥°¹¥Ì¡	±½­Ì¹%IP¤ñðÍ½¥°¹¥Ì¡	±½­Ì¹A=i=0¤¤¤É•ÑÕÉ¸™…±Í”ì(€€€€€€€	±½­A½ÌÁ½Ì€ôÉ½Õ¹¹…‰½Ù” ¤ì(€€€€€€€	±½­MÑ…Ñ”µÕÍ¡É½½´€ô!…Í¡9½¥Í”¹‰½Õ¹‘•¡!…Í¡9½¥Í”¹½µ‰¥¹”¡Í••°ÕÉÍ½È°€ÁàääÄÅ0¤°€È¤€ôô€À(€€€€€€€€€€€€€€€€ü	±½­Ì¹I}5UM!I==4¹‘•™…Õ±Ñ	±½­MÑ…Ñ” ¤€è	±½­Ì¹	I=]9}5UM!I==4¹‘•™…Õ±Ñ	±½­MÑ…Ñ” ¤ì(€€€€€€€É•ÑÕÉ¸±•Ù•°¹•Ñ	±½­MÑ…Ñ”¡Á½Ì¤¹¥Í¥È ¤€˜˜µÕÍ¡É½½´¹…¹MÕÉÙ¥Ù”¡±•Ù•°°Á½Ì¤€˜˜±•Ù•°¹Í•Ñ	±½¬¡Á½Ì°µÕÍ¡É½½´°€Ì¤ì(€€€ô((€€€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ‰½½±•…¸¹½9•…É‰åA±…å•È¡M•ÉÙ•É1•Ù•°±•Ù•°°	±½­A½ÌÁ½Ì°‘½Õ‰±”É…‘¥ÕÌ¤ì(€€€€€€€É•ÑÕÉ¸±•Ù•°¹•Ñ9•…É•ÍÑA±…å•È¡Á½Ì¹•Ñ` ¤€¬€À¸Õ°Á½Ì¹•Ñd ¤€¬€À¸Õ°Á½Ì¹•Ñh ¤€¬€À¸Õ°É…‘¥ÕÌ°™…±Í”¤€ôô¹Õ±°ì(€€€ô((€€€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ‰½½±•…¸ÑÉåA±…•½‰Ý•ˆ¡M•ÉÙ•É1•Ù•°±•Ù•°°	±½­A½ÌÁ½Ì¤ì(€€€€€€€¥˜€ …±•Ù•°¹•Ñ	±½­MÑ…Ñ”¡Á½Ì¤¹¥Í¥È ¤¤É•ÑÕÉ¸™…±Í”ì(€€€€€€€¥˜€¡±•Ù•°¹…¹M••M­ä¡Á½Ì¤¤É•ÑÕÉ¸™…±Í”ì(€€€€€€€¥˜€ …¹½9•…É‰åA±…å•È¡±•Ù•°°Á½Ì°€Ü¸Á¤¤É•ÑÕÉ¸™…±Í”ì((€€€€€€€‰½½±•…¸¹…ÑÕÉ…±MÕÁÁ½ÉÐ€ô™…±Í”ì(€€€€€€€™½È€¡¥É•Ñ¥½¸‘¥É•Ñ¥½¸€è¥É•Ñ¥½¸¹Ù…±Õ•Ì ¤¤ì(€€€€€€€€€€€	±½­A½ÌÍÕÁÁ½ÉÑA½Ì€ôÁ½Ì¹É•±…Ñ¥Ù”¡‘¥É•Ñ¥½¸¤ì(€€€€€€€€€€€	±½­MÑ…Ñ”ÍÕÁÁ½ÉÐ€ô±•Ù•°¹•Ñ	±½­MÑ…Ñ”¡ÍÕÁÁ½ÉÑA½Ì¤ì(€€€€€€€€€€€¥˜€¡±•Ù•°¹•Ñ	±½­¹Ñ¥Ñä¡ÍÕÁÁ½ÉÑA½Ì¤€„ô¹Õ±°¤É•ÑÕÉ¸™…±Í”ì(€€€€€€€€€€€¥˜€¡¥Í9…ÑÕÉ…±MÕÁÁ½ÉÐ¡ÍÕÁÁ½ÉÐ¤(€€€€€€€€€€€€€€€€€€€€˜˜ÍÕÁÁ½ÉÐ¹¥Í…•MÑÕÉ‘ä¡±•Ù•°°ÍÕÁÁ½ÉÑA½Ì°‘¥É•Ñ¥½¸¹•Ñ=ÁÁ½Í¥Ñ” ¤¤¤ì(€€€€€€€€€€€€€€€¹…ÑÕÉ…±MÕÁÁ½ÉÐ€ôÑÉÕ”ì(€€€€€€€€€€€ô(€€€€€€€ô(€€€€€€€¥˜€ …¹…ÑÕÉ…±MÕÁÁ½ÉÐ¤É•ÑÕÉ¸™…±Í”ì(€€€€€€€É•ÑÕÉ¸±•Ù•°¹Í•Ñ	±½¬¡Á½Ì°	±½­Ì¹=	]¹‘•™…Õ±Ñ	±½­MÑ…Ñ” ¤°€Ì¤ì(€€€ô((€€€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ‰½½±•…¸¥Í9…ÑÕÉ…±MÕÁÁ½ÉÐ¡	±½­MÑ…Ñ”ÍÑ…Ñ”¤ì(€€€€€€€É•ÑÕÉ¸ÍÑ…Ñ”¹¥Ì¡	±½­Ì¹MQ=9¤ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹AM1Q¤ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹QU¤(€€€€€€€€€€€€€€€ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹I9%Q¤ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹%=I%Q¤ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹9M%Q¤(€€€€€€€€€€€€€€€ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹%IP¤ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹IMM}	1=,¤ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹A=i=0¤(€€€€€€€€€€€€€€€ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹=-}1=¤ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹MAIU}1=¤ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹	%I!}1=¤(€€€€€€€€€€€€€€€ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹)U91}1=¤ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹%}1=¤ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹I-}=-}1=¤(€€€€€€€€€€€€€€€ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹59I=Y}1=¤ñðÍÑ…Ñ”¹¥Ì¡	±½­Ì¹!IIe}1=¤ì(€€€ô((€€€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ‰½½±•…¸¥ÍA±…å•É	½Õ¹¡5½ˆµ½ˆ¤ì(€€€€€€€¥˜€¡µ½ˆ¥¹ÍÑ…¹•½˜Q…µ…‰±•¹¥µ…°Ñ…µ”€˜˜Ñ…µ”¹¥ÍQ…µ” ¤¤É•ÑÕÉ¸ÑÉÕ”ì(€€€€€€€É•ÑÕÉ¸µ½ˆ¥¹ÍÑ…¹•½˜‰ÍÑÉ…Ñ!½ÉÍ”¡½ÉÍ”€˜˜¡½ÉÍ”¹¥ÍQ…µ• ¤ì(€€€ô((€€€ÁÕ‰±¥ŒÍÑ…Ñ¥ŒÙ½¥Í•ÑM¥µÕ±…Ñ¥½¹M…±”¡M•ÉÙ•É1•Ù•°±•Ù•°°‘½Õ‰±”Í…±”¤ì(€€€€€€€±½¹œ¹½Ü€ô±•Ù•°¹•Ñ…µ•Q¥µ” ¤ì(€€€€€€€€¼¼I•‰…Í”…±°…‰ÍÑÉ…Ð±½­Ì‰•™½É”¡…¹¥¹œÍ…±”Í¼Ñ¡”¹•ÜµÕ±Ñ¥Á±¥•È¥Ì¹•Ù•ÈÉ•ÑÉ½…Ñ¥Ù”¸(€€€€€€€¹Ù¥É½¹µ•¹ÑM…Ù•‘…Ñ„¹•Ð¡±•Ù•°¤¹É•‰…Í”¡¹½Ü¤ì(€€€€€€€Q•ÉÉ¥Ñ½ÉåM…Ù•‘…Ñ„¹•Ð¡±•Ù•°¤¹Í•ÑM¥µÕ±…Ñ¥½¹M…±”¡Í…±”°¹½Ü¤ì(€€€ô((€€€ÁÕ‰±¥ŒÍÑ…Ñ¥Œ‘½Õ‰±”Í¥µÕ±…Ñ¥½¹M…±”¡M•ÉÙ•É1•Ù•°±•Ù•°¤ì(€€€€€€€É•ÑÕÉ¸Q•ÉÉ¥Ñ½ÉåM…Ù•‘…Ñ„¹•Ð¡±•Ù•°¤¹Í¥µÕ±…Ñ¥½¹M…±” ¤ì(€€€ô((€€€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ‘½Õ‰±”ÍÅÈ¡‘½Õ‰±”Ù…±Õ”¤ìÉ•ÑÕÉ¸Ù…±Õ”€¨Ù…±Õ”ìô)ô