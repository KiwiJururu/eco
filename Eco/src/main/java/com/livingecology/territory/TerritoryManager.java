package com.livingecology.territory;

import com.livingecology.ai.BehaviorUtil;
import com.livingecology.data.AttributeType;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
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
import net.minecraft.world.entity.animal.Wolf;
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
        if (species == SpeciesType.WOLF && mob instanceof Wolf wolf && wolf.isTame()) return null;

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

        SpeciesProfile profile = SpeciesProfile.of(species);
        List<Mob> same = level.getEntitiesOfClass(Mob.class,
                mob.getBoundingBox().inflate(profile.territorySearchRadius()),
                e -> e.isAlive() && species.matches(e)
                        && !(e instanceof Wolf w && w.isTame()));
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
        int radius = Mth.clamp(2 + territoriality / 25 + Math.min(2, same.size() / 4), 2, maxRadius(species));
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

    private static int maxRadius(SpeciesType species) {
        return switch (species) {
            case COW -> 6;
            case WOLF -> 7;
            case SPIDER -> 6;
            case ZOMBIE -> 7;
        };
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

        int target = Mth.clamp(18 + record.population() * 5 + record.maturity() / 3
                + (env.habitability() - 50) / 3, 0, 100);
        int maxChange = (int) Math.min(30L, steps / 2L + 1L);
        if (record.state() == TerritoryState.ABANDONED) target = Math.min(target, 10);
        record.setPressure(approach(record.pressure(), target, maxChange));

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
                e -> e.isAlive() && record.species().matches(e)
                        && !(e instanceof Wolf w && w.isTame()));
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
        int constitutionMid = switch (record.species()) {
            case COW -> 63;
            case WOLF -> 65;
            case SPIDER -> 48;
            case ZOMBIE -> 73;
        };
        return record.pressure() + record.population() * 3 + constitutionMid / 2
                + (record.leader() != null ? 12 : 0);
    }

    private static void conquestStep(ServerLevel level, TerritorySavedData data, TerritoryRecord winner,
                                     TerritoryRecord loser, RelationRecord relation) {
        winner.setRadiusChunks(Math.min(maxRadius(winner.species()), winner.radiusChunks() + 1));
        loser.setRadiusChunks(Math.max(2, loser.radiusChunks() - 1));
        winner.addPressure(4);
        loser.addPressure(-8);
        if (loser.pressure() < 25) loser.setState(TerritoryState.RECOVERING);

        BlockPos oldCenter = loser.center();
        double dx = loser.center().getX() - winner.center().getX();
        double dz = loser.center().getZ() - winner.center().getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.1D) { dx = 1.0D; dz = 0.0D; len = 1.0D; }
        int shiftX = (int) Math.round(dx / len * 16.0D);
        int shiftZ = (int) Math.round(dz / len * 16.0D);
        loser.setCenter(oldCenter.offset(shiftX, 0, shiftZ));
        data.reindex(loser, oldCenter);
        relation.resetMomentum();
        data.setDirty();
    }

    public static TerritoryContext contextForMob(Mob mob, ServerLevel level) {
        return contextAt(level, mob.blockPosition(), MobMindData.territoryId(mob));
    }

    public static TerritoryContext contextAt(ServerLevel level, BlockPos pos, long ownId) {
        TerritorySavedData data = TerritorySavedData.get(level);
        TerritoryRecord own = data.get(ownId);
        TerritoryZone ownZone = own == null ? TerritoryZone.OUTSIDE : zoneFor(own, pos);

        TerritoryRecord first = null;
        TerritoryRecord second = null;
        double firstInfluence = 0.0D;
        double secondInfluence = 0.0D;
        for (TerritoryRecord record : data.near(pos)) {
            double influence = influenceAt(record, pos);
            if (influence <= 0.0D) continue;
            if (influence > firstInfluence) {
                second = first;
                secondInfluence = firstInfluence;
                first = record;
                firstInfluence = influence;
            } else if (influence > secondInfluence) {
                second = record;
                secondInfluence = influence;
            }
        }

        boolean noMansLand = false;
        boolean contested = false;
        int tension = 0;
        int affinity = 0;
        if (first != null && second != null) {
            boolean overlap = firstInfluence > 8.0D && secondInfluence > 8.0D;
            tension = RelationService.tension(data, first, second, overlap);
            affinity = RelationService.effectiveAffinity(data, first, second);
            RelationshipProfile natural = RelationService.natural(first.species(), second.species());
            boolean wolfSpider = (first.species() == SpeciesType.WOLF && second.species() == SpeciesType.SPIDER)
                    || (first.species() == SpeciesType.SPIDER && second.species() == SpeciesType.WOLF);
            noMansLand = wolfSpider && natural.kind() == RelationKind.BORDERED && overlap
                    && tension >= 20 && tension < 70 && Math.abs(firstInfluence - secondInfluence) <= 15.0D;
            contested = overlap && tension >= 60;
        }
        return new TerritoryContext(own, ownZone, first, second, noMansLand, contested, tension, affinity);
    }

    public static TerritoryZone zoneFor(TerritoryRecord record, BlockPos pos) {
        double radius = effectiveRadiusBlocks(record, pos);
        double distance = horizontalDistance(record.center(), pos);
        if (distance > radius || record.pressure() <= 0) return TerritoryZone.OUTSIDE;
        double normalized = distance / Math.max(1.0D, radius);
        if (normalized <= 0.20D) return TerritoryZone.CORE;
        if (normalized <= 0.65D) return TerritoryZone.INNER;
        return TerritoryZone.OUTER;
    }

    public static double influenceAt(TerritoryRecord record, BlockPos pos) {
        double radius = effectiveRadiusBlocks(record, pos);
        double distance = horizontalDistance(record.center(), pos);
        if (distance >= radius) return 0.0D;
        double factor = 1.0D - distance / Math.max(1.0D, radius);
        return record.pressure() * factor;
    }

    private static double effectiveRadiusBlocks(TerritoryRecord record, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        double edgeNoise = 0.88D + HashNoise.unit(HashNoise.combine(record.seed(), chunkX, chunkZ)) * 0.24D;
        return record.radiusChunks() * 16.0D * edgeNoise;
    }

    private static double horizontalDistance(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static void recordAggression(Mob victim, LivingEntity attacker, ServerLevel level, int severity) {
        if (!(attacker instanceof Mob attackerMob)) return;
        if (!MobMindData.supports(attackerMob)) return;
        ensureTerritory(victim, level);
        ensureTerritory(attackerMob, level);
        long a = MobMindData.territoryId(victim);
        long b = MobMindData.territoryId(attackerMob);
        if (a <= 0 || b <= 0 || a == b) return;
        TerritorySavedData data = TerritorySavedData.get(level);
        RelationRecord relation = data.relation(a, b);
        if (relation != null) {
            relation.addRivalry(Math.max(1, severity), level.getGameTime());
            relation.adjustAffinity(-Math.max(1, severity / 2));
            data.setDirty();
        }
    }

    public static void recordCooperation(Mob helper, Mob ally, ServerLevel level, int amount) {
        ensureTerritory(helper, level);
        ensureTerritory(ally, level);
        long a = MobMindData.territoryId(helper);
        long b = MobMindData.territoryId(ally);
        if (a <= 0 || b <= 0 || a == b) return;
        TerritorySavedData data = TerritorySavedData.get(level);
        RelationRecord relation = data.relation(a, b);
        if (relation != null) {
            relation.addAffinity(Math.max(1, amount), level.getGameTime());
            relation.addRivalry(-1, level.getGameTime());
            data.setDirty();
        }
    }

    public static void onCobwebBroken(ServerLevel level, BlockPos pos) {
        TerritorySavedData data = TerritorySavedData.get(level);
        TerritoryRecord spider = data.near(pos).stream()
                .filter(r -> r.species() == SpeciesType.SPIDER && zoneFor(r, pos) != TerritoryZone.OUTSIDE)
                .max(Comparator.comparingDouble(r -> influenceAt(r, pos))).orElse(null);
        if (spider == null) return;
        spider.addFootprintProgress(-1);
        if (zoneFor(spider, pos) == TerritoryZone.CORE) spider.addPressure(-1);
        data.setDirty();
    }

    public static void boostBossTerritory(Mob boss, ServerLevel level) {
        if (!MobMindData.isBoss(boss)) return;
        TerritoryRecord territory = ensureTerritory(boss, level);
        if (territory == null) return;
        territory.setLeader(boss.getUUID());
        if (level.getGameTime() % 200L == 0L) territory.addPressure(1);
        TerritorySavedData.get(level).setDirty();
    }

    private static void materializeFootprints(ServerLevel level, int blockBudget, int maxChecks) {
        if (blockBudget <= 0 || maxChecks <= 0) return;
        TerritorySavedData data = TerritorySavedData.get(level);
        List<TerritoryRecord> spiders = activeTerritories(level).stream()
                .filter(r -> r.species() == SpeciesType.SPIDER)
                .sorted(Comparator.comparingLong(TerritoryRecord::id)).toList();
        if (spiders.isEmpty()) return;

        int start = (int) Math.floorMod(level.getGameTime(), spiders.size());
        int changed = 0;
        int checks = 0;
        for (int offset = 0; offset < spiders.size() && changed < blockBudget && checks < maxChecks; offset++) {
            TerritoryRecord record = spiders.get((start + offset) % spiders.size());
            int desired = record.desiredFootprint();
            boolean grow = record.footprintProgress() < desired;
            boolean decay = record.footprintProgress() > desired;
            if (!grow && !decay) continue;

            while (changed < blockBudget && checks < maxChecks) {
                checks++;
                int cursor = record.nextFootprintCursor();
                BlockPos candidate = footprintCandidate(record, cursor);
                if (!level.hasChunkAt(candidate)) continue;
                TerritoryZone zone = zoneFor(record, candidate);
                if (zone == TerritoryZone.OUTSIDE) continue;

                if (grow) {
                    double chance = switch (zone) {
                        case CORE -> 0.90D;
                        case INNER -> 0.58D;
                        case OUTER -> 0.24D;
                        case OUTSIDE -> 0.0D;
                    };
                    if (HashNoise.unit(HashNoise.combine(record.seed(), cursor, 991L)) > chance) continue;
                    if (tryPlaceCobweb(level, candidate)) {
                        record.addFootprintProgress(1);
                        changed++;
                        break;
                    }
                } else if (level.getBlockState(candidate).is(Blocks.COBWEB)
                        && level.getNearestPlayer(candidate.getX() + 0.5D, candidate.getY() + 0.5D,
                        candidate.getZ() + 0.5D, 7.0D, false) == null) {
                    level.setBlock(candidate, Blocks.AIR.defaultBlockState(), 3);
                    record.addFootprintProgress(-1);
                    changed++;
                    break;
                }
            }
        }
        if (changed > 0 || checks > 0) data.setDirty();
    }

    private static BlockPos footprintCandidate(TerritoryRecord record, int cursor) {
        long h1 = HashNoise.combine(record.seed(), cursor, 0x1f123bb5L);
        long h2 = HashNoise.mix64(h1 ^ 0x5deece66dL);
        double angle = HashNoise.unit(h1) * Math.PI * 2.0D;
        double maxRadius = record.radiusChunks() * 16.0D * 0.86D;
        double distance = Math.sqrt(HashNoise.unit(h2)) * maxRadius;
        int dx = (int) Math.round(Math.cos(angle) * distance);
        int dz = (int) Math.round(Math.sin(angle) * distance);
        int dy = HashNoise.signed(h2 ^ 0x123456789abcdefL, 7);
        return record.core().offset(dx, dy, dz);
    }

    private static boolean tryPlaceCobweb(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return false;
        if (level.canSeeSky(pos)) return false;
        if (level.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 7.0D, false) != null)
            return false;

        boolean naturalSupport = false;
        for (Direction direction : Direction.values()) {
            BlockPos supportPos = pos.relative(direction);
            BlockState support = level.getBlockState(supportPos);
            if (level.getBlockEntity(supportPos) != null) return false;
            if (isNaturalSupport(support)
                    && support.isFaceSturdy(level, supportPos, direction.getOpposite())) {
                naturalSupport = true;
            }
        }
        if (!naturalSupport) return false;
        return level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
    }

    private static boolean isNaturalSupport(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.TUFF)
                || state.is(Blocks.GRANITE) || state.is(Blocks.DIORITE) || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL)
                || state.is(Blocks.OAK_LOG) || state.is(Blocks.SPRUCE_LOG) || state.is(Blocks.BIRCH_LOG)
                || state.is(Blocks.JUNGLE_LOG) || state.is(Blocks.ACACIA_LOG) || state.is(Blocks.DARK_OAK_LOG)
                || state.is(Blocks.MANGROVE_LOG) || state.is(Blocks.CHERRY_LOG);
    }

    public static void setSimulationScale(ServerLevel level, double scale) {
        long now = level.getGameTime();
        // Rebase all abstract clocks before changing scale so the new multiplier is never retroactive.
        EnvironmentSavedData.get(level).rebase(now);
        TerritorySavedData.get(level).setSimulationScale(scale, now);
    }

    public static double simulationScale(ServerLevel level) {
        return TerritorySavedData.get(level).simulationScale();
    }

    private static double sqr(double value) { return value * value; }
}
